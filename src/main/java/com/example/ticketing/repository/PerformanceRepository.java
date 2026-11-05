package com.example.ticketing.repository;

import com.example.ticketing.entity.Performance;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerformanceRepository extends JpaRepository<Performance, Long> {
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query(
      "select p from Performance p where p.performanceId = :id")
  Optional<Performance> findByIdForUpdate(
      @org.springframework.data.repository.query.Param("id") Long id);
}
