package com.example.ticketing.controller;

import com.example.ticketing.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @PostMapping("/{seatId}/hold")
    public String holdSeat(
            @PathVariable Long seatId,
            @RequestParam Long userId
    ) {
        return seatService.holdSeat(seatId, userId);
    }
}