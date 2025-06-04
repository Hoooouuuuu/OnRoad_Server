package com.cctv.road.member.security;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.cctv.road.member.dto.MemberDTO;
import com.cctv.road.member.entity.Member;
import com.cctv.road.member.repository.MemberRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

  private final MemberRepository memberRepository;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {

    CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

    Optional<Member> optionalMember = memberRepository
        .findByOauthProviderAndOauthId(oAuth2User.getOauthProvider(), oAuth2User.getOauthId());

    if (optionalMember.isPresent()) {
      // ✅ 로그인 처리
      Member member = optionalMember.get();

      CustomUserDetails userDetails = new CustomUserDetails(member);
      Authentication auth = new CustomAuthenticationToken(userDetails, userDetails.getAuthorities());

      // Spring Security 인증 컨텍스트에 세팅
      SecurityContextHolder.getContext().setAuthentication(auth);

      // 세션에도 등록 (선택사항)
      request.getSession().setAttribute("loginUser", userDetails);

      response.sendRedirect("/"); // 마이페이지 등 원하는 곳으로 이동 가능
    } else {
      // ✅ 생일 처리 (옵션)
      LocalDate birthDate = null;
      String birthYear = oAuth2User.getBirthYear();
      String birthDay = oAuth2User.getBirthDay();

      if (birthYear != null && birthDay != null) {
        try {
          birthDate = LocalDate.parse(birthYear + "-" + birthDay, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception ignored) {
        }
      }

      // ✅ 세션에 등록하여 회원가입 유도
      MemberDTO dto = MemberDTO.builder()
          .userId(oAuth2User.getUsername())
          .name(oAuth2User.getName())
          .email(oAuth2User.getEmail())
          .phoneNumber(oAuth2User.getPhoneNumber())
          .birthDate(birthDate)
          .oauthProvider(oAuth2User.getOauthProvider())
          .oauthId(oAuth2User.getOauthId())
          .build();

      request.getSession().setAttribute("socialUser", dto);
      response.sendRedirect("/register/oauth2");
    }
  }
}
