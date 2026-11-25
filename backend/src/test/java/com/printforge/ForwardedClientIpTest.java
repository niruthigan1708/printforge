package com.printforge;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs a real Tomcat (MockMvc skips the connector) to check that, behind a proxy, login throttling
 * counts each forwarded client separately instead of locking out everyone behind the proxy.
 * The test client connects from 127.0.0.1, a trusted proxy address by default.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
  "app.auth.max-failed-logins-per-ip=2",
  "app.auth.max-failed-logins-per-account=100",
})
class ForwardedClientIpTest {
  @LocalServerPort int port;
  @Autowired Environment environment;
  private final HttpClient http = HttpClient.newHttpClient();

  private int login(String email, String forwardedFor) throws Exception {
    HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/login"))
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}"));
    if (forwardedFor != null) request.header("X-Forwarded-For", forwardedFor);
    return http.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
  }

  @Test void eachForwardedClientIsThrottledSeparately() throws Exception {
    // Client A, through the proxy, exhausts its per-IP allowance.
    assertEquals(401, login("a1@example.com", "203.0.113.10"));
    assertEquals(401, login("a2@example.com", "203.0.113.10"));
    assertEquals(429, login("a3@example.com", "203.0.113.10"));

    // Client B, through the same proxy, is unaffected. Without forwarded-header support both
    // would share the proxy's IP and B would be locked out too.
    assertEquals(401, login("b1@example.com", "198.51.100.20"));
  }

  @Test void theLastHopIsTheClientWhenProxiesChain() throws Exception {
    // "spoofed, real": the client-supplied first entry must not let it dodge the limit.
    assertEquals(401, login("c1@example.com", "1.2.3.4, 203.0.113.99"));
    assertEquals(401, login("c2@example.com", "5.6.7.8, 203.0.113.99"));
    assertEquals(429, login("c3@example.com", "9.9.9.9, 203.0.113.99"));
  }

  /** The forwarded-header handling under test comes from the real application.yml, not this test. */
  @Test void productionConfigEnablesForwardedHeaders() {
    assertEquals("native", environment.getProperty("server.forward-headers-strategy"));
    assertTrue(environment.getProperty("server.tomcat.remoteip.internal-proxies").contains("127.0.0.0/8"));
  }
}
