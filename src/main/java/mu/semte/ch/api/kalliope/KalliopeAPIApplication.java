package mu.semte.ch.api.kalliope;

import mu.semte.ch.lib.config.CoreConfig;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@Import(CoreConfig.class)
public class KalliopeAPIApplication {
  public static void main(String[] args) {
    SpringApplication.run(KalliopeAPIApplication.class, args);
  }
}
