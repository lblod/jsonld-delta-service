package mu.semte.ch.api.kalliope;

public interface Constants {
  String HEADER_MU_AUTH_SUDO = "mu-auth-sudo";

  String LOGICAL_FILE_PREFIX = "http://data.lblod.info/id/files";
  String FILTER_GRAPH_PREFIX = "http://mu.semte.ch/graphs/harvesting/tasks/filtering";
  String VALIDATING_GRAPH_PREFIX = "http://mu.semte.ch/graphs/harvesting/tasks/validating";
  String DELTA_INSERTS_GRAPH_PREFIX = "http://mu.semte.ch/graphs/kalliope/inserts";
  String DELTA_DELETES_GRAPH_PREFIX = "http://mu.semte.ch/graphs/kalliope/deletes";
  String DELTA_CHANGES_GRAPH_PREFIX = "http://mu.semte.ch/graphs/kalliope/changes";
  String DELTA_CONSOLIDATED_GRAPH_PREFIX = "http://mu.semte.ch/graphs/kalliope/consolidated";
  String ERROR_URI_PREFIX = "http://redpencil.data.gift/id/jobs/error/";

  String DELETE_ACTION = "http://schema.org/DeleteAction";
  String INSERT_ACTION = "http://schema.org/InsertAction";

  String ORGANIZATIONS_PRODUCER_GRAPH = "http://redpencil.data.gift/id/deltas/producer/organizations";

  String DC_DATE = "http://purl.org/dc/terms/date";
}
