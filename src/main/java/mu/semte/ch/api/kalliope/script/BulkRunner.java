package mu.semte.ch.api.kalliope.script;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import mu.semte.ch.api.kalliope.service.PersistService;
import mu.semte.ch.lib.utils.ModelUtils;
import mu.semte.ch.lib.utils.SparqlClient;
import mu.semte.ch.lib.utils.SparqlQueryStore;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.riot.Lang;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static mu.semte.ch.lib.utils.ModelUtils.toModel;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.jena.riot.Lang.TURTLE;

@Component
@Slf4j
public class BulkRunner {
  private final PersistService persistService;
  private final SparqlClient sparqlClient;
  private final SparqlQueryStore queryStore;

  @Value("${application.bulk.directory}")
  private String applicationBulkDirectory;

  public BulkRunner(PersistService persistService, SparqlClient sparqlClient, SparqlQueryStore queryStore) {
    this.persistService = persistService;
    this.sparqlClient = sparqlClient;
    this.queryStore = queryStore;
  }

  @Scheduled(initialDelay = 30000,
             fixedDelay = 600000) // run 30 seconds after startup & then every 10 minutes
  public void run() throws IOException {
    var bulkDir = Paths.get(applicationBulkDirectory);
    if (isDatabaseUp() && Files.exists(bulkDir) && Files.isDirectory(bulkDir)) {
      try (Stream<Path> stream = Files.list(bulkDir)) {
        stream
                .peek(path -> log.debug(path.getFileName().toString()))
                .filter(path -> !Files.isDirectory(path) && TURTLE.getFileExtensions()
                                                                  .contains(getExtension(path.getFileName()
                                                                                             .toString())))
                .map(Path::toFile)
                .peek(f -> log.info("processing file {}", f.getName()))
                .forEach(this::writeBulk);
      }
    }
  }

  @SneakyThrows
  private Void renameFileAfterProcessed(File f) {
    FileUtils.moveFile(f, new File(f.getAbsolutePath() + ".processed"));
    return null;
  }

  @SneakyThrows
  private void writeBulk(File f) {
    persistService.writeBulk(toModel(new FileInputStream(f), TURTLE.getName()), () -> this.renameFileAfterProcessed(f));
  }

  private boolean isDatabaseUp() {
    try {
      var dummyResult = sparqlClient.executeSelectQueryAsListMap(queryStore.getQuery("dummyQuery"));
      log.debug("DB UP");
      return true;
    }
    catch (Exception e) {
      log.debug("DB NOT UP", e);
      return false;
    }
  }


}
