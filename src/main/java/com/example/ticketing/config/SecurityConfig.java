package com.example.ticketing.config;

import com.example.ticketing.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
public class SecurityConfig {
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  public SecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository();
  }

  @Bean
  public HttpSessionCsrfTokenRepository csrfTokenRepository() {
    return new HttpSessionCsrfTokenRepository();
  }

  @Bean
  public UserDetailsService userDetailsService(UserRepository users) {
    return username -> {
      var user =
          users
              .findByUsername(username)
              .orElseThrow(() -> new UsernameNotFoundException("로그인 정보를 확인하세요."));
      return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
          .password(user.getPassword())
          .roles(user.getUserType().name())
          .build();
    };
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
      throws Exception {
    return configuration.getAuthenticationManager();
  }

  @Bean
  public SecurityFilterChain filterChain(
      HttpSecurity http,
      SecurityContextRepository contexts,
      HttpSessionCsrfTokenRepository csrfTokens)
      throws Exception {
    http.securityContext(context -> context.securityContextRepository(contexts))
        .csrf(csrf -> csrf.csrfTokenRepository(csrfTokens))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/admin/**", "/admin.html")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/bookings")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        HttpMethod.GET,
                        "/",
                        "/*.html",
                        "/js/**",
                        "/css/**",
                        "/images/**",
                        "/favicon.ico",
                        "/performances/**",
                        "/seats/performance/**",
                        "/users/check",
                        "/users/csrf",
                        "/error")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/users/login", "/users/signup")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            errors ->
                errors
                    .authenticationEntryPoint(
                        (req, res, ex) ->
                            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "로그인이 필요합니다."))
                    .accessDeniedHandler(
                        (req, res, ex) ->
                            res.sendError(
                                HttpServletResponse.SC_FORBIDDEN, "접근 권한 또는 요청 토큰을 확인하세요.")))
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable())
        .logout(
            logout ->
                logout
                    .logoutUrl("/users/logout")
                    .logoutSuccessHandler(
                        (req, res, auth) -> {
                          res.setContentType("text/plain;charset=UTF-8");
                          res.getWriter().write("로그아웃 성공");
                        }));
    return http.build();
  }
}
