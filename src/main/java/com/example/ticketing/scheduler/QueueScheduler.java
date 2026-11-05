package com.example.ticketing.scheduler;

import com.example.ticketing.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class QueueScheduler {
  private final QueueService queueService;

  @Scheduled(fixedDelay = 5000)
  public void processQueue() {
    queueService.processQueue();
  }
}
