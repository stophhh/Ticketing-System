package com.example.ticketing.controller;

import com.example.ticketing.entity.QueueEntry;
import com.example.ticketing.service.QueueService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/queue")
@RequiredArgsConstructor
public class QueueController {
  private final QueueService queueService;

  @PostMapping("/join")
  public QueueEntry join(@RequestParam Long performanceId) {
    return queueService.joinQueue(performanceId);
  }

  @GetMapping("/check")
  public Map<String, Object> check(@RequestParam Long queueId) {
    var entry = queueService.owned(queueId);
    return Map.of(
        "status",
        entry.getStatus().name(),
        "peopleAhead",
        queueService.checkQueue(queueId),
        "expiresAt",
        entry.getExpiresAt() == null ? "" : entry.getExpiresAt().toString());
  }

  @PostMapping("/enter")
  public String enter(@RequestParam Long queueId) {
    queueService.enterQueue(queueId);
    return "입장 허용";
  }
}
