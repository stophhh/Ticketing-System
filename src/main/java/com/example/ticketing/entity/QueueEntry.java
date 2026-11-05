package com.example.ticketing.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class QueueEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private int queueNumber;

  private Long userId;

  private Long performanceId;

  @Enumerated(EnumType.STRING)
  private QueueStatus status;

  private LocalDateTime joinedAt;

  private LocalDateTime expiresAt;
}
