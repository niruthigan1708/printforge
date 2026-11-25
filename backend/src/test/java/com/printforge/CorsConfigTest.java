package com.printforge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.cors.allowed-origins=https://shop.example.com, http://localhost:3000")
@AutoConfigureMockMvc
class CorsConfigTest {
  @Autowired MockMvc mvc;

  @Test void configuredOriginsArePermitted() throws Exception {
    for (String origin : new String[] { "https://shop.example.com", "http://localhost:3000" }) {
      mvc.perform(options("/api/products").header("Origin", origin).header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", origin));
    }
  }

  @Test void otherOriginsAreRefused() throws Exception {
    mvc.perform(options("/api/products").header("Origin", "https://evil.example").header("Access-Control-Request-Method", "GET"))
      .andExpect(status().isForbidden())
      .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
  }
}
