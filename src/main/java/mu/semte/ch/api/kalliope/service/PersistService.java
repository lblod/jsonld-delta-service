package mu.semte.ch.api.kalliope.service;

import java.time.LocalDateTime;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.springframework.stereotype.Service;

import mu.semte.ch.api.kalliope.Constants;
import mu.semte.ch.lib.utils.ModelUtils;


@Service
public class PersistService {
  private final TaskService taskService;

  public PersistService(TaskService taskService) {
    this.taskService = taskService;
  }

  public Dataset getConsolidated() {
    Dataset d = DatasetFactory.create(getResponseMeta(Constants.DELTA_CONSOLIDATED_GRAPH_PREFIX));

    d.addNamedModel(
            Constants.DELTA_CONSOLIDATED_GRAPH_PREFIX,
            getConsolidatedGraph()
    );

    return d;
  }

  public Model getConsolidatedGraph() {
    return taskService.fetchModelFromDump(Constants.ORGANIZATIONS_DUMP_SUBJECT);
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
