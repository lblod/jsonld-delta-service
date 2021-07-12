package mu.semte.ch.api.kalliope.rest;

import java.io.IOException;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
  public ResponseEntity<Void> bulk(@RequestParam("file") MultipartFile file,
  RedirectAttributes redirectAttributes) throws IOException {
    persistService.writeBulk(ModelUtils.toModel(file.getInputStream(), Lang.TURTLE.getName()));
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
