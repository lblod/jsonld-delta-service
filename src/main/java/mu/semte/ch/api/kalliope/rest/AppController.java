package mu.semte.ch.api.kalliope.rest;

import lombok.extern.slf4j.Slf4j;
import mu.semte.ch.api.kalliope.service.PersistService;
import mu.semte.ch.lib.dto.Delta;
import mu.semte.ch.lib.utils.ModelUtils;
import org.apache.jena.riot.Lang;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@Slf4j
public class AppController {

  private final PersistService persistService;

  public AppController(PersistService persistService) {
    this.persistService = persistService;
  }

  @PostMapping("/delta")
  public ResponseEntity<Void> delta(@RequestBody List<Delta> deltas) {
    if (deltas.isEmpty()) {
      log.error("No delta messages found in request body");
      return ResponseEntity.noContent().build();
    }

    // // NOTE: we don't wait as we do not want to keep hold off the connection.

    deltas.forEach(persistService::writeDelta);

    return ResponseEntity.ok().build();
  }

  @GetMapping(value = "/changes",
              produces = "application/ld+json")
  public ResponseEntity<String> change(
          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime snapshot) {

    var dataset = persistService.getAllChanges(since, snapshot);
    String response = ModelUtils.toString(dataset, Lang.JSONLD);
    return ResponseEntity.ok(response);
  }

  @GetMapping(value = "/consolidated",
              produces = "application/ld+json")
  public ResponseEntity<String> consolidated(
          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime snapshot) {
    var dataset = persistService.getConsolidated(since, snapshot);
    String response = ModelUtils.toString(dataset, Lang.JSONLD);
    return ResponseEntity.ok(response);
  }

  @GetMapping(
    value = "/test",
    produces = "application/ld+json")
  public ResponseEntity<String> test() {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://database:8890/sparql?query=CONSTRUCT%20%7B%20?s%20?p%20?o%20%7D%20WHERE%20%7BGRAPH%20%3Chttp://redpencil.data.gift/id/deltas/producer/organizations%3E%20%7B%20?s%20?p%20?o%20%7D%20%7D"))
        .GET()
        .header("Accept", "application/ld+json")
        .build();
    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      return ResponseEntity.ok(response.body());
    } catch (IOException | InterruptedException e) {
      log.error(e.getMessage());
      return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body("Internal server error");
    }
  }

  @GetMapping(
    value = "/test2",
    produces = "application/ld+json")
  public ResponseEntity<String> test2() {
    var dataset = persistService.getConsolidated();
    String response = ModelUtils.toString(dataset, Lang.JSONLD);
    return ResponseEntity.ok(response);
  }
}
