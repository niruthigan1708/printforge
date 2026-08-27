package com.printforge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.printforge.entity.Role;
import com.printforge.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class CustomPrintWorkflowTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired TestTokens tokens;

  @Test void customerCanSubmitACustomPrintRequest() throws Exception {
    String token = tokens.tokenFor("custom-print-customer@example.com", Role.CUSTOMER);
    MockMultipartFile file = new MockMultipartFile("file", "bracket.stl", "model/stl", "solid bracket".getBytes());

    mvc.perform(multipart("/api/custom-prints").file(file)
        .param("material", "PLA").param("color", "Black").param("quantity", "2").param("notes", "Please print sturdy")
        .header("Authorization", "Bearer " + token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.requestNumber").value(org.hamcrest.Matchers.startsWith("CR-")))
      .andExpect(jsonPath("$.status").value("SUBMITTED"));
  }

  @Test void unsupportedFileTypeIsRejected() throws Exception {
    String token = tokens.tokenFor("custom-print-customer-2@example.com", Role.CUSTOMER);
    MockMultipartFile file = new MockMultipartFile("file", "bracket.txt", "text/plain", "not a model".getBytes());

    mvc.perform(multipart("/api/custom-prints").file(file)
        .param("material", "PLA").param("color", "Black").param("quantity", "1")
        .header("Authorization", "Bearer " + token))
      .andExpect(status().isBadRequest());
  }

  @Test void adminCanQuoteAndCustomerCanAcceptTheRequest() throws Exception {
    String customerToken = tokens.tokenFor("custom-print-quote-customer@example.com", Role.CUSTOMER);
    String adminToken = tokens.tokenFor("custom-print-quote-admin@example.com", Role.ADMIN);
    MockMultipartFile file = new MockMultipartFile("file", "model.3mf", "model/3mf", "3mf-bytes".getBytes());

    String created = mvc.perform(multipart("/api/custom-prints").file(file)
        .param("material", "PETG").param("color", "Blue").param("quantity", "1")
        .header("Authorization", "Bearer " + customerToken))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    Long requestId = json.readTree(created).get("id").asLong();

    var quoteBody = Map.of("amount", "5500.00", "notes", "Includes finishing");
    mvc.perform(put("/api/admin/custom-prints/" + requestId + "/quote").header("Authorization", "Bearer " + adminToken).contentType("application/json").content(json.writeValueAsString(quoteBody)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("QUOTED"))
      .andExpect(jsonPath("$.adminQuote").value(5500.00));

    mvc.perform(put("/api/custom-prints/" + requestId + "/accept").header("Authorization", "Bearer " + customerToken))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("ACCEPTED"));
  }

  @Test void nonAdminCannotAccessAdminCustomPrintEndpoints() throws Exception {
    String customerToken = tokens.tokenFor("custom-print-blocked@example.com", Role.CUSTOMER);
    mvc.perform(get("/api/admin/custom-prints").header("Authorization", "Bearer " + customerToken)).andExpect(status().isForbidden());
  }
}
