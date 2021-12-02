package mu.semte.ch.api.kalliope.rest;

import org.apache.jena.riot.Lang;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import mu.semte.ch.api.kalliope.service.PersistService;
import mu.semte.ch.lib.utils.ModelUtils;

@RestController
public class AppController {

  private final PersistService persistService;

  public AppController(PersistService persistService) {
    this.persistService = persistService;
  }

  @GetMapping(value = "/consolidated",
              produces = "application/ld+json")
  public ResponseEntity<String> consolidated() {
            var dataset = persistService.getConsolidated();
            String response = ModelUtils.toString(dataset, Lang.JSONLD);
            return ResponseEntity.ok(response);
  }
}
