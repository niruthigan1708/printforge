package com.printforge.controller;

import com.printforge.dto.CustomPrintRequestResponse;
import com.printforge.dto.CustomPrintStatusUpdateRequest;
import com.printforge.dto.QuoteRequest;
import com.printforge.entity.CustomPrintRequest;
import com.printforge.service.CustomPrintService;
import com.printforge.service.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/admin/custom-prints") @RequiredArgsConstructor
public class AdminCustomPrintController {
  private final CustomPrintService service;
  private final FileStorageService fileStorage;

  @GetMapping public List<CustomPrintRequestResponse> all() { return service.all(); }
  @GetMapping("/{id}") public CustomPrintRequestResponse get(@PathVariable Long id) { return service.get(id); }

  @GetMapping("/{id}/file")
  public ResponseEntity<Resource> file(@PathVariable Long id) {
    CustomPrintRequest request = service.findEntity(id);
    Resource resource = new FileSystemResource(fileStorage.resolve(request.getFilePath()));
    return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + request.getFileName() + "\"").body(resource);
  }

  @PutMapping("/{id}/quote") public CustomPrintRequestResponse quote(@PathVariable Long id, @Valid @RequestBody QuoteRequest request) { return service.quote(id, request); }
  @PutMapping("/{id}/status") public CustomPrintRequestResponse status(@PathVariable Long id, @Valid @RequestBody CustomPrintStatusUpdateRequest request) { return service.updateStatus(id, request.status()); }
}
