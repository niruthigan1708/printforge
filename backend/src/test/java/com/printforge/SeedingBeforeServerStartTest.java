package com.printforge;

import com.printforge.repository.ProductRepository;
import com.printforge.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Demo data must be in place before the server accepts its first request; seeding used to run
 * after Tomcat was already serving, so early requests saw an empty shop and demo logins failed.
 * Uses its own in-memory database so the demo data does not leak into other tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
  "app.seed.enabled=true",
  "spring.datasource.url=jdbc:h2:mem:seeding-order;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
})
class SeedingBeforeServerStartTest {
  /** Products in the database at the moment the web server reported it had started. */
  static final AtomicLong productsWhenServerStarted = new AtomicLong(-1);
  static final AtomicLong usersWhenServerStarted = new AtomicLong(-1);

  @TestConfiguration
  static class RecordStateAtServerStart {
    @Bean ApplicationListener<WebServerInitializedEvent> recordSeedState(ProductRepository products, UserRepository users) {
      return event -> {
        productsWhenServerStarted.set(products.count());
        usersWhenServerStarted.set(users.count());
      };
    }
  }

  @LocalServerPort int port;
  @Autowired UserRepository users;

  @Test void demoDataExistsBeforeTheServerAcceptsRequests() throws Exception {
    assertEquals(15, productsWhenServerStarted.get(), "all demo products must exist when the server starts");
    assertEquals(2, usersWhenServerStarted.get(), "both demo accounts must exist when the server starts");

    HttpResponse<String> login = HttpClient.newHttpClient().send(
      HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/login"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"admin@printforge.com\",\"password\":\"Admin@12345\"}")).build(),
      HttpResponse.BodyHandlers.ofString());
    assertEquals(200, login.statusCode());
  }
}
