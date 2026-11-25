package com.printforge;

import tools.jackson.databind.ObjectMapper;
import com.printforge.entity.Category;
import com.printforge.entity.Product;
import com.printforge.entity.Role;
import com.printforge.repository.CategoryRepository;
import com.printforge.repository.ProductRepository;
import com.printforge.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** A customer's own orders and requests are paged and never include anyone else's. */
@SpringBootTest @AutoConfigureMockMvc
class CustomerListsTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired TestTokens tokens;
  @Autowired CategoryRepository categories;
  @Autowired ProductRepository products;

  @Test void ordersArePagedAndPrivate() throws Exception {
    Category category = categories.save(new Category("Customer Lists Category", "desc"));
    Product product = new Product();
    product.setName("Customer Lists Stand");
    product.setDescription("desc");
    product.setPrice(new BigDecimal("100.00"));
    product.setStockQuantity(50);
    product.setMaterial("PLA");
    product.setColor("Black");
    product.setCategory(category);
    product = products.save(product);

    String alice = "Bearer " + tokens.tokenFor("lists-alice@example.com", Role.CUSTOMER);
    String bob = "Bearer " + tokens.tokenFor("lists-bob@example.com", Role.CUSTOMER);
    var order = json.writeValueAsString(Map.of("items", List.of(Map.of("productId", product.getId(), "quantity", 1)),
      "shippingName", "Jane", "shippingPhone", "0771234567", "shippingAddress", "1 Road", "shippingCity", "Colombo", "shippingPostalCode", "00300"));
    for (int i = 0; i < 3; i++) mvc.perform(post("/api/orders").header("Authorization", alice).contentType("application/json").content(order)).andExpect(status().isOk());
    mvc.perform(post("/api/orders").header("Authorization", bob).contentType("application/json").content(order)).andExpect(status().isOk());

    mvc.perform(get("/api/orders").param("size", "2").header("Authorization", alice))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content.length()").value(2))
      .andExpect(jsonPath("$.totalElements").value(3))
      .andExpect(jsonPath("$.totalPages").value(2))
      .andExpect(jsonPath("$.content[*].customerEmail", everyItem(is("lists-alice@example.com"))));
    mvc.perform(get("/api/orders").param("page", "1").param("size", "2").header("Authorization", alice))
      .andExpect(jsonPath("$.content.length()").value(1));
  }

  @Test void customPrintRequestsArePagedAndPrivate() throws Exception {
    String carol = "Bearer " + tokens.tokenFor("lists-carol@example.com", Role.CUSTOMER);
    String dave = "Bearer " + tokens.tokenFor("lists-dave@example.com", Role.CUSTOMER);
    MockMultipartFile file = new MockMultipartFile("file", "part.stl", "model/stl", "solid part".getBytes());
    for (String who : List.of(carol, carol, dave)) {
      mvc.perform(multipart("/api/custom-prints").file(file).param("material", "PLA").param("color", "Black").param("quantity", "1").header("Authorization", who))
        .andExpect(status().isOk());
    }

    mvc.perform(get("/api/custom-prints").header("Authorization", carol))
      .andExpect(jsonPath("$.totalElements").value(2))
      .andExpect(jsonPath("$.content[*].customerEmail", everyItem(is("lists-carol@example.com"))));
  }

  @Test void invalidPagingIsRejected() throws Exception {
    mvc.perform(get("/api/orders").param("size", "500").header("Authorization", "Bearer " + tokens.tokenFor("lists-eve@example.com", Role.CUSTOMER)))
      .andExpect(status().isBadRequest());
  }
}
