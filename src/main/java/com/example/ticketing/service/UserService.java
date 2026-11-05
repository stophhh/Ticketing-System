package com.example.ticketing.service;

import com.example.ticketing.dto.UserDto;
import com.example.ticketing.entity.User;
import com.example.ticketing.entity.UserType;
import com.example.ticketing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public String signup(UserDto dto, String ip) {
    var username = dto.getUsername() == null ? "" : dto.getUsername().trim();
    var password = dto.getPassword();
    if (!username.matches("[A-Za-z0-9_]{3,30}"))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "아이디는 영문·숫자·밑줄 3~30자로 입력하세요.");
    if (password == null
        || password.length() < 8
        || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "비밀번호는 8자 이상, UTF-8 기준 72바이트 이하로 입력하세요.");
    if (userRepository.findByUsername(username).isPresent())
      throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 아이디입니다.");
    User user = new User();
    user.setUsername(username);
    user.setPassword(passwordEncoder.encode(password));
    user.setIp(ip);
    user.setUserType(UserType.MEMBER);
    userRepository.save(user);
    return "회원가입 성공";
  }

  public boolean exists(String username) {
    return userRepository.findByUsername(username).isPresent();
  }
}
