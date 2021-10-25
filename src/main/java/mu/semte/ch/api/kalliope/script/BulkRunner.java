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

  @Scheduled(initialDelay = 1000,
             fixedDelay = 60000) // run 30 seconds after startup & then every 60 seconds
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
                .forEach(f -> {
                  log.info("processing file {}", f.getName());
                  try {
                    persistService.writeBulk(ModelUtils.toModel(new FileInputStream(f), TURTLE.getName()), () -> this.renameFileAfterProcessed(f)); //rename file
                  }
                  catch (IOException e) {
                    log.error("an error occurred while processing file", e);
                  }

                });
      }
    }
  }

  @SneakyThrows
  private Void renameFileAfterProcessed(File f) {
    FileUtils.moveFile(f, new File(f.getAbsolutePath() + ".processed"));
    return null;
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
