package com.example.ticketing.controller;

import com.example.ticketing.entity.Seat;
import com.example.ticketing.service.SeatService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/seats")
@RequiredArgsConstructor
public class SeatController {
  private final SeatService seatService;

  @PostMapping("/{seatId}/hold")
  public String holdSeat(@PathVariable Long seatId) {
    return seatService.holdSeat(seatId);
  }

  public record HoldRequest(Long performanceId, List<Long> seatIds) {}

  @PostMapping("/hold")
  public String holdSeats(@RequestBody HoldRequest request) {
    return seatService.holdSeats(request.performanceId(), request.seatIds());
  }

  @GetMapping("/performance/{performanceId}")
  public List<Seat> getSeatsByPerformance(@PathVariable Long performanceId) {

    return seatService.getSeatsByPerformance(performanceId);
  }
}
