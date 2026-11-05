package com.example.ticketing.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Seat {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String seatNumber;

  private String grade; // VIP, R, S
  private int price;

  @Enumerated(EnumType.STRING)
  private SeatStatus status;

  private LocalDateTime holdExpiresAt;

  @JsonIgnore private Long heldByUserId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "performance_id")
  @JsonIgnore
  private Performance performance;
}
