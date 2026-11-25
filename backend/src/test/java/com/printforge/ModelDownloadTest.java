package com.printforge;

import tools.jackson.databind.ObjectMapper;
import com.printforge.entity.CustomPrintRequest;
import com.printforge.entity.Role;
import com.printforge.repository.CustomPrintRequestRepository;
import com.printforge.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ContentDisposition;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Admins must be able to download any uploaded model, whatever the customer named it. */
@SpringBootTest @AutoConfigureMockMvc
class ModelDownloadTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired CustomPrintRequestRepository requests;
  @Autowired TestTokens tokens;

  private String admin() { return "Bearer " + tokens.tokenFor("download-admin@example.com", Role.ADMIN); }

  private long upload(String fileName, String contents) throws Exception {
    MockMultipartFile file = new MockMultipartFile("file", fileName, "model/stl", contents.getBytes(StandardCharsets.UTF_8));
    String body = mvc.perform(multipart("/api/custom-prints").file(file).param("material", "PLA").param("color", "Black").param("quantity", "1")
        .header("Authorization", "Bearer " + tokens.tokenFor("download-customer@example.com", Role.CUSTOMER)))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    return json.readTree(body).get("id").asLong();
  }

  @Test void fileNamesWithQuotesAndNonAsciiCharactersDownloadIntact() throws Exception {
    String name = "my \"best\" módel; v2 ✓.stl";
    long id = upload(name, "solid model");

    String header = mvc.perform(get("/api/admin/custom-prints/" + id + "/file").header("Authorization", admin()))
      .andExpect(status().isOk())
      .andExpect(content().bytes("solid model".getBytes(StandardCharsets.UTF_8)))
      .andReturn().getResponse().getHeader("Content-Disposition");

    ContentDisposition parsed = ContentDisposition.parse(header);
    assertEquals("attachment", parsed.getType());
    assertEquals(name, parsed.getFilename(), "the name must round-trip through the header unchanged");
  }

  @Test void missingFileIsNotFoundWithAClearMessage() throws Exception {
    long id = upload("gone.stl", "solid");
    CustomPrintRequest request = requests.findById(id).orElseThrow();
    Files.delete(Path.of(request.getFilePath()));

    mvc.perform(get("/api/admin/custom-prints/" + id + "/file").header("Authorization", admin()))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message", containsString("no longer available")));
  }

  @Test void fileIsFoundAfterTheUploadDirectoryMoved() throws Exception {
    long id = upload("moved.stl", "solid moved");
    CustomPrintRequest request = requests.findById(id).orElseThrow();
    // Simulate a path recorded under an old location; the file itself is still in the current dir.
    String storedName = Path.of(request.getFilePath()).getFileName().toString();
    request.setFilePath(Path.of("old-location-that-no-longer-exists", storedName).toAbsolutePath().toString());
    requests.save(request);

    mvc.perform(get("/api/admin/custom-prints/" + id + "/file").header("Authorization", admin()))
      .andExpect(status().isOk())
      .andExpect(content().bytes("solid moved".getBytes(StandardCharsets.UTF_8)));
  }
}
