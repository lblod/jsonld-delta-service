package mu.semte.ch.api.kalliope;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = {"mu.semte.ch"})
public class KalliopeAPIApplication {

  public static void main(String[] args) {
    SpringApplication.run(KalliopeAPIApplication.class, args);
  }

}
