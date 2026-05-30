package com.example.ticketing.controller;
import com.example.ticketing.entity.Booking;
import com.example.ticketing.service.BookingService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;
    @PostMapping
    public Booking createBooking(@RequestBody BookingRequest request) {
        return bookingService.createBooking(
                request.getUserId(),
                request.getPerformanceId(),
                request.getSeatIds()
        );
    }
    @GetMapping("/{id}")
    public Booking getBooking(@PathVariable Long id) {
        return bookingService.getBooking(id);
    }
    @DeleteMapping("/{id}")
    public String cancelBooking(@PathVariable Long id) {
        bookingService.cancelBooking(id);
        return "예매 취소 완료";
    }
    @GetMapping("/user/{userId}")
    public List<Booking> getBookingsByUser(@PathVariable Long userId) {
        return bookingService.getBookingsByUser(userId);
    }
    @Getter
    @Setter
    public static class BookingRequest {
        private Long userId;
        private Long performanceId;
        private List<Long> seatIds;
    }
}