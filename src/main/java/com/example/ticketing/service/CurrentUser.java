package com.example.ticketing.service;

import com.example.ticketing.entity.User;
import com.example.ticketing.entity.UserType;
import com.example.ticketing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CurrentUser {
  private final UserRepository users;

  public User require() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName()))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
    return users
        .findByUsername(auth.getName())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  }

  public void requireOwner(User owner) {
    var actor = require();
    if (!actor.getId().equals(owner.getId()) && actor.getUserType() != UserType.ADMIN)
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 예매만 사용할 수 있습니다.");
  }
}
