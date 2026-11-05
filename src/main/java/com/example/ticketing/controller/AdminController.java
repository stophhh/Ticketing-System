package com.example.ticketing.controller;

import com.example.ticketing.entity.Performance;
import com.example.ticketing.repository.BookingRepository;
import com.example.ticketing.service.PerformanceService;
import com.example.ticketing.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

  private final PerformanceService performanceService;
  private final SeatService seatService;
  private final BookingRepository bookingRepository;

  @PostMapping("/performances")
  public Performance createPerformance(@RequestBody Performance performance) {

    return performanceService.createPerformance(performance);
  }

  @DeleteMapping("/performances/{id}")
  public String deletePerformance(@PathVariable Long id) {

    performanceService.deletePerformance(id);

    return "공연 삭제 완료";
  }

  @PostMapping("/performances/{id}/seats")
  public String createSeats(
      @PathVariable Long id,
      @RequestParam(defaultValue = "20") int vipCount,
      @RequestParam(defaultValue = "30") int rCount,
      @RequestParam(defaultValue = "50") int sCount) {

    return seatService.createSeats(id, vipCount, rCount, sCount);
  }
}
