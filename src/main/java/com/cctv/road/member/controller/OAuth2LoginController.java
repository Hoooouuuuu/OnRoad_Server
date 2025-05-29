package com.cctv.road.member.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cctv.road.member.dto.MemberDTO;
import com.cctv.road.member.security.CustomOAuth2User;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@RequestMapping("/oauth2")
@Slf4j
public class OAuth2LoginController {

  @GetMapping("/redirect")
  public String oauth2Redirect(@AuthenticationPrincipal CustomOAuth2User oAuth2User,
      HttpSession session) {

    if (oAuth2User == null) {
      log.warn("🔒 소셜 로그인 정보 없음: 인증 principal이 null입니다.");
      return "redirect:/login?error=social";
    }

    log.info("🔥 [OAuth2 Redirect] 사용자 정보 수신됨: {}", oAuth2User.getUsername());

    if (oAuth2User.getOauthProvider() != null) {
      // ✅ memberId 필드 제거됨 → 조건문 삭제
      // 소셜 로그인 성공 → 회원가입된 경우는 그냥 메인으로
      // 가입 안 된 경우는 세션에 정보 저장 후 가입 폼으로
      MemberDTO memberDTO = MemberDTO.builder()
          .userId(oAuth2User.getUsername()) // ✅ loginId → userId
          .name(oAuth2User.getName())
          .email(oAuth2User.getEmail())
          .oauthProvider(oAuth2User.getOauthProvider())
          .oauthId(oAuth2User.getOauthId())
          .naverBirthYear(oAuth2User.getBirthYear())
          .naverBirthDay(oAuth2User.getBirthDay())
          .phoneNumber(oAuth2User.getPhoneNumber())
          .build();

      session.setAttribute("socialUser", memberDTO);
      log.info("🧾 신규 소셜 사용자, 회원가입 진행 필요 - 세션 저장 완료");

      return "redirect:/register/oauth2"; // 👉 회원가입 페이지로 이동
    }

    log.warn("⚠️ 소셜 로그인인데 provider 정보가 없습니다.");
    return "redirect:/login?error=social";
  }

  // 🔽 추가: 회원가입 폼으로 이동하는 핸들러
  @GetMapping("/register")
  public String showSocialRegisterForm(HttpSession session, Model model) {
    MemberDTO memberDTO = (MemberDTO) session.getAttribute("socialUser");

    if (memberDTO == null) {
      log.warn("❌ 세션에서 소셜 사용자 정보가 없습니다.");
      return "redirect:/login?social_info_missing";
    }

    model.addAttribute("memberDTO", memberDTO);
    log.info("📄 소셜 회원가입 폼 렌더링 시작");
    return "register/oauth2";
  }
}
