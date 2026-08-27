package com.printforge.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(long totalProducts, long totalOrders, long pendingOrders, long customRequests, BigDecimal revenue, List<OrderResponse> recentOrders, List<CustomPrintRequestResponse> recentCustomRequests) { }
