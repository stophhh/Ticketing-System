package com.example.ticketing.controller;

import com.example.ticketing.dto.UserDto;
import com.example.ticketing.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public String signup(@RequestBody UserDto dto, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return userService.signup(dto, ip);
    }

    @GetMapping("/check")
    public String checkUsername(@RequestParam String username) {
        if(userService.exists(username)) {
            return "duplicate";
        }
        return "ok";
    }

    @PostMapping("/login")
    public String login(@RequestBody UserDto dto) {
        return userService.login(dto);
    }

    @PostMapping("/logout")
    public String logout() {
        return userService.logout();
    }
}