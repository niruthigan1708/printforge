package com.printforge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.printforge.entity.Category;
import com.printforge.entity.Role;
import com.printforge.repository.CategoryRepository;
import com.printforge.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Over-long or missing input must be a clear 400, never a database error surfacing as a 500. */
@SpringBootTest @AutoConfigureMockMvc
class InputLimitsTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired CategoryRepository categories;
  @Autowired TestTokens tokens;

  private String admin() { return "Bearer " + tokens.tokenFor("limits-admin@example.com", Role.ADMIN); }
  private String customer() { return "Bearer " + tokens.tokenFor("limits-customer@example.com", Role.CUSTOMER); }

  @Test void overLongProductNameIsRejectedNamingTheField() throws Exception {
    Category category = categories.save(new Category("Limits Category", "desc"));
    var body = Map.of("name", "x".repeat(151), "description", "desc", "price", "100.00", "stockQuantity", 1,
      "material", "PLA", "color", "Black", "imageUrl", "", "categoryId", category.getId(), "active", true);

    mvc.perform(post("/api/products").header("Authorization", admin()).contentType("application/json").content(json.writeValueAsString(body)))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.message", startsWith("Validation failed: name")))
      .andExpect(jsonPath("$.errors.name").exists());
  }

  @Test void overLongProductDescriptionIsRejected() throws Exception {
    Category category = categories.save(new Category("Limits Description Category", "desc"));
    var body = Map.of("name", "Fine", "description", "x".repeat(2001), "price", "100.00", "stockQuantity", 1,
      "material", "PLA", "color", "Black", "imageUrl", "", "categoryId", category.getId(), "active", true);

    mvc.perform(post("/api/products").header("Authorization", admin()).contentType("application/json").content(json.writeValueAsString(body)))
      .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.description").exists());
  }

  @Test void overLongCategoryDescriptionIsRejected() throws Exception {
    mvc.perform(post("/api/categories").header("Authorization", admin()).contentType("application/json")
        .content(json.writeValueAsString(Map.of("name", "Long Description", "description", "x".repeat(256)))))
      .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.description").exists());
  }

  @Test void overLongShippingAddressIsRejected() throws Exception {
    Map<String, Object> body = new HashMap<>(Map.of("items", List.of(Map.of("productId", 1, "quantity", 1)),
      "shippingName", "Jane", "shippingPhone", "0771234567", "shippingAddress", "x".repeat(256), "shippingCity", "Colombo", "shippingPostalCode", "00300"));

    mvc.perform(post("/api/orders").header("Authorization", customer()).contentType("application/json").content(json.writeValueAsString(body)))
      .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.shippingAddress").exists());
  }

  @Test void registeringWithoutAPasswordIsRejected() throws Exception {
    mvc.perform(post("/api/auth/register").contentType("application/json")
        .content(json.writeValueAsString(Map.of("name", "No Password", "email", "nopassword@example.com"))))
      .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.password").exists());
  }

  @Test void overLongCustomPrintNotesAreRejected() throws Exception {
    MockMultipartFile file = new MockMultipartFile("file", "model.stl", "model/stl", "solid".getBytes());
    mvc.perform(multipart("/api/custom-prints").file(file).param("material", "PLA").param("color", "Black").param("quantity", "1")
        .param("notes", "x".repeat(2001)).header("Authorization", customer()))
      .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message", containsString("Notes")));
  }

  @Test void overLongUploadFileNameIsTruncatedNotRejected() throws Exception {
    MockMultipartFile file = new MockMultipartFile("file", "m".repeat(300) + ".stl", "model/stl", "solid".getBytes());
    mvc.perform(multipart("/api/custom-prints").file(file).param("material", "PLA").param("color", "Black").param("quantity", "1")
        .header("Authorization", customer()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.fileName", allOf(endsWith(".stl"), hasLength(200))));
  }

  @Test void uploadFileNameWithCharactersIllegalOnWindowsIsAccepted() throws Exception {
    MockMultipartFile file = new MockMultipartFile("file", "C:\\Users\\me\\what?<model>:v2.stl", "model/stl", "solid".getBytes());
    mvc.perform(multipart("/api/custom-prints").file(file).param("material", "PLA").param("color", "Black").param("quantity", "1")
        .header("Authorization", customer()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.fileName").value("what?<model>:v2.stl"));
  }
}
