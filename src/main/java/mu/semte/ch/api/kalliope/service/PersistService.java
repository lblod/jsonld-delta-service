package mu.semte.ch.api.kalliope.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import mu.semte.ch.api.kalliope.Constants;
import mu.semte.ch.lib.dto.Delta;
import mu.semte.ch.lib.dto.Triple;
import mu.semte.ch.lib.utils.ModelUtils;


@Service
@Slf4j
public class PersistService {
    private final TaskService taskService;

    public PersistService(TaskService taskService) {
      this.taskService = taskService;
    }    

    @Async
    public void writeDelta(Delta delta) {
      var now = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
      
      taskService.writeTtlFile(
        Constants.DELTA_INSERTS_GRAPH_PREFIX,
        extractModel(delta.getInserts()),
        now+"-inserts.ttl"
      );
      taskService.writeTtlFile(
        Constants.DELTA_DELETES_GRAPH_PREFIX,
        extractModel(delta.getDeletes()),
        now+"-deletes.ttl"
      );
    }

    private Model extractModel(List<Triple> inserts) {
      var model = ModelFactory.createDefaultModel();

      var statements = inserts
        .stream()
        .map(triple -> ResourceFactory.createStatement(
          (Resource) ModelUtils.nodeToResource(triple.getSubject()) ,
          ModelUtils.nodeToProperty(triple.getPredicate()),
          ModelUtils.nodeToResource(triple.getObject())
          )
        ).toArray(Statement[]::new);

      model.add(statements);
      return model;
    }

    public Model getAllInserts(LocalDateTime since, LocalDateTime snapshot) {
      return getGraph(Constants.DELTA_INSERTS_GRAPH_PREFIX, since, snapshot);
    }

    public Model getAllDeletes(LocalDateTime since, LocalDateTime snapshot) {
      return getGraph(Constants.DELTA_DELETES_GRAPH_PREFIX, since, snapshot);
    }

    public Model getConsolidated(LocalDateTime since, LocalDateTime snapshot) {
      Model graph = ModelFactory.createDefaultModel();
      taskService.readTurtleFiles(
        Arrays.asList(
          Constants.DELTA_DELETES_GRAPH_PREFIX,
          Constants.DELTA_INSERTS_GRAPH_PREFIX
        ),
        since,
        snapshot
      ).forEach(t -> {
        if(t.getGraph().equals(Constants.DELTA_INSERTS_GRAPH_PREFIX)) {
          graph.read(t.getPhysicalFile());
        } else if (t.getGraph().equals(Constants.DELTA_DELETES_GRAPH_PREFIX)) {
          graph.remove(RDFDataMgr.loadModel(t.getPhysicalFile()));
        } else {
          log.error(String.format("Unknown graph {}", t.getGraph()));
        }
      });
      return graph;
    }

    private Model getGraph(String graphName, LocalDateTime since, LocalDateTime snapshot) {
      Model graph = ModelFactory.createDefaultModel();
      taskService.readTurtleFiles(graphName, since, snapshot)
        .forEach(t -> graph.read(t.getPhysicalFile()));
      return graph;
    }
}
