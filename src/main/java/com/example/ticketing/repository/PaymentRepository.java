package com.example.ticketing.repository;

import com.example.ticketing.entity.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
  Optional<Payment> findByBooking_BookingId(Long bookingId);
}
