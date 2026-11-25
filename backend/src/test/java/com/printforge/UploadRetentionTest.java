package com.printforge;

import tools.jackson.databind.ObjectMapper;
import com.printforge.entity.CustomPrintRequest;
import com.printforge.entity.Role;
import com.printforge.repository.CustomPrintRequestRepository;
import com.printforge.service.CustomPrintService;
import com.printforge.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Model files of long-closed requests are deleted; everything still needed is kept. */
@SpringBootTest @AutoConfigureMockMvc
class UploadRetentionTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired TestTokens tokens;
  @Autowired CustomPrintRequestRepository requests;
  @Autowired CustomPrintService customPrints;

  private String customer() { return "Bearer " + tokens.tokenFor("retention-customer@example.com", Role.CUSTOMER); }
  private String admin() { return "Bearer " + tokens.tokenFor("retention-admin@example.com", Role.ADMIN); }

  private long submit() throws Exception {
    MockMultipartFile file = new MockMultipartFile("file", "part.stl", "model/stl", "solid part".getBytes());
    String body = mvc.perform(multipart("/api/custom-prints").file(file).param("material", "PLA").param("color", "Black").param("quantity", "1").header("Authorization", customer()))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    return json.readTree(body).get("id").asLong();
  }

  private void adminStatus(long id, String status) throws Exception {
    mvc.perform(put("/api/admin/custom-prints/" + id + "/status").header("Authorization", admin()).contentType("application/json").content("{\"status\":\"" + status + "\"}")).andExpect(status().isOk());
  }

  private void quote(long id) throws Exception {
    mvc.perform(put("/api/admin/custom-prints/" + id + "/quote").header("Authorization", admin()).contentType("application/json").content("{\"amount\":\"900.00\"}")).andExpect(status().isOk());
  }

  /** Pretends the request was last changed {@code days} days ago. */
  private void age(long id, int days) {
    CustomPrintRequest request = requests.findById(id).orElseThrow();
    request.setUpdatedAt(Instant.now().minus(Duration.ofDays(days)));
    requests.save(request);
  }

  private boolean fileOnDisk(long id) { return Files.exists(Path.of(requests.findById(id).orElseThrow().getFilePath())); }

  @Test void onlyOldRejectedOrCancelledRequestsLoseTheirFile() throws Exception {
    long cancelledOld = submit();
    mvc.perform(put("/api/custom-prints/" + cancelledOld + "/cancel").header("Authorization", customer())).andExpect(status().isOk());
    age(cancelledOld, 40);

    long rejectedOld = submit();
    quote(rejectedOld);
    mvc.perform(put("/api/custom-prints/" + rejectedOld + "/reject").header("Authorization", customer())).andExpect(status().isOk());
    age(rejectedOld, 40);

    long rejectedRecent = submit();
    quote(rejectedRecent);
    mvc.perform(put("/api/custom-prints/" + rejectedRecent + "/reject").header("Authorization", customer())).andExpect(status().isOk());
    age(rejectedRecent, 5);

    long completedOld = submit();
    quote(completedOld);
    mvc.perform(put("/api/custom-prints/" + completedOld + "/accept").header("Authorization", customer())).andExpect(status().isOk());
    adminStatus(completedOld, "IN_PRODUCTION");
    adminStatus(completedOld, "COMPLETED");
    age(completedOld, 400);

    long openOld = submit();
    age(openOld, 400);

    customPrints.purgeClosedRequestFiles(Instant.now().minus(Duration.ofDays(30)));

    assertFalse(fileOnDisk(cancelledOld), "old cancelled request's file should be deleted");
    assertFalse(fileOnDisk(rejectedOld), "old rejected request's file should be deleted");
    assertTrue(fileOnDisk(rejectedRecent), "recently rejected request keeps its file");
    assertTrue(fileOnDisk(completedOld), "completed requests keep their file for reprints");
    assertTrue(fileOnDisk(openOld), "open requests keep their file however old");
    assertNotNull(requests.findById(rejectedOld).orElseThrow().getFilePurgedAt());
    assertNull(requests.findById(completedOld).orElseThrow().getFilePurgedAt());
  }

  @Test void purgedFileIsReportedAsGoneNotBroken() throws Exception {
    long id = submit();
    mvc.perform(put("/api/custom-prints/" + id + "/cancel").header("Authorization", customer())).andExpect(status().isOk());
    age(id, 40);
    customPrints.purgeClosedRequestFiles(Instant.now().minus(Duration.ofDays(30)));

    mvc.perform(get("/api/admin/custom-prints/" + id + "/file").header("Authorization", admin()))
      .andExpect(status().isGone())
      .andExpect(jsonPath("$.message", containsString("deleted after this request was closed")));
    mvc.perform(get("/api/admin/custom-prints/" + id).header("Authorization", admin()))
      .andExpect(jsonPath("$.fileAvailable").value(false))
      .andExpect(jsonPath("$.status").value("CANCELLED"));
  }

  @Test void purgingTwiceIsHarmless() throws Exception {
    long id = submit();
    mvc.perform(put("/api/custom-prints/" + id + "/cancel").header("Authorization", customer())).andExpect(status().isOk());
    age(id, 40);
    Instant cutoff = Instant.now().minus(Duration.ofDays(30));
    customPrints.purgeClosedRequestFiles(cutoff);
    Instant firstPurge = requests.findById(id).orElseThrow().getFilePurgedAt();

    customPrints.purgeClosedRequestFiles(cutoff);
    assertEquals(firstPurge, requests.findById(id).orElseThrow().getFilePurgedAt(), "already-purged requests are skipped");
  }
}
