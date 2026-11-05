package com.example.ticketing.service;

import com.example.ticketing.entity.*;
import com.example.ticketing.repository.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class QueueService {
  private final QueueRepository queueRepository;
  private final PerformanceRepository performances;
  private final UserRepository users;
  private final CurrentUser currentUser;
  private final Clock clock;

  @Transactional
  public QueueEntry joinQueue(Long performanceId) {
    var actor = currentUser.require();
    // 한 사용자의 동시 입장 요청을 직렬화하여 중복 대기표를 방지합니다.
    users.findByIdForUpdate(actor.getId()).orElseThrow();
    if (performanceId == null || !performances.existsById(performanceId))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "공연을 찾을 수 없습니다.");
    var existing =
        queueRepository.findFirstByUserIdAndPerformanceIdAndStatusInOrderByIdDesc(
            actor.getId(), performanceId, List.of(QueueStatus.WAITING, QueueStatus.ALLOWED));
    if (existing.isPresent()) {
      var entry = existing.get();
      if (entry.getStatus() == QueueStatus.WAITING
          || (entry.getExpiresAt() != null && entry.getExpiresAt().isAfter(now()))) return entry;
      entry.setStatus(QueueStatus.EXPIRED);
    }
    var entry = new QueueEntry();
    entry.setUserId(actor.getId());
    entry.setPerformanceId(performanceId);
    entry.setStatus(QueueStatus.WAITING);
    entry.setJoinedAt(now());
    entry = queueRepository.saveAndFlush(entry);
    entry.setQueueNumber(Math.toIntExact(entry.getId()));
    return entry;
  }

  @Transactional(readOnly = true)
  public QueueEntry owned(Long id) {
    var entry =
        queueRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "대기열 정보가 없습니다."));
    if (!currentUser.require().getId().equals(entry.getUserId()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    return entry;
  }

  @Transactional(readOnly = true)
  public long checkQueue(Long id) {
    var entry = owned(id);
    return queueRepository.countByPerformanceIdAndStatusAndIdLessThan(
        entry.getPerformanceId(), QueueStatus.WAITING, id);
  }

  public void requireAdmission(Long performanceId, Long userId) {
    if (!queueRepository.existsByUserIdAndPerformanceIdAndStatusAndExpiresAtAfter(
        userId, performanceId, QueueStatus.ALLOWED, now()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "대기열에서 입장 허가를 받은 후 좌석을 선택하세요.");
  }

  public void enterQueue(Long id) {
    var entry = owned(id);
    requireAdmission(entry.getPerformanceId(), entry.getUserId());
  }

  @Transactional
  public void processQueue() {
    for (var expired : queueRepository.findExpired(now())) expired.setStatus(QueueStatus.EXPIRED);
    queueRepository
        .findFirstByStatusOrderByIdAsc(QueueStatus.WAITING)
        .ifPresent(
            entry -> {
              entry.setStatus(QueueStatus.ALLOWED);
              entry.setExpiresAt(now().plusMinutes(5));
            });
  }

  private LocalDateTime now() {
    return LocalDateTime.now(clock);
  }
}
