package com.example.ticketing.repository;

import com.example.ticketing.entity.QueueEntry;
import com.example.ticketing.entity.QueueStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface QueueRepository extends JpaRepository<QueueEntry, Long> {
  Optional<QueueEntry> findFirstByUserIdAndPerformanceIdAndStatusInOrderByIdDesc(
      Long userId, Long performanceId, List<QueueStatus> statuses);

  boolean existsByUserIdAndPerformanceIdAndStatusAndExpiresAtAfter(
      Long userId, Long performanceId, QueueStatus status, LocalDateTime now);

  long countByPerformanceIdAndStatusAndIdLessThan(Long performanceId, QueueStatus status, Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<QueueEntry> findFirstByStatusOrderByIdAsc(QueueStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select q from QueueEntry q where q.status = com.example.ticketing.entity.QueueStatus.ALLOWED"
          + " and q.expiresAt < :now")
  List<QueueEntry> findExpired(LocalDateTime now);
}
