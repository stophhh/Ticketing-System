package com.example.ticketing.service;

import com.example.ticketing.entity.Seat;
import com.example.ticketing.entity.SeatStatus;
import com.example.ticketing.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;

    @Transactional
    public String holdSeat(Long seatId, Long userId) {

        Seat seat = seatRepository.findByIdForUpdate(seatId)
                .orElseThrow(() -> new RuntimeException("좌석 없음"));

        if (seat.getStatus() == SeatStatus.RESERVED) {
            throw new RuntimeException("이미 예약된 좌석");
        }

        if (seat.getStatus() == SeatStatus.HELD) {
            throw new RuntimeException("이미 선택된 좌석");
        }

        seat.setStatus(SeatStatus.HELD);

        return "좌석 선택 성공";
    }
}