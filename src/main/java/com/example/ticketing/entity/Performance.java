package com.example.ticketing.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Performance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long performanceId;

    private String title;

    private String place;

    private LocalDateTime performanceDate;

    private int price;

    private String description;

    private String posterUrl;

    private String category;
}