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
public class BookingService {
  private final BookingRepository bookings;
  private final SeatRepository seats;
  private final PerformanceRepository performances;
  private final PaymentRepository payments;
  private final CurrentUser currentUser;
  private final Clock clock;
  private final jakarta.persistence.EntityManager entityManager;
  private static final int MAX_TICKETS = 4;

  @Transactional
  public Booking createBooking(Long performanceId, List<Long> seatIds) {
    var actor = currentUser.require();
    if (performanceId == null
        || seatIds == null
        || seatIds.isEmpty()
        || seatIds.size() > MAX_TICKETS
        || seatIds.stream().anyMatch(Objects::isNull)
        || new HashSet<>(seatIds).size() != seatIds.size())
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "서로 다른 좌석을 1~4개 선택하세요.");
    var performance =
        performances
            .findById(performanceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "공연 없음"));
    var selected = new ArrayList<Seat>();

    // 항상 ID 오름차순으로 잠가 다중 좌석 요청의 교착 가능성을 줄입니다.
    for (var id : seatIds.stream().sorted().toList()) {
      var seat =
          seats
              .findByIdForUpdate(id)
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "좌석 없음"));
      selected.add(seat);
    }
    var now = LocalDateTime.now(clock);
    for (var seat : selected) {
      if (!seat.getPerformance().getPerformanceId().equals(performanceId))
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "선택한 공연의 좌석이 아닙니다.");
      if (seat.getStatus() != SeatStatus.HELD
          || !actor.getId().equals(seat.getHeldByUserId())
          || seat.getHoldExpiresAt() == null
          || !seat.getHoldExpiresAt().isAfter(now))
        throw new ResponseStatusException(HttpStatus.CONFLICT, "본인이 선점한 유효한 좌석만 예매할 수 있습니다.");
    }
    for (var seat : selected) {
      seat.setStatus(SeatStatus.RESERVED);
      seat.setHeldByUserId(null);
      seat.setHoldExpiresAt(null);
    }
    var booking = new Booking();
    booking.setUser(actor);
    booking.setPerformance(performance);
    booking.setSeats(selected);
    booking.setBookingTime(now);
    booking.setExpiresAt(now.plusMinutes(5));
    booking.setStatus(BookingStatus.PENDING);
    return bookings.save(booking);
  }

  @Transactional(readOnly = true)
  public Booking getBooking(Long id) {
    var booking =
        bookings
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "예매 없음"));
    currentUser.requireOwner(booking.getUser());
    return booking;
  }

  @Transactional(readOnly = true)
  public List<Booking> getMyBookings() {
    return bookings.findByUser_Id(currentUser.require().getId());
  }

  @Transactional(readOnly = true)
  public List<Booking> getBookingsByUser(Long id) {
    var actor = currentUser.require();
    if (!actor.getId().equals(id) && actor.getUserType() != UserType.ADMIN)
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    return bookings.findByUser_Id(id);
  }

  @Transactional
  public void cancelBooking(Long id) {
    var booking = locked(id);
    currentUser.requireOwner(booking.getUser());
    release(booking);
  }

  public Booking locked(Long id) {
    var booking =
        bookings
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "예매 없음"));
    entityManager.refresh(booking, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    return booking;
  }

  // 호출자는 예매 잠금을 먼저 확보해야 합니다. 좌석은 동일 순서로 잠급니다.
  public void release(Booking booking) {
    if (booking.getStatus() == BookingStatus.CANCELED) return;
    for (var id : booking.getSeats().stream().map(Seat::getId).sorted().toList()) {
      var seat = seats.findByIdForUpdate(id).orElseThrow();
      seat.setStatus(SeatStatus.AVAILABLE);
      seat.setHoldExpiresAt(null);
      seat.setHeldByUserId(null);
    }
    payments
        .findByBooking_BookingId(booking.getBookingId())
        .ifPresent(payment -> payment.setStatus(PaymentStatus.CANCELED));
    booking.setStatus(BookingStatus.CANCELED);
  }

  @Transactional
  public void expirePendingBookings() {
    for (var candidate :
        bookings.findByStatusAndExpiresAtBefore(BookingStatus.PENDING, LocalDateTime.now(clock))) {
      var booking = locked(candidate.getBookingId());
      if (booking.getStatus() == BookingStatus.PENDING
          && booking.getExpiresAt().isBefore(LocalDateTime.now(clock))) release(booking);
    }
  }

  @Transactional(readOnly = true)
  public List<Booking> getAllBookings() {
    if (currentUser.require().getUserType() != UserType.ADMIN)
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    return bookings.findAll();
  }
}
