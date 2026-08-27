package com.printforge.controller;

import com.printforge.dto.CustomPrintRequestResponse;
import com.printforge.service.CustomPrintService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController @RequestMapping("/api/custom-prints") @RequiredArgsConstructor
public class CustomPrintController {
  private final CustomPrintService service;

  @PostMapping(consumes = "multipart/form-data")
  public CustomPrintRequestResponse create(Authentication auth, @RequestParam MultipartFile file, @RequestParam String material, @RequestParam String color, @RequestParam int quantity, @RequestParam(required = false) String notes) {
    return service.create(auth.getName(), file, material, color, quantity, notes);
  }

  @GetMapping public List<CustomPrintRequestResponse> mine(Authentication auth) { return service.mine(auth.getName()); }
  @GetMapping("/{id}") public CustomPrintRequestResponse get(Authentication auth, @PathVariable Long id) { return service.getMine(auth.getName(), id); }
  @PutMapping("/{id}/accept") public CustomPrintRequestResponse accept(Authentication auth, @PathVariable Long id) { return service.accept(auth.getName(), id); }
  @PutMapping("/{id}/reject") public CustomPrintRequestResponse reject(Authentication auth, @PathVariable Long id) { return service.reject(auth.getName(), id); }
}
