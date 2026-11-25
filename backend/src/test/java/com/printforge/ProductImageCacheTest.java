package com.printforge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.printforge.entity.Category;
import com.printforge.entity.Product;
import com.printforge.entity.Role;
import com.printforge.repository.CategoryRepository;
import com.printforge.repository.ProductRepository;
import com.printforge.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** A replaced product photo must never be served stale from a browser cache. */
@SpringBootTest @AutoConfigureMockMvc
class ProductImageCacheTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired CategoryRepository categories;
  @Autowired ProductRepository products;
  @Autowired TestTokens tokens;

  private Product newProduct(String name) {
    Category category = categories.save(new Category(name + " Category", "desc"));
    Product product = new Product();
    product.setName(name);
    product.setDescription("desc");
    product.setPrice(new BigDecimal("100.00"));
    product.setStockQuantity(5);
    product.setMaterial("PLA");
    product.setColor("Black");
    product.setCategory(category);
    return products.save(product);
  }

  private String versionOf(Product product) throws Exception {
    String body = mvc.perform(get("/api/products/" + product.getId())).andReturn().getResponse().getContentAsString();
    return json.readTree(body).get("imageVersion").asText();
  }

  private String upload(Product product, String bytes) throws Exception {
    MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", bytes.getBytes());
    String body = mvc.perform(multipart("/api/products/" + product.getId() + "/image").file(file)
        .header("Authorization", "Bearer " + tokens.tokenFor("image-cache-admin@example.com", Role.ADMIN)))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    return json.readTree(body).get("imageVersion").asText();
  }

  @Test void unversionedImageUrlsMustBeRevalidated() throws Exception {
    Product product = newProduct("Cache Unversioned");
    mvc.perform(get("/api/products/" + product.getId() + "/image"))
      .andExpect(status().isOk())
      .andExpect(header().string("Cache-Control", containsString("no-cache")));
  }

  @Test void currentVersionedImageUrlsAreCachedLongTerm() throws Exception {
    Product product = newProduct("Cache Versioned");
    mvc.perform(get("/api/products/" + product.getId() + "/image").param("v", versionOf(product)))
      .andExpect(status().isOk())
      .andExpect(header().string("Cache-Control", allOf(containsString("max-age=31536000"), containsString("immutable"))));
  }

  @Test void uploadingAPhotoChangesTheVersionAndRetiresTheOldOne() throws Exception {
    Product product = newProduct("Cache Upload");
    String before = versionOf(product);
    Thread.sleep(5); // versions are millisecond timestamps
    String after = upload(product, "new-photo");

    assertNotEquals(before, after, "a new photo must get a new versioned URL");
    // The old version no longer names the current photo, so it must not be cached long-term.
    mvc.perform(get("/api/products/" + product.getId() + "/image").param("v", before))
      .andExpect(header().string("Cache-Control", containsString("no-cache")));
    mvc.perform(get("/api/products/" + product.getId() + "/image").param("v", after))
      .andExpect(status().isOk())
      .andExpect(content().bytes("new-photo".getBytes()))
      .andExpect(header().string("Cache-Control", containsString("immutable")));
  }

  @Test void unchangedUploadedPhotoRevalidatesAsNotModified() throws Exception {
    Product product = newProduct("Cache Revalidate");
    upload(product, "photo");
    String lastModified = mvc.perform(get("/api/products/" + product.getId() + "/image"))
      .andExpect(header().exists("Last-Modified")).andReturn().getResponse().getHeader("Last-Modified");

    mvc.perform(get("/api/products/" + product.getId() + "/image").header("If-Modified-Since", lastModified))
      .andExpect(status().isNotModified());
  }
}
