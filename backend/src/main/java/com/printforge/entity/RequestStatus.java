package com.printforge.entity;

import java.util.List;

/**
 * Custom print lifecycle:
 * SUBMITTED → UNDER_REVIEW → QUOTED → ACCEPTED → IN_PRODUCTION → COMPLETED, or REJECTED / CANCELLED.
 *
 * <p>Who drives each step:
 * <ul>
 *   <li>QUOTED is reached only through the admin quote endpoint, which sets the price.</li>
 *   <li>ACCEPTED / REJECTED are the customer's answer to a quote; an admin cannot set them.</li>
 *   <li>Every other move goes through the admin status endpoint, limited to {@link #adminNextStatuses()}.</li>
 * </ul>
 * REJECTED, COMPLETED and CANCELLED are final.
 */
public enum RequestStatus {
  SUBMITTED, UNDER_REVIEW, QUOTED, ACCEPTED, REJECTED, IN_PRODUCTION, COMPLETED, CANCELLED;

  public boolean isTerminal() { return this == REJECTED || this == COMPLETED || this == CANCELLED; }

  /** The statuses an admin may set directly from this status via the status endpoint. */
  public List<RequestStatus> adminNextStatuses() {
    return switch (this) {
      case SUBMITTED -> List.of(UNDER_REVIEW, CANCELLED);
      case UNDER_REVIEW, QUOTED -> List.of(CANCELLED);
      case ACCEPTED -> List.of(IN_PRODUCTION, CANCELLED);
      case IN_PRODUCTION -> List.of(COMPLETED, CANCELLED);
      case REJECTED, COMPLETED, CANCELLED -> List.of();
    };
  }

  public boolean adminCanTransitionTo(RequestStatus target) { return adminNextStatuses().contains(target); }

  /** A quote can be sent, or revised, until the customer has answered it. */
  public boolean canBeQuoted() { return this == SUBMITTED || this == UNDER_REVIEW || this == QUOTED; }

  /** Customers may withdraw a request until it is quoted; after that they accept or reject the quote. */
  public boolean customerCanCancel() { return this == SUBMITTED || this == UNDER_REVIEW; }
}
