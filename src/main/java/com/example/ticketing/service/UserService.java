package com.example.ticketing.service;


import com.example.ticketing.dto.UserDto;
import com.example.ticketing.entity.User;
import com.example.ticketing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.ticketing.entity.UserType;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public String signup(UserDto dto, String ip) {
        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            throw new RuntimeException("아이디를 입력하세요.");
        }

        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            throw new RuntimeException("비밀번호를 입력하세요.");
        }

        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new RuntimeException("이미 존재하는 아이디입니다.");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setIp(ip);
        user.setUserType(UserType.MEMBER);

        userRepository.save(user);
        return "회원가입 성공";
    }

    public String login(UserDto dto) {
        User user = userRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new RuntimeException("회원이 없습니다."));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호가 틀렸습니다.");
        }

        return "로그인 성공";
    }
    public boolean exists(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    public String logout() {
        return "로그아웃 성공";
    }
}