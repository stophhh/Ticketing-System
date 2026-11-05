package com.example.ticketing.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Payment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private int amount;

  private String method;

  @Enumerated(EnumType.STRING)
  private PaymentStatus status;

  private LocalDateTime paidAt;

  @OneToOne
  @JoinColumn(name = "booking_id", unique = true)
  private Booking booking;
}
