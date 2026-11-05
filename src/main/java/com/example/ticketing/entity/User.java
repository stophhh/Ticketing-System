package com.example.ticketing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true)
  private String username;

  @com.fasterxml.jackson.annotation.JsonIgnore private String password;

  @com.fasterxml.jackson.annotation.JsonIgnore private String ip;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserType userType;
}
