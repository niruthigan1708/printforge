package com.printforge.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Order lifecycle: PENDING → CONFIRMED → PRINTING → READY → SHIPPED → DELIVERED, or CANCELLED.
 *
 * <p>Orders only move forward and may skip production steps (e.g. PENDING → PRINTING), but an
 * order can only be DELIVERED after it has been SHIPPED, which needs a tracking number. Orders can
 * be cancelled until they ship. DELIVERED and CANCELLED are final.
 */
public enum OrderStatus {
  PENDING, CONFIRMED, PRINTING, READY, SHIPPED, DELIVERED, CANCELLED;

  public boolean isTerminal() { return this == DELIVERED || this == CANCELLED; }

  /** The statuses an admin may move an order in this status to. */
  public List<OrderStatus> nextStatuses() {
    List<OrderStatus> next = new ArrayList<>();
    if (isTerminal()) return next;
    for (OrderStatus candidate : values()) {
      if (candidate == CANCELLED || candidate.ordinal() <= ordinal()) continue;
      if (candidate == DELIVERED && this != SHIPPED) continue; // never "delivered" without a shipment
      next.add(candidate);
    }
    if (ordinal() < SHIPPED.ordinal()) next.add(CANCELLED);
    return next;
  }

  public boolean canTransitionTo(OrderStatus target) { return nextStatuses().contains(target); }

  /** Customers may cancel on their own only before the studio has confirmed the order. */
  public boolean customerCanCancel() { return this == PENDING; }
}
