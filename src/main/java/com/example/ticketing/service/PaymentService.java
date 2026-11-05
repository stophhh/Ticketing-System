package com.example.ticketing.service;

import com.example.ticketing.entity.*;
import com.example.ticketing.repository.PaymentRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** 외부 PG 호출이나 실제 금전 거래가 없는 모의 결제입니다. */
@Service
@RequiredArgsConstructor
public class PaymentService {
  private final PaymentRepository payments;
  private final BookingService bookings;
  private final CurrentUser currentUser;
  private final Clock clock;

  @Transactional
  public Payment requestPayment(Long bookingId, int amount, String method) {
    var booking = bookings.locked(bookingId);
    currentUser.requireOwner(booking.getUser());
    if (booking.getStatus() == BookingStatus.CANCELED)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "취소된 예매입니다.");
    if (booking.getStatus() == BookingStatus.PENDING
        && (booking.getExpiresAt() == null
            || !booking.getExpiresAt().isAfter(LocalDateTime.now(clock))))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "결제 가능 시간이 만료되었습니다.");
    int expected = booking.getSeats().stream().mapToInt(Seat::getPrice).sum();
    if (amount != expected || expected <= 0)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 금액이 좌석 가격과 일치하지 않습니다.");
    if (method == null
        || !java.util.Set.of(
                "CARD", "BANK", "KAKAO", "card", "bank", "kakao", "카드", "계좌이체", "카카오페이")
            .contains(method))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 수단을 확인하세요.");
    var existing = payments.findByBooking_BookingId(bookingId);
    if (existing.isPresent()) return existing.get();
    var payment = new Payment();
    payment.setBooking(booking);
    payment.setAmount(expected);
    payment.setMethod(method);
    payment.setStatus(PaymentStatus.PENDING);
    return payments.save(payment);
  }

  @Transactional
  public String confirmPayment(Long id) {
    var payment =
        payments
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "결제 없음"));
    var booking = bookings.locked(payment.getBooking().getBookingId());
    currentUser.requireOwner(booking.getUser());
    // 예매 잠금 대기 전의 영속 컨텍스트에 있던 결제를 재조회하지 않아도 예매 상태로 중복 요청을 판별합니다.
    if (booking.getStatus() == BookingStatus.COMPLETED) return "모의 결제 완료";
    if (booking.getStatus() != BookingStatus.PENDING
        || payment.getStatus() != PaymentStatus.PENDING
        || booking.getExpiresAt() == null
        || !booking.getExpiresAt().isAfter(LocalDateTime.now(clock)))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "완료할 수 없는 결제입니다.");
    booking.setStatus(BookingStatus.COMPLETED);
    payment.setStatus(PaymentStatus.COMPLETED);
    payment.setPaidAt(LocalDateTime.now(clock));
    return "모의 결제 완료";
  }

  @Transactional
  public String cancelPayment(Long id) {
    var payment =
        payments
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "결제 없음"));
    bookings.cancelBooking(payment.getBooking().getBookingId());
    return "모의 결제 취소";
  }
}
