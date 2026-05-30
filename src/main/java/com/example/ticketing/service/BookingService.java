package com.example.ticketing.service;

import com.example.ticketing.entity.*;
import com.example.ticketing.repository.BookingRepository;
import com.example.ticketing.repository.PaymentRepository;
import com.example.ticketing.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final SeatRepository seatRepository;

    public Booking createBooking() {
        Booking booking = new Booking();
        booking.setBookingTime(LocalDateTime.now());
        booking.setStatus(BookingStatus.PENDING);

        return bookingRepository.save(booking);
    }

    public Booking getBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("예매 정보를 찾을 수 없습니다."));
    }

    public void cancelBooking(Long bookingId) {
        Booking booking = getBooking(bookingId);
        booking.setStatus(BookingStatus.CANCELED);

        bookingRepository.save(booking);
    }

    @Transactional
    public String completeBooking(Long paymentId, Long seatId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("결제 정보 없음"));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new RuntimeException("결제가 완료되지 않았습니다.");
        }

        Seat seat = seatRepository.findByIdForUpdate(seatId)
                .orElseThrow(() -> new RuntimeException("좌석 없음"));

        if (seat.getStatus() != SeatStatus.HELD) {
            throw new RuntimeException("선점된 좌석이 아닙니다.");
        }

        Booking booking = new Booking();
        booking.setBookingTime(LocalDateTime.now());
        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);

        seat.setStatus(SeatStatus.RESERVED);

        return "예매 완료";
    }
}