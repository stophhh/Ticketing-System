package com.example.ticketing.service;

import com.example.ticketing.entity.*;
import com.example.ticketing.repository.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SeatService {
  private final SeatRepository seats;
  private final PerformanceRepository performances;
  private final UserRepository users;
  private final CurrentUser currentUser;
  private final QueueService queue;
  private final Clock clock;
  private final jakarta.persistence.EntityManager entityManager;

  public List<Seat> getSeatsByPerformance(Long id) {
    return seats.findByPerformance_PerformanceId(id);
  }

  @Transactional
  public String holdSeats(Long performanceId, List<Long> ids) {
    if (performanceId == null
        || ids == null
        || ids.isEmpty()
        || ids.size() > 4
        || ids.stream().anyMatch(Objects::isNull)
        || new HashSet<>(ids).size() != ids.size())
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "서로 다른 좌석을 1~4개 선택하세요.");
    for (var id : ids.stream().sorted().toList()) {
      var seat =
          seats
              .findById(id)
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "좌석 없음"));
      if (!seat.getPerformance().getPerformanceId().equals(performanceId))
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "선택한 공연의 좌석이 아닙니다.");
    }
    for (var id : ids.stream().sorted().toList()) holdSeat(id);
    return "좌석 선택 성공";
  }

  @Transactional
  public String holdSeat(Long seatId) {
    var user = currentUser.require();
    users.findByIdForUpdate(user.getId()).orElseThrow();
    var seat =
        seats
            .findByIdForUpdate(seatId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "좌석 없음"));
    entityManager.refresh(seat, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    queue.requireAdmission(seat.getPerformance().getPerformanceId(), user.getId());
    var now = LocalDateTime.now(clock);
    if (seat.getStatus() == SeatStatus.HELD
        && seat.getHoldExpiresAt() != null
        && !seat.getHoldExpiresAt().isAfter(now)) clear(seat);
    if (seat.getStatus() == SeatStatus.HELD && user.getId().equals(seat.getHeldByUserId()))
      return "좌석 선택 성공";
    if (seat.getStatus() != SeatStatus.AVAILABLE)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 선택되거나 예매된 좌석입니다.");
    if (seats.countByHeldByUserIdAndPerformance_PerformanceIdAndStatusAndHoldExpiresAtAfter(
            user.getId(), seat.getPerformance().getPerformanceId(), SeatStatus.HELD, now)
        >= 4)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "한 공연에서 최대 4개 좌석까지 선점할 수 있습니다.");
    seat.setStatus(SeatStatus.HELD);
    seat.setHeldByUserId(user.getId());
    seat.setHoldExpiresAt(now.plusMinutes(5));
    return "좌석 선택 성공";
  }

  @Transactional
  public void releaseExpiredHeldSeats() {
    var now = LocalDateTime.now(clock);
    for (var old : seats.findByStatusAndHoldExpiresAtBefore(SeatStatus.HELD, now)) {
      var seat = seats.findByIdForUpdate(old.getId()).orElseThrow();
      entityManager.refresh(seat, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
      if (seat.getStatus() == SeatStatus.HELD
          && seat.getHoldExpiresAt() != null
          && !seat.getHoldExpiresAt().isAfter(now)) clear(seat);
    }
  }

  private void clear(Seat seat) {
    seat.setStatus(SeatStatus.AVAILABLE);
    seat.setHeldByUserId(null);
    seat.setHoldExpiresAt(null);
  }

  @Transactional
  public String createSeats(Long performanceId, int vip, int regular, int standard) {
    if (vip < 0
        || regular < 0
        || standard < 0
        || (long) vip + regular + standard < 1
        || (long) vip + regular + standard > 1000)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "좌석 수는 등급별 0 이상, 합계 1~1000개로 입력하세요.");
    var performance =
        performances
            .findByIdForUpdate(performanceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "공연 없음"));
    if (!seats.findByPerformance_PerformanceId(performanceId).isEmpty())
      throw new ResponseStatusException(HttpStatus.CONFLICT, "좌석이 이미 생성되어 있습니다.");
    create(performance, "VIP", vip, 200000);
    create(performance, "R", regular, 150000);
    create(performance, "S", standard, 100000);
    return "좌석 생성 완료";
  }

  private void create(Performance performance, String grade, int count, int price) {
    for (int i = 1; i <= count; i++) {
      var seat = new Seat();
      seat.setSeatNumber(grade + "-" + i);
      seat.setGrade(grade);
      seat.setPrice(price);
      seat.setStatus(SeatStatus.AVAILABLE);
      seat.setPerformance(performance);
      seats.save(seat);
    }
  }
}
