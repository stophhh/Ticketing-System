package com.example.ticketing.controller;
import com.example.ticketing.entity.Payment;
import com.example.ticketing.service.PaymentService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    @PostMapping
    public Payment requestPayment(@RequestBody PaymentRequest request) {
        return paymentService.requestPayment(
                request.getBookingId(),
                request.getAmount(),
                request.getMethod()
        );
    }
    @PostMapping("/{paymentId}/confirm")
    public String confirmPayment(@PathVariable Long paymentId) {
        return paymentService.confirmPayment(paymentId);
    }
    @PostMapping("/{paymentId}/cancel")
    public String cancelPayment(@PathVariable Long paymentId) {
        return paymentService.cancelPayment(paymentId);
    }
    @Getter

    @Setter
    public static class PaymentRequest {
        private Long bookingId;
        private int amount;
        private String method;
    }
}