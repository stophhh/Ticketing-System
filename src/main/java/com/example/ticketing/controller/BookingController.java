package com.example.ticketing.controller;

import com.example.ticketing.entity.Booking;
import com.example.ticketing.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/complete")
    public String completeBooking(
            @RequestParam Long paymentId,
            @RequestParam Long seatId
    ) {
        return bookingService.completeBooking(paymentId, seatId);
    }

    @PostMapping
    public Booking createBooking() {
        return bookingService.createBooking();
    }

    @GetMapping("/{bookingId}")
    public Booking getBooking(@PathVariable Long bookingId) {
        return bookingService.getBooking(bookingId);
    }

    @PostMapping("/{bookingId}/cancel")
    public void cancelBooking(@PathVariable Long bookingId) {
        bookingService.cancelBooking(bookingId);
    }
}