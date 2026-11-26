package com.printforge.dto;

import java.math.BigDecimal;
import java.util.List;

/** @param deliveryIssues delivered orders whose customer reported non-delivery and that are not yet resolved */
public record DashboardResponse(long totalProducts, long totalOrders, long pendingOrders, long deliveryIssues, long customRequests, BigDecimal revenue, List<OrderResponse> recentOrders, List<CustomPrintRequestResponse> recentCustomRequests) { }
