package com.printforge;

import com.printforge.entity.Role;
import com.printforge.repository.CustomPrintRequestRepository;
import com.printforge.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A custom print upload whose database save fails must not leave its model file on disk. */
@SpringBootTest @AutoConfigureMockMvc
class UploadRollbackTest {
  @Autowired MockMvc mvc;
  @Autowired TestTokens tokens;
  @MockitoSpyBean CustomPrintRequestRepository requests;
  @Value("${app.upload-dir}") String uploadDir;

  private Set<String> storedFiles() throws IOException {
    Path dir = Path.of(uploadDir);
    if (!Files.exists(dir)) return Set.of();
    try (Stream<Path> files = Files.list(dir)) { return files.map(p -> p.getFileName().toString()).collect(Collectors.toSet()); }
  }

  @Test void failedSaveRemovesTheStoredFile() throws Exception {
    String token = tokens.tokenFor("rollback-customer@example.com", Role.CUSTOMER);
    doThrow(new IllegalStateException("simulated database failure")).when(requests).saveAndFlush(any());
    Set<String> before = storedFiles();

    MockMultipartFile file = new MockMultipartFile("file", "part.stl", "model/stl", "solid part".getBytes());
    mvc.perform(multipart("/api/custom-prints").file(file).param("material", "PLA").param("color", "Black").param("quantity", "1")
        .header("Authorization", "Bearer " + token))
      .andExpect(status().isInternalServerError()); // an unexpected database failure

    assertEquals(before, storedFiles(), "the model file written before the failed save must be deleted");
  }
}
