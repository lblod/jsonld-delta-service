package mu.semte.ch.api.kalliope.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.actuate.context.ShutdownEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@ConfigurationProperties("application.security")
@Order(0)
@Slf4j
public class SecurityConfig extends WebSecurityConfigurerAdapter implements ApplicationContextAware, CommandLineRunner {
  @Value("${application.security.source}")
  private String applicationSecuritySource;
  @Value("${application.security.output}")
  private String applicationSecurityConfig;

  @Setter
  private List<String> allowedIpAddresses;

  private final ObjectMapper mapper = new ObjectMapper();


  @Override
  protected void configure(HttpSecurity http) throws Exception {
    var whitelist = allowedIpAddresses.stream().collect(Collectors.joining("') or hasIpAddress('", "hasIpAddress('","')"));
    String accesses = "authenticated and (%s)".formatted(whitelist);
    log.warn("access: '{}'", accesses);
    http.cors().and()
        .authorizeRequests()
        .anyRequest().access(accesses)
        .and()
        .csrf()
        .disable()
        .httpBasic()
        .realmName("KALLIOPE")
        .and()
        .exceptionHandling()
        .authenticationEntryPoint( (req, resp, e) -> resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED))
        .and()
        .formLogin()
        .disable()
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Autowired
  public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
    var config = new File(applicationSecurityConfig);
    if (config.exists()) {
      var users = mapper.readValue(config, mu.semte.ch.api.kalliope.security.User[].class);
      var builder = auth.inMemoryAuthentication().passwordEncoder(passwordEncoder());
      Stream.of(users).forEach(u -> {
        var user = User.withUsername(u.getUsername()).password(u.getPassword()).roles(u.getRoles().toArray(new String[]{})).build();
        builder.withUser(user);
      });
    }

  }

  public void run(String... args) throws Exception {
    var source = new File(applicationSecuritySource);
    var config = new File(applicationSecurityConfig);

    if (source.exists()) {
      var users = mapper.readValue(source, mu.semte.ch.api.kalliope.security.User[].class);
      var encodedUsers = Stream.of(users).map(u -> u.toBuilder().password(passwordEncoder().encode(u.getPassword())).build())
                               .collect(Collectors.toList());
      var bytes = mapper.writeValueAsBytes(encodedUsers);

      FileUtils.writeByteArrayToFile(config, bytes);
      source.delete();
      log.warn("stopping application. Please restart it to apply changes.");
      ShutdownEndpoint shutdownEndpoint = new ShutdownEndpoint();
      shutdownEndpoint.setApplicationContext(this.getApplicationContext());
      shutdownEndpoint.shutdown();
    }
    else {
      log.warn("source doesn't exist");
    }
  }
}
