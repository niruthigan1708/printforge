package com.printforge.controller;

import com.printforge.dto.CustomPrintRequestResponse;
import com.printforge.dto.CustomPrintStatusUpdateRequest;
import com.printforge.dto.PageResponse;
import com.printforge.dto.QuoteRequest;
import com.printforge.entity.CustomPrintRequest;
import com.printforge.service.CustomPrintService;
import com.printforge.service.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@RestController @RequestMapping("/api/admin/custom-prints") @RequiredArgsConstructor
public class AdminCustomPrintController {
  private final CustomPrintService service;
  private final FileStorageService fileStorage;

  /** Newest first, paged: ?page=0&size=20 (max 100). */
  @GetMapping public PageResponse<CustomPrintRequestResponse> all(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return service.all(PageResponse.newestFirst(page, size)); }
  @GetMapping("/{id}") public CustomPrintRequestResponse get(@PathVariable Long id) { return service.get(id); }

  @GetMapping("/{id}/file")
  public ResponseEntity<Resource> file(@PathVariable Long id) {
    CustomPrintRequest request = service.findEntity(id);
    if (request.getFilePurgedAt() != null) {
      throw new ResponseStatusException(HttpStatus.GONE, "The model file was deleted after this request was closed (" + request.getStatus() + ")");
    }
    Path path = fileStorage.locate(request.getFilePath())
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "The uploaded model file is no longer available on the server"));
    // Built with ContentDisposition so quotes, semicolons and non-ASCII characters in the
    // customer's file name are escaped/encoded (RFC 6266 + RFC 5987) instead of breaking the header.
    ContentDisposition disposition = ContentDisposition.attachment().filename(request.getFileName(), StandardCharsets.UTF_8).build();
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .body(new FileSystemResource(path));
  }

  @PutMapping("/{id}/quote") public CustomPrintRequestResponse quote(@PathVariable Long id, @Valid @RequestBody QuoteRequest request) { return service.quote(id, request); }
  @PutMapping("/{id}/status") public CustomPrintRequestResponse status(@PathVariable Long id, @Valid @RequestBody CustomPrintStatusUpdateRequest request) { return service.updateStatus(id, request.status()); }
}
