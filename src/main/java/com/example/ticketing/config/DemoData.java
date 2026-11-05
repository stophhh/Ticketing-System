package com.example.ticketing.config;

import com.example.ticketing.entity.*;
import com.example.ticketing.repository.*;
import com.example.ticketing.service.SeatService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** demo 프로필의 메모리 DB에만 만드는 가상 데이터. 운영 계정이 아닙니다. */
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoData implements CommandLineRunner {
  private final UserRepository users;
  private final PerformanceRepository performances;
  private final PasswordEncoder passwords;
  private final SeatService seats;

  @Override
  public void run(String... args) {
    addUser("demo_user", "demo-user-change-me", UserType.MEMBER);
    addUser("demo_admin", "demo-admin-change-me", UserType.ADMIN);
    var p = new Performance();
    p.setTitle("JH LIVE - 데모 공연");
    p.setPlace("데모 콘서트홀");
    p.setDescription("좌석 선택과 모의 결제를 체험하는 가상 공연입니다. 실제 예매나 결제가 발생하지 않습니다.");
    p.setCategory("콘서트");
    p.setPerformanceDate(LocalDateTime.of(2027, 6, 12, 18, 0));
    p.setPrice(100000);
    performances.save(p);
    seats.createSeats(p.getPerformanceId(), 8, 12, 12);
  }

  private void addUser(String name, String password, UserType type) {
    var u = new User();
    u.setUsername(name);
    u.setPassword(passwords.encode(password));
    u.setUserType(type);
    users.save(u);
  }
}
