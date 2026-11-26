package com.printforge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

/** One entry in an order's history: who did what, and when. */
@Entity @Getter @Setter @NoArgsConstructor
public class OrderEvent {
  public enum Type { PLACED, STATUS_CHANGED, RECEIVED, ISSUE_REPORTED, ISSUE_RESOLVED }

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @ManyToOne(optional = false, fetch = FetchType.LAZY) private Order order;
  @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Type eventType;
  /** The order's status after this event. */
  @Enumerated(EnumType.STRING) @Column(length = 30) private OrderStatus status;
  /** Copied from the account at the time, so history survives renames. Null for imported history. */
  private String actorName;
  private String actorEmail;
  @Enumerated(EnumType.STRING) @Column(length = 20) private Role actorRole;
  @Column(length = 2000) private String note;
  @Column(nullable = false) private Instant createdAt = Instant.now();

  static OrderEvent of(Order order, Type type, User actor, String note) {
    OrderEvent event = new OrderEvent();
    event.setOrder(order);
    event.setEventType(type);
    event.setStatus(order.getStatus());
    if (actor != null) {
      event.setActorName(actor.getName());
      event.setActorEmail(actor.getEmail());
      event.setActorRole(actor.getRole());
    }
    event.setNote(note);
    return event;
  }
}
