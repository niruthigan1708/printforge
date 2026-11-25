package com.printforge.controller;

import com.printforge.dto.CustomPrintRequestResponse;
import com.printforge.dto.PageResponse;
import com.printforge.service.CustomPrintService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController @RequestMapping("/api/custom-prints") @RequiredArgsConstructor
public class CustomPrintController {
  private final CustomPrintService service;

  @PostMapping(consumes = "multipart/form-data")
  public CustomPrintRequestResponse create(Authentication auth, @RequestParam MultipartFile file, @RequestParam String material, @RequestParam String color, @RequestParam int quantity, @RequestParam(required = false) String notes) {
    return service.create(auth.getName(), file, material, color, quantity, notes);
  }

  /** The caller's custom print requests, newest first, paged: ?page=0&size=20 (max 100). */
  @GetMapping public PageResponse<CustomPrintRequestResponse> mine(Authentication auth, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return service.mine(auth.getName(), PageResponse.newestFirst(page, size));
  }
  @GetMapping("/{id}") public CustomPrintRequestResponse get(Authentication auth, @PathVariable Long id) { return service.getMine(auth.getName(), id); }
  @PutMapping("/{id}/accept") public CustomPrintRequestResponse accept(Authentication auth, @PathVariable Long id) { return service.accept(auth.getName(), id); }
  @PutMapping("/{id}/reject") public CustomPrintRequestResponse reject(Authentication auth, @PathVariable Long id) { return service.reject(auth.getName(), id); }
  /** Customers can withdraw a request until it has been quoted. */
  @PutMapping("/{id}/cancel") public CustomPrintRequestResponse cancel(Authentication auth, @PathVariable Long id) { return service.cancelMine(auth.getName(), id); }
}
