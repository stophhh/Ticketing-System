package com.example.ticketing.service;

import com.example.ticketing.entity.QueueEntry;
import com.example.ticketing.entity.QueueStatus;
import com.example.ticketing.repository.QueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class QueueService {

    private final QueueRepository queueRepository;

    public QueueEntry joinQueue() {
        QueueEntry queueEntry = new QueueEntry();

        queueEntry.setQueueNumber((int) (queueRepository.count() + 1));
        queueEntry.setStatus(QueueStatus.WAITING);
        queueEntry.setJoinedAt(LocalDateTime.now());

        return queueRepository.save(queueEntry);
    }

    public int checkQueue(Long queueId) {
        QueueEntry me = queueRepository.findById(queueId)
                .orElseThrow(() -> new RuntimeException("대기열 정보 없음"));

        Integer minWaitingNumber = queueRepository.findMinWaitingNumber();

        if (minWaitingNumber == null) {
            return 0;
        }

        return Math.max(0, me.getQueueNumber() - minWaitingNumber);
    }

    public void enterQueue(Long queueId) {
        QueueEntry queueEntry = queueRepository.findById(queueId)
                .orElseThrow(() -> new RuntimeException("대기열 정보 없음"));

        queueEntry.setStatus(QueueStatus.ALLOWED);
        queueEntry.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        queueRepository.save(queueEntry);
    }
}