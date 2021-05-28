package mu.semte.ch.api.kalliope.rest;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.jena.riot.Lang;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import mu.semte.ch.lib.dto.Delta;
import mu.semte.ch.lib.utils.ModelUtils;
import mu.semte.ch.api.kalliope.service.PersistService;

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

  @PostMapping("/bulk")
  public ResponseEntity<Void> bulk(@RequestBody String dataModel) {
    persistService.writeBulk(ModelUtils.toModel(dataModel, Lang.TURTLE.getName()));
    
    return ResponseEntity.accepted().build();
  }
  

  @GetMapping(value = "/changes",
  produces = "application/ld+json")
  public ResponseEntity<String> change(
  @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since, 
  @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime snapshot) {
    
    var dataset = persistService.getAllChanges(since, snapshot);
    String response = ModelUtils.toString(dataset, Lang.JSONLD);
    return ResponseEntity.ok(response); 
  }

  // @GetMapping(value = "/inserts",
  // produces = "application/ld+json")
  // public ResponseEntity<String> insert(
  // @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since, 
  // @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime snapshot) {
    
  //   var model = persistService.getAllInserts(since, snapshot);
  //   String response = ModelUtils.toString(model, Lang.JSONLD);
  //   return ResponseEntity.ok(response); 
  // }
  
  // @GetMapping(value = "/deletes",
  // produces = "application/ld+json")
  // public ResponseEntity<String> deletes(
  // @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since, 
  // @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime snapshot) {
  //   var model = persistService.getAllDeletes(since, snapshot);
  //   String response = ModelUtils.toString(model, Lang.JSONLD);
  //   return ResponseEntity.ok(response); 
  // }
  
  @GetMapping(value = "/consolidated",
  produces = "application/ld+json")
  public ResponseEntity<String> consolidated(
  @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since, 
  @RequestParam(required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime snapshot) {
    var dataset = persistService.getConsolidated(since, snapshot);
    String response = ModelUtils.toString(dataset, Lang.JSONLD);
    return ResponseEntity.ok(response); 
  }
}
