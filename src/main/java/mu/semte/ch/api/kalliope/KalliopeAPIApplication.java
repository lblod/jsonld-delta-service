package mu.semte.ch.api.kalliope;

import mu.semte.ch.lib.config.CoreConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@Import(CoreConfig.class)
public class KalliopeAPIApplication {
  public static void main(String[] args) {
    SpringApplication.run(KalliopeAPIApplication.class, args);
  }
}
