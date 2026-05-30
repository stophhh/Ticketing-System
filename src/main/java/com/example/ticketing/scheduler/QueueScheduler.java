package com.example.ticketing.scheduler;

import com.example.ticketing.entity.QueueEntry;
import com.example.ticketing.entity.QueueStatus;
import com.example.ticketing.repository.QueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QueueScheduler {

    private final QueueRepository queueRepository;

    @Scheduled(fixedRate = 5000)
    public void processQueue() {

        QueueEntry queueEntry =
                queueRepository
                        .findTopByStatusOrderByQueueNumberAsc(
                                QueueStatus.WAITING
                        );

        if (queueEntry != null) {

            queueEntry.setStatus(
                    QueueStatus.ALLOWED
            );

            queueRepository.save(queueEntry);

            System.out.println(
                    "입장 허용: "
                            + queueEntry.getQueueNumber()
            );
        }
    }
}