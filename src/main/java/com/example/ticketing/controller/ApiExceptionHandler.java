package com.example.ticketing.controller;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<?> status(ResponseStatusException ex) {
    return ResponseEntity.status(ex.getStatusCode())
        .body(Map.of("message", ex.getReason() == null ? "요청을 처리할 수 없습니다." : ex.getReason()));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<?> login(AuthenticationException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Map.of("message", "아이디와 비밀번호를 확인하세요."));
  }

  @ExceptionHandler({
    DataIntegrityViolationException.class,
    org.springframework.dao.ConcurrencyFailureException.class
  })
  public ResponseEntity<?> conflict(Exception ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("message", "이미 처리됐거나 연결된 데이터가 있는 요청입니다."));
  }

  @ExceptionHandler({
    IllegalArgumentException.class,
    org.springframework.http.converter.HttpMessageNotReadableException.class
  })
  public ResponseEntity<?> invalid(Exception ex) {
    return ResponseEntity.badRequest().body(Map.of("message", "입력값을 확인하세요."));
  }
}
