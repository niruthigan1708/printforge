package com.printforge.controller;

import com.printforge.dto.DashboardResponse;
import com.printforge.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/admin/dashboard") @RequiredArgsConstructor
public class AdminDashboardController {
  private final DashboardService service;
  @GetMapping public DashboardResponse summary() { return service.summary(); }
}
