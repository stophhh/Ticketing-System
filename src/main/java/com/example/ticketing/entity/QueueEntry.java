package com.example.ticketing.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Entity
@Getter
@Setter
public class QueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int queueNumber;

    @Enumerated(EnumType.STRING)
    private QueueStatus status;

    private LocalDateTime joinedAt;

    private LocalDateTime expiresAt;

}