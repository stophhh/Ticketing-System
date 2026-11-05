package com.example.ticketing.repository;

import com.example.ticketing.entity.Booking;
import com.example.ticketing.entity.BookingStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface BookingRepository extends JpaRepository<Booking, Long> {
  List<Booking> findByUser_Id(Long userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select b from Booking b where b.bookingId = :id")
  Optional<Booking> findByIdForUpdate(Long id);

  List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime now);
}
