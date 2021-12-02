package mu.semte.ch.api.kalliope.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import mu.semte.ch.lib.dto.FileDataObject;
import mu.semte.ch.lib.utils.ModelUtils;
import mu.semte.ch.lib.utils.SparqlClient;
import mu.semte.ch.lib.utils.SparqlQueryStore;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import java.util.Map;

import static java.util.Optional.ofNullable;
import static mu.semte.ch.api.kalliope.Constants.LOGICAL_FILE_PREFIX;
import static mu.semte.ch.lib.utils.ModelUtils.filenameToLang;
import static mu.semte.ch.lib.utils.ModelUtils.formattedDate;
import static mu.semte.ch.lib.utils.ModelUtils.getContentType;
import static mu.semte.ch.lib.utils.ModelUtils.getExtension;
import static mu.semte.ch.lib.utils.ModelUtils.uuid;

@Service
@Slf4j
public class TaskService {

  private final SparqlQueryStore queryStore;
  private final SparqlClient sparqlClient;
  @Value("${share-folder.path}")
  private String shareFolderPath;
  @Value("${sparql.defaultBatchSize}")
  private int defaultBatchSize;
  @Value("${sparql.defaultLimitSize}")
  private int defaultLimitSize;
  @Value("${sparql.maxRetry}")
  private int maxRetry;

  public TaskService(SparqlQueryStore queryStore, SparqlClient sparqlClient) {
    this.queryStore = queryStore;
    this.sparqlClient = sparqlClient;
  }

  public Model loadDeltaModel(String graphImportedTriples) {
    String queryTask = queryStore.getQuery("loadImportedTriples").formatted(graphImportedTriples);
    return sparqlClient.executeSelectQuery(queryTask);
  }

  public void importTriples(String graph,
                            Model model) {
    log.debug("running import triples with batch size {}, model size: {}, graph: <{}>", defaultBatchSize, model.size(), graph);
    List<Triple> triples = model.getGraph().find().toList(); //duplicate so we can splice
    Lists.partition(triples, defaultBatchSize)
         .stream()
         .parallel()
         .map(batch -> {
           Model batchModel = ModelFactory.createDefaultModel();
           Graph batchGraph = batchModel.getGraph();
           batch.forEach(batchGraph::add);
           return batchModel;
         }).forEach(batchModel -> sparqlClient.insertModel(graph, batchModel));
  }

  @SneakyThrows
  public String writeTtlFile(String graph,
                             Model content,
                             String logicalFileName) {
    var rdfLang = filenameToLang(logicalFileName);
    var fileExtension = getExtension(rdfLang);
    var contentType = getContentType(rdfLang);
    var phyId = uuid();
    var phyFilename = "%s.%s".formatted(phyId, fileExtension);
    var path = "%s/%s".formatted(shareFolderPath, phyFilename);
    var physicalFile = "share://%s".formatted(phyFilename);
    var loId = uuid();
    var logicalFile = "%s/%s".formatted(LOGICAL_FILE_PREFIX, loId);
    var now = formattedDate(LocalDateTime.now());
    var file = ModelUtils.toFile(content, rdfLang, path);
    var fileSize = file.length();
    var queryParameters = ImmutableMap.<String, Object>builder()
                                      .put("graph", graph)
                                      .put("physicalFile", physicalFile)
                                      .put("logicalFile", logicalFile)
                                      .put("phyId", phyId)
                                      .put("phyFilename", phyFilename)
                                      .put("now", now)
                                      .put("fileSize", fileSize)
                                      .put("loId", loId)
                                      .put("logicalFileName", logicalFileName)
                                      .put("fileExtension", "nt")
                                      .put("contentType", contentType).build();

    var queryStr = queryStore.getQueryWithParameters("writeTtlFile", queryParameters);
    sparqlClient.executeUpdateQuery(queryStr);
    return logicalFile;
  }

  @SneakyThrows
  public List<FileDataObject> readTurtleFiles(List<String> graphUris, LocalDateTime since, LocalDateTime snapshot) {
    var queryParameters = ImmutableMap.<String, Object>builder()
                                      .put("graphs", graphUris)
                                      .put("since", ofNullable(since).map(ModelUtils::formattedDate).orElse(""))
                                      .put("snapshot", ofNullable(snapshot).map(ModelUtils::formattedDate).orElse("")).build();

    String query = queryStore.getQueryWithParameters("readTtlFile", queryParameters);

    return sparqlClient.executeSelectQuery(query, resultSet -> {
      if (!resultSet.hasNext()) {
        log.debug("No files found for graph");
      }
      List<FileDataObject> turtleFiles = new ArrayList<>();


      resultSet.forEachRemaining(r -> turtleFiles.add(FileDataObject.builder()
                                                                    .graph(r.getResource("graph").getURI())
                                                                    .physicalFileName(r.getLiteral("physicalFileName")
                                                                                       .getString())
                                                                    .physicalFile("%s/%s".formatted(shareFolderPath, r.getLiteral("physicalFileName")
                                                                                                                      .getString()))
                                                                    .physicalId(r.getLiteral("physicalId").getString())
                                                                    .creator(r.getResource("creator").getURI())
                                                                    .logicalId(r.getLiteral("logicalId").getString())
                                                                    .logicalFileName(r.getLiteral("logicalFileName")
                                                                                      .getString())
                                                                    .fileSize(r.getLiteral("fileSize").getString())
                                                                    .fileExtension(r.getLiteral("fileExtension").getString())
                                                                    .contentType(r.getLiteral("contentType").getString())
                                                                    .build()));
      return turtleFiles;
    });
  }

  public List<FileDataObject> readTurtleFiles(String graphUri, LocalDateTime since, LocalDateTime snapshot) {
    return readTurtleFiles(Collections.singletonList(graphUri), since, snapshot);
  }

  public Model fetchTriplesFromGraph(String graph) {
    var countTriplesQuery = queryStore.getQuery("countTriplesInGraph").formatted(graph);
    var countTriples = sparqlClient.executeSelectQuery(countTriplesQuery, resultSet -> {
      if (!resultSet.hasNext()) {
        return 0;
      }
      return resultSet.next().getLiteral("count").getInt();
    });
    var pagesCount = countTriples > defaultLimitSize ? countTriples / defaultLimitSize : defaultLimitSize;

    return IntStream.rangeClosed(0, pagesCount)
                    .mapToObj(page -> {
                      var query = queryStore.getQueryWithParameters("triplesInGraph",
                                                                    Map.of("graphUri", graph,
                                                                          "limitSize", defaultLimitSize,
                                                                          "offsetNumber", page * defaultLimitSize
                                                                    )
                      );
                      return sparqlClient.executeSelectQuery(query);
                    }).reduce(ModelFactory.createDefaultModel(), Model::add);
                  }
}
