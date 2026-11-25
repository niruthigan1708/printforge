package com.printforge;

import com.printforge.entity.Category;
import com.printforge.entity.Product;
import com.printforge.entity.Role;
import com.printforge.repository.CategoryRepository;
import com.printforge.repository.ProductRepository;
import com.printforge.support.TestFiles;
import com.printforge.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Uploads are accepted on their content, not just their extension. */
@SpringBootTest @AutoConfigureMockMvc
class UploadContentTest {
  @Autowired MockMvc mvc;
  @Autowired TestTokens tokens;
  @Autowired CategoryRepository categories;
  @Autowired ProductRepository products;

  @Test void renamedFileIsRejectedAsAModel() throws Exception {
    MockMultipartFile fake = new MockMultipartFile("file", "invoice.stl", "model/stl", "<html><body>not a model</body></html>".getBytes());
    mvc.perform(multipart("/api/custom-prints").file(fake).param("material", "PLA").param("color", "Black").param("quantity", "1")
        .header("Authorization", "Bearer " + tokens.tokenFor("upload-customer@example.com", Role.CUSTOMER)))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.message", containsString("not a valid STL")));
  }

  @Test void binaryStlIsAccepted() throws Exception {
    MockMultipartFile stl = new MockMultipartFile("file", "part.stl", "model/stl", TestFiles.binaryStl(4));
    mvc.perform(multipart("/api/custom-prints").file(stl).param("material", "PLA").param("color", "Black").param("quantity", "1")
        .header("Authorization", "Bearer " + tokens.tokenFor("upload-customer@example.com", Role.CUSTOMER)))
      .andExpect(status().isOk());
  }

  @Test void renamedFileIsRejectedAsAProductPhoto() throws Exception {
    Category category = categories.save(new Category("Upload Content Category", "desc"));
    Product product = new Product();
    product.setName("Upload Content");
    product.setDescription("desc");
    product.setPrice(new BigDecimal("100.00"));
    product.setStockQuantity(1);
    product.setMaterial("PLA");
    product.setColor("Black");
    product.setCategory(category);
    product = products.save(product);

    MockMultipartFile svg = new MockMultipartFile("file", "photo.png", "image/png", "<svg xmlns='http://www.w3.org/2000/svg' onload='alert(1)'/>".getBytes());
    mvc.perform(multipart("/api/products/" + product.getId() + "/image").file(svg)
        .header("Authorization", "Bearer " + tokens.tokenFor("upload-admin@example.com", Role.ADMIN)))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.message", containsString("not a valid PNG")));
  }
}
