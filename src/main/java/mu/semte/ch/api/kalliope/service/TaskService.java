package mu.semte.ch.api.kalliope.service;

import java.util.Map;
import java.util.stream.IntStream;

import com.github.jsonldjava.core.RDFParser;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFParserBuilder;
import org.apache.jena.riot.system.stream.LocatorHTTP;
import org.apache.jena.riot.system.stream.StreamManager;
import org.apache.jena.sparql.function.library.uuid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import mu.semte.ch.lib.utils.SparqlClient;
import mu.semte.ch.lib.utils.SparqlQueryStore;

@Service
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

  public Model fetchModelFromDump(String subject) {
    var query = queryStore.getQuery("lastDump").formatted(subject);
    var fileName = sparqlClient.executeSelectQuery(query, resultSet -> {
      if (!resultSet.hasNext()) {
        return "";
      }
      return resultSet.next().getResource("phyiscalFile").getURI();
    });
    if (fileName.isEmpty()) {
      return ModelFactory.createDefaultModel();
    } else {
      return RDFDataMgr.loadModel(fileName.replaceAll("share://", "file://share/"));
    }
   }
}
