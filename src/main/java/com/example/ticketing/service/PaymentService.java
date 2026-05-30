package com.example.ticketing.service;

import com.example.ticketing.entity.Payment;
import com.example.ticketing.entity.PaymentStatus;
import com.example.ticketing.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public Payment requestPayment(Long bookingId, int amount, String method) {
        Payment payment = new Payment();
        payment.setBookingId(bookingId);
        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setStatus(PaymentStatus.PENDING);

        return paymentRepository.save(payment);
    }

    public String confirmPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("결제 정보 없음"));

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());

        paymentRepository.save(payment);

        return "결제 완료";
    }

    public String cancelPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("결제 정보 없음"));

        payment.setStatus(PaymentStatus.CANCELED);
        paymentRepository.save(payment);

        return "결제 취소";
    }
}