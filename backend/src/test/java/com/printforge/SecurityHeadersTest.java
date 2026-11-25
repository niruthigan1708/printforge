package com.printforge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

/** Every API response carries a locked-down set of browser security headers. */
@SpringBootTest @AutoConfigureMockMvc
class SecurityHeadersTest {
  @Autowired MockMvc mvc;

  @Test void jsonResponsesAreLockedDown() throws Exception {
    mvc.perform(get("/api/categories"))
      .andExpect(header().string("Content-Security-Policy", containsString("default-src 'none'")))
      .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
      .andExpect(header().string("X-Content-Type-Options", "nosniff"))
      .andExpect(header().string("X-Frame-Options", "DENY"))
      .andExpect(header().string("Referrer-Policy", "no-referrer"))
      .andExpect(header().string("Permissions-Policy", containsString("camera=()")));
  }

  @Test void hstsIsSentOverHttps() throws Exception {
    mvc.perform(get("/api/categories").secure(true))
      .andExpect(header().string("Strict-Transport-Security", containsString("max-age=")));
  }

  @Test void svgPlaceholdersCannotRunScriptsWhenOpenedDirectly() throws Exception {
    mvc.perform(get("/api/products/999999/image"))
      .andExpect(header().string("Content-Security-Policy", containsString("default-src 'none'")));
  }
}
