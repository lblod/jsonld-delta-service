package mu.semte.ch.api.kalliope.service;

import java.util.Map;
import java.util.stream.IntStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
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
