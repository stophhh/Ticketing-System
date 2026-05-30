package com.example.ticketing.repository;
import com.example.ticketing.entity.Seat;
import com.example.ticketing.entity.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.id = :id")
    Optional<Seat> findByIdForUpdate(@Param("id") Long id);
    List<Seat> findByPerformance_PerformanceId(Long performanceId);
    List<Seat> findByStatusAndHoldExpiresAtBefore(
            SeatStatus status,
            LocalDateTime time
    );
}