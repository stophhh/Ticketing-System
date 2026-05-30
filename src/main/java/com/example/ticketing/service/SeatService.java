package com.example.ticketing.service;
import com.example.ticketing.entity.Performance;
import com.example.ticketing.entity.Seat;
import com.example.ticketing.entity.SeatStatus;
import com.example.ticketing.repository.PerformanceRepository;
import com.example.ticketing.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
@Service

@RequiredArgsConstructor
public class SeatService {
    private final SeatRepository seatRepository;
    private final PerformanceRepository performanceRepository;
    public List<Seat> getSeatsByPerformance(Long performanceId) {
        return seatRepository.findByPerformance_PerformanceId(performanceId);
    }
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
        seat.setHoldExpiresAt(LocalDateTime.now().plusMinutes(5));
        return "좌석 선택 성공";
    }
    @Transactional
    public void releaseExpiredHeldSeats() {
        List<Seat> expiredSeats = seatRepository.findByStatusAndHoldExpiresAtBefore(
                SeatStatus.HELD,
                LocalDateTime.now()
        );
        for (Seat seat : expiredSeats) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setHoldExpiresAt(null);
        }
    }
    @Transactional
    public String createSeats(Long performanceId, int vipCount, int rCount, int sCount) {
        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new RuntimeException("공연 없음"));

        createGradeSeats(performance, "VIP", vipCount, 200000);
        createGradeSeats(performance, "R", rCount, 150000);
        createGradeSeats(performance, "S", sCount, 100000);
        return "좌석 생성 완료";
    }
    private void createGradeSeats(Performance performance, String grade, int count, int price) {
        for (int i = 1; i <= count; i++) {
            Seat seat = new Seat();
            seat.setSeatNumber(grade + "-" + i);
            seat.setGrade(grade);
            seat.setPrice(price);
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setPerformance(performance);
            seatRepository.save(seat);
        }
    }
}