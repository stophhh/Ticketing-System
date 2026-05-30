package com.example.ticketing.service;
import com.example.ticketing.entity.Performance;
import com.example.ticketing.repository.PerformanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final PerformanceRepository performanceRepository;

    public List<Performance> getAllPerformances() {
        return performanceRepository.findAll();
    }

    public Performance getPerformance(Long performanceId) {
        return performanceRepository.findById(performanceId)
                .orElseThrow(() -> new RuntimeException("공연 없음"));
    }
    public Performance createPerformance(Performance performance) {
        return performanceRepository.save(performance);
    }
    public void deletePerformance(Long performanceId) {
        performanceRepository.deleteById(performanceId);
    }
}