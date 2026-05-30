package com.example.ticketing.service;
import com.example.ticketing.entity.*;
import com.example.ticketing.repository.BookingRepository;
import com.example.ticketing.repository.PerformanceRepository;
import com.example.ticketing.repository.SeatRepository;
import com.example.ticketing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
@Service
@RequiredArgsConstructor
public class BookingService {
    private static final int MAX_TICKET_COUNT = 4;
    private final BookingRepository bookingRepository;

    private final UserRepository userRepository;
    private final PerformanceRepository performanceRepository;
    private final SeatRepository seatRepository;
    @Transactional
    public Booking createBooking(Long userId, Long performanceId, List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new RuntimeException("좌석을 선택해야 합니다.");
        }
        if (seatIds.size() > MAX_TICKET_COUNT) {
            throw new RuntimeException("최대 " + MAX_TICKET_COUNT + "매까지 예매할 수 있습니다.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));
        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new RuntimeException("공연 없음"));
        List<Seat> seats = seatRepository.findAllById(seatIds);
        if (seats.size() != seatIds.size()) {
            throw new RuntimeException("존재하지 않는 좌석이 포함되어 있습니다.");
        }
        for (Seat seat : seats) {
            if (seat.getStatus() != SeatStatus.HELD) {
                throw new RuntimeException("선점된 좌석만 예매할 수 있습니다.");
            }
            seat.setStatus(SeatStatus.RESERVED);
            seat.setHoldExpiresAt(null);
        }
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setPerformance(performance);
        booking.setSeats(seats);
        booking.setBookingTime(LocalDateTime.now());
        booking.setStatus(BookingStatus.PENDING);
        return bookingRepository.save(booking);
    }
    public Booking getBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)

                .orElseThrow(() -> new IllegalArgumentException("예매 정보를 찾을 수 없습니다."));
    }
    public List<Booking> getBookingsByUser(Long userId) {
        return bookingRepository.findByUser_Id(userId);
    }
    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking = getBooking(bookingId);
        booking.setStatus(BookingStatus.CANCELED);
        for (Seat seat : booking.getSeats()) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setHoldExpiresAt(null);
        }
        bookingRepository.save(booking);
    }
}
