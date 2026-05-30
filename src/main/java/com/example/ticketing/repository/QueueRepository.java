package com.example.ticketing.repository;

import com.example.ticketing.entity.QueueEntry;
import com.example.ticketing.entity.QueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QueueRepository
        extends JpaRepository<QueueEntry, Long> {

    @Query("""
        SELECT MIN(q.queueNumber)
        FROM QueueEntry q
        WHERE q.status = 'WAITING'
    """)
    Integer findMinWaitingNumber();

    QueueEntry findTopByStatusOrderByQueueNumberAsc(
            QueueStatus status
    );
}