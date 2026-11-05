package com.example.ticketing.controller;

import com.example.ticketing.dto.UserDto;
import com.example.ticketing.service.CurrentUser;
import com.example.ticketing.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;
  private final CurrentUser currentUser;
  private final AuthenticationManager authenticationManager;
  private final SecurityContextRepository contexts;
  private final HttpSessionCsrfTokenRepository csrfTokens;

  @GetMapping("/csrf")
  public CsrfToken csrf(CsrfToken token) {
    return token;
  }

  @PostMapping("/signup")
  public String signup(@RequestBody UserDto dto, HttpServletRequest request) {
    return userService.signup(dto, request.getRemoteAddr());
  }

  @GetMapping("/check")
  public String check(@RequestParam String username) {
    return userService.exists(username) ? "duplicate" : "ok";
  }

  @PostMapping("/login")
  public Map<String, Object> login(
      @RequestBody UserDto dto, HttpServletRequest request, HttpServletResponse response) {
    var authentication =
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(
                dto.getUsername(), dto.getPassword()));
    if (request.getSession(false) != null) request.changeSessionId();
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    contexts.saveContext(context, request, response);
    csrfTokens.saveToken(null, request, response);
    return me();
  }

  @GetMapping("/me")
  public Map<String, Object> me() {
    var user = currentUser.require();
    return Map.of(
        "userId", user.getId(), "username", user.getUsername(), "role", user.getUserType().name());
  }
}
