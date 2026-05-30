package com.example.ticketing.controller;

import com.example.ticketing.entity.QueueEntry;
import com.example.ticketing.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    @PostMapping("/join")
    public QueueEntry joinQueue() {
        return queueService.joinQueue();
    }

    @GetMapping("/check")
    public int checkQueue(@RequestParam Long queueId) {
        return queueService.checkQueue(queueId);
    }

    @PostMapping("/enter")
    public String enterQueue(@RequestParam Long queueId) {
        queueService.enterQueue(queueId);
        return "입장 허용";
    }
}