package com.example.ticketing.controller;
import com.example.ticketing.entity.Performance;
import com.example.ticketing.service.PerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/performances")
@RequiredArgsConstructor

public class PerformanceController {
    private final PerformanceService performanceService;
    @GetMapping
    public List<Performance> getAllPerformances() {
        return performanceService.getAllPerformances();
    }
    @GetMapping("/{id}")
    public Performance getPerformance(@PathVariable Long id) {
        return performanceService.getPerformance(id);
    }
}