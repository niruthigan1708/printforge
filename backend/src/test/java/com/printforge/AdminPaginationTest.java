package com.printforge;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.printforge.entity.Category;
import com.printforge.entity.Product;
import com.printforge.entity.Role;
import com.printforge.repository.CategoryRepository;
import com.printforge.repository.ProductRepository;
import com.printforge.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Admin lists are paged instead of returning every row. */
@SpringBootTest @AutoConfigureMockMvc
class AdminPaginationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired CategoryRepository categories;
  @Autowired ProductRepository products;
  @Autowired TestTokens tokens;

  private String admin() { return "Bearer " + tokens.tokenFor("paging-admin@example.com", Role.ADMIN); }

  private JsonNode page(String path, int page, int size) throws Exception {
    return json.readTree(mvc.perform(get(path).param("page", String.valueOf(page)).param("size", String.valueOf(size)).header("Authorization", admin()))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
  }

  @Test void productPagesDoNotOverlapAndReportTotals() throws Exception {
    Category category = categories.save(new Category("Paging Category", "desc"));
    for (int i = 0; i < 5; i++) {
      Product product = new Product();
      product.setName("Paged " + i);
      product.setDescription("desc");
      product.setPrice(new BigDecimal("10.00"));
      product.setStockQuantity(1);
      product.setMaterial("PLA");
      product.setColor("Black");
      product.setCategory(category);
      products.save(product);
    }

    JsonNode first = page("/api/admin/products", 0, 2);
    JsonNode second = page("/api/admin/products", 1, 2);
    assertEquals(2, first.get("content").size());
    assertEquals(2, first.get("size").asInt());
    assertEquals(0, first.get("page").asInt());
    assertTrue(first.get("totalElements").asLong() >= 5);
    assertEquals((first.get("totalElements").asLong() + 1) / 2, first.get("totalPages").asLong());

    Set<Long> ids = new HashSet<>();
    first.get("content").forEach(p -> ids.add(p.get("id").asLong()));
    second.get("content").forEach(p -> assertTrue(ids.add(p.get("id").asLong()), "pages must not repeat items"));
  }

  @Test void allAdminListsArePaged() throws Exception {
    for (String path : new String[] { "/api/admin/orders", "/api/admin/products", "/api/admin/custom-prints" }) {
      mvc.perform(get(path).header("Authorization", admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.totalElements").isNumber());
    }
  }

  @Test void invalidPagingIsRejected() throws Exception {
    mvc.perform(get("/api/admin/orders").param("size", "101").header("Authorization", admin())).andExpect(status().isBadRequest());
    mvc.perform(get("/api/admin/orders").param("page", "-1").header("Authorization", admin())).andExpect(status().isBadRequest());
  }
}
