package com.example.ticketing.controller;

import com.example.ticketing.entity.Payment;
import com.example.ticketing.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/request")
    public Payment requestPayment(
            @RequestParam Long bookingId,
            @RequestParam int amount,
            @RequestParam String method
    ) {
        return paymentService.requestPayment(bookingId, amount, method);
    }

    @PostMapping("/{paymentId}/confirm")
    public String confirmPayment(@PathVariable Long paymentId) {
        return paymentService.confirmPayment(paymentId);
    }

    @PostMapping("/{paymentId}/cancel")
    public String cancelPayment(@PathVariable Long paymentId) {
        return paymentService.cancelPayment(paymentId);
    }
}