package mu.semte.ch.api.kalliope.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
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
        Constants.DELTA_DELETES_GRAPH_PREFIX,
        extractModel(delta.getDeletes()),
        now+"-deletes.ttl"
      );
      
      taskService.writeTtlFile(
        Constants.DELTA_INSERTS_GRAPH_PREFIX,
        extractModel(delta.getInserts()),
        now+"-inserts.ttl"
      );
    }

    @Async
    public void writeBulk(Model model) {
      var now = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

      taskService.writeTtlFile(
        Constants.DELTA_INSERTS_GRAPH_PREFIX,
        model,
        now+"-inserts.ttl"
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

    public Dataset getAllChanges(LocalDateTime since, LocalDateTime snapshot) {
      Dataset d = DatasetFactory.create(getResponseMeta(Constants.DELTA_CHANGES_GRAPH_PREFIX));
      
      d.addNamedModel(
        Constants.DELTA_DELETES_GRAPH_PREFIX,
        getGraph(
          Constants.DELTA_DELETES_GRAPH_PREFIX,
          since,
          snapshot
        )
      );
      
      d.addNamedModel(
        Constants.DELTA_INSERTS_GRAPH_PREFIX,
        getGraph(
          Constants.DELTA_INSERTS_GRAPH_PREFIX,
          since,
          snapshot
        )
      );

      return d;
    }

    public Dataset getConsolidated(LocalDateTime since, LocalDateTime snapshot) {
      Dataset d = DatasetFactory.create(getResponseMeta(Constants.DELTA_CONSOLIDATED_GRAPH_PREFIX));

      d.addNamedModel(
        Constants.DELTA_CONSOLIDATED_GRAPH_PREFIX, 
        getConsolidatedGraph(since, snapshot)
      );

      return d;
    }

    public Model getConsolidatedGraph(LocalDateTime since, LocalDateTime snapshot) {
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

    private Model getResponseMeta(String graphName) {
      Model m = ModelFactory.createDefaultModel();

      m.add(
        ResourceFactory.createResource(graphName),
        ResourceFactory.createProperty(Constants.DC_DATE),
        ResourceFactory.createTypedLiteral(
          ModelUtils.formattedDate(LocalDateTime.now()),
          org.apache.jena.datatypes.xsd.XSDDatatype.XSDdateTime
        )
      );

      return m;
    }
}
