package com.cctv.road.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import com.cctv.road.member.security.OAuthFailureHandler;
import com.cctv.road.member.security.OAuthSuccessHandler;
import com.cctv.road.member.service.CustomOAuth2UserService;
import com.cctv.road.member.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final @Lazy CustomOAuth2UserService customOAuth2UserService;
  private final CustomUserDetailsService customUserDetailsService;
  private final OAuthSuccessHandler oAuthSuccessHandler;
  private final OAuthFailureHandler oAuthFailureHandler;

  public SecurityConfig(
      @Lazy CustomOAuth2UserService customOAuth2UserService,
      CustomUserDetailsService customUserDetailsService,
      OAuthSuccessHandler oAuthSuccessHandler,
      OAuthFailureHandler oAuthFailureHandler) {
    this.customOAuth2UserService = customOAuth2UserService;
    this.customUserDetailsService = customUserDetailsService;
    this.oAuthSuccessHandler = oAuthSuccessHandler;
    this.oAuthFailureHandler = oAuthFailureHandler;
  }

  // 🔐 비밀번호 암호화 방식
  @Bean
  public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  // 🔐 사용자 인증 제공자 설정
  @Bean
  public DaoAuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(customUserDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
    return authConfig.getAuthenticationManager();
  }

  // ✅ 1번 체인: /api/proxy/** 는 인증 없이 허용 + CSRF 비활성화
  @Bean
  @Order(1)
  public SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
    http
        .securityMatcher("/api/proxy/**")
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    return http.build();
  }

  // ✅ 2번 체인: 나머지 요청은 인증 필요, CSRF 켜짐
  @Bean
  @Order(2)
  public SecurityFilterChain appChain(HttpSecurity http) throws Exception {
    http
        .securityMatcher(request -> !request.getRequestURI().startsWith("/api/proxy/"))
        .csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
        .headers(headers -> headers
            .frameOptions(frameOptions -> frameOptions.disable()) // ❗️iframe 허용
        )
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/", "/login", "/register/**",
                "/css/**", "/js/**", "/image/**", "/favicon.ico",
                "/json/**", "/pages/**", "/api/**", "/api/weather/**",
                "/member/find/**", "/find-id", "/find-password",
                "/board/list/**", "/board/view/**", "/naver-driving-path/**",
                "/chart-view", "/chart/**", "/pages/map/**", "/news-view/**"
                )
            .permitAll()
            .anyRequest().authenticated())
        .formLogin(form -> form
            .loginPage("/login")
            .defaultSuccessUrl("/", true)
            .permitAll())
        .oauth2Login(oauth2 -> oauth2
            .loginPage("/login")
            .userInfoEndpoint(endpoint -> endpoint.userService(customOAuth2UserService))
            .successHandler(oAuthSuccessHandler)
            .failureHandler(oAuthFailureHandler))
        .logout(logout -> logout
            .logoutSuccessUrl("/")
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID"));

    return http.build();
  }
}
