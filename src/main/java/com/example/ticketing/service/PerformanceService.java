package com.example.ticketing.service;

import com.example.ticketing.entity.Performance;
import com.example.ticketing.repository.PerformanceRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PerformanceService {
  private final PerformanceRepository performances;

  public List<Performance> getAllPerformances() {
    return performances.findAll();
  }

  public Performance getPerformance(Long id) {
    return performances
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "공연을 찾을 수 없습니다."));
  }

  @Transactional
  public Performance createPerformance(Performance p) {
    if (p.getTitle() == null
        || p.getTitle().isBlank()
        || p.getTitle().length() > 120
        || p.getPlace() == null
        || p.getPlace().isBlank()
        || p.getPerformanceDate() == null
        || p.getPrice() < 0)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "공연명·장소·일시와 올바른 가격을 입력하세요.");
    if (p.getPosterUrl() != null
        && !p.getPosterUrl().isBlank()
        && !p.getPosterUrl().matches("^(https://|/images/).+"))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "포스터는 HTTPS 주소 또는 /images/ 경로를 사용하세요.");
    p.setPerformanceId(null);
    return performances.save(p);
  }

  @Transactional
  public void deletePerformance(Long id) {
    // 예매·좌석이 연결된 공연은 FK 제약을 유지하여 기록의 무결성을 보존합니다.
    var p =
        performances
            .findByIdForUpdate(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "공연 없음"));
    performances.delete(p);
    performances.flush();
  }
}
