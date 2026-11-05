package com.example.ticketing.scheduler;

import com.example.ticketing.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "app.scheduling.enabled",
    havingValue = "true",
    matchIfMissing = true)
@Component
@RequiredArgsConstructor
public class SeatScheduler {

  private final SeatService seatService;
  private final com.example.ticketing.service.BookingService bookings;

  @Scheduled(fixedRate = 60000)
  public void releaseExpiredHeldSeats() {
    seatService.releaseExpiredHeldSeats();
    bookings.expirePendingBookings();
  }
}
