package com.example.ticketing.repository;
import com.example.ticketing.entity.Performance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerformanceRepository
        extends JpaRepository<Performance, Long> {
}