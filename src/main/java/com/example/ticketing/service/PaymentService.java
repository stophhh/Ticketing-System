package com.example.ticketing.service;
import com.example.ticketing.entity.Booking;
import com.example.ticketing.entity.BookingStatus;
import com.example.ticketing.entity.Payment;
import com.example.ticketing.entity.PaymentStatus;
import com.example.ticketing.repository.BookingRepository;
import com.example.ticketing.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    @Transactional
    public Payment requestPayment(Long bookingId, int amount, String method) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("예매 정보 없음"));
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setStatus(PaymentStatus.PENDING);
        return paymentRepository.save(payment);
    }
    @Transactional
    public String confirmPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("결제 정보 없음"));
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());
        Booking booking = payment.getBooking();
        booking.setStatus(BookingStatus.COMPLETED);
        paymentRepository.save(payment);
        return "결제 완료";
    }
    @Transactional
    public String cancelPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("결제 정보 없음"));
        payment.setStatus(PaymentStatus.CANCELED);
        Booking booking = payment.getBooking();
        booking.setStatus(BookingStatus.CANCELED);
        paymentRepository.save(payment);
        return "결제 취소";
    }
}