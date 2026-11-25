package com.printforge.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Order lifecycle: PENDING → CONFIRMED → PRINTING → READY → SHIPPED → DELIVERED, or CANCELLED.
 *
 * <p>Orders only move forward (skipping steps is allowed, e.g. PENDING → PRINTING), and can be
 * cancelled until they ship. DELIVERED and CANCELLED are final.
 */
public enum OrderStatus {
  PENDING, CONFIRMED, PRINTING, READY, SHIPPED, DELIVERED, CANCELLED;

  public boolean isTerminal() { return this == DELIVERED || this == CANCELLED; }

  /** The statuses an admin may move an order in this status to. */
  public List<OrderStatus> nextStatuses() {
    List<OrderStatus> next = new ArrayList<>();
    if (isTerminal()) return next;
    for (OrderStatus candidate : values()) {
      if (candidate != CANCELLED && candidate.ordinal() > ordinal()) next.add(candidate);
    }
    if (ordinal() < SHIPPED.ordinal()) next.add(CANCELLED);
    return next;
  }

  public boolean canTransitionTo(OrderStatus target) { return nextStatuses().contains(target); }

  /** Customers may cancel on their own only before the studio has confirmed the order. */
  public boolean customerCanCancel() { return this == PENDING; }
}
