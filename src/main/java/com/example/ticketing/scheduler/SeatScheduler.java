package com.example.ticketing.scheduler;

import com.example.ticketing.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeatScheduler {

    private final SeatService seatService;

    @Scheduled(fixedRate = 60000)
    public void releaseExpiredHeldSeats() {
        seatService.releaseExpiredHeldSeats();
    }
}