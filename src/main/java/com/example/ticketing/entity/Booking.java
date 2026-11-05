package com.example.ticketing.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Booking {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long bookingId;

  private LocalDateTime bookingTime;

  @Enumerated(EnumType.STRING)
  private BookingStatus status;

  private LocalDateTime expiresAt;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;

  @ManyToMany
  @JoinTable(
      name = "booking_seats",
      joinColumns = @JoinColumn(name = "booking_id"),
      inverseJoinColumns = @JoinColumn(name = "seat_id"))
  private List<Seat> seats = new ArrayList<>();

  @ManyToOne
  @JoinColumn(name = "performance_id")
  private Performance performance;
}
