package com.cctv.road.member.service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.cctv.road.member.dto.MemberDTO;
import com.cctv.road.member.entity.Member;
import com.cctv.road.member.repository.MemberRepository;
import com.cctv.road.member.security.CustomOAuth2User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

  private final MemberRepository memberRepository;

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    log.info("🔥 [loadUser] 진입");

    OAuth2User oauth2User = super.loadUser(userRequest);
    String registrationId = userRequest.getClientRegistration().getRegistrationId(); // naver, kakao, google
    log.info("✅ registrationId = {}", registrationId);

    // 공통 사용자 정보
    String name = null;
    String email = null;
    String oauthId = null;
    String birthYear = null;
    String birthDay = null;
    String mobile = null;

    Map<String, Object> attributes = oauth2User.getAttributes();

    // ✅ 제공자별 파싱
    if ("naver".equalsIgnoreCase(registrationId)) {
      Map<String, Object> response = (Map<String, Object>) attributes.get("response");
      name = (String) response.get("name");
      email = (String) response.get("email");
      oauthId = (String) response.get("id");
      birthYear = (String) response.get("birthyear");
      birthDay = (String) response.get("birthday");
      mobile = ((String) response.get("mobile")).replaceAll("-", "");

    } else if ("kakao".equalsIgnoreCase(registrationId)) {
      oauthId = String.valueOf(attributes.get("id"));
      Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");

      if (kakaoAccount != null) {
        email = (String) kakaoAccount.get("email");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        if (profile != null) {
          name = (String) profile.get("nickname");
        }
      }

    } else if ("google".equalsIgnoreCase(registrationId)) {
      name = (String) attributes.get("name");
      email = (String) attributes.get("email");
      oauthId = (String) attributes.get("sub");

      log.warn("📡 구글 사용자 정보 수신:");
      log.warn(" - name: {}", name);
      log.warn(" - email: {}", email);
      log.warn(" - oauthId(sub): {}", oauthId);

      if (name == null || email == null || oauthId == null) {
        log.error("❌ 구글 로그인 정보 중 필수 값이 null입니다. 회원가입 불가.");
        throw new OAuth2AuthenticationException(
            new OAuth2Error("invalid_google_user"),
            "구글 사용자 정보가 부족하여 회원가입할 수 없습니다.");
      }
    }

    // ✅ 기존 회원인 경우: 로그인 처리
    Optional<Member> optionalMember = memberRepository.findByOauthProviderAndOauthId(registrationId, oauthId);
    if (optionalMember.isPresent()) {
      Member member = optionalMember.get();
      log.info("✅ 기존 회원 로그인 처리: {}", member.getUserId());

      // ✅ 명시적으로 권한 지정 (ROLE_ 접두사 포함된 enum 값 사용)
      return new CustomOAuth2User(
          member.getUserId(),
          member.getPassword(),
          member.getName(),
          member.getEmail(),
          member.getOauthProvider(),
          member.getOauthId(),
          Collections.singletonList(new SimpleGrantedAuthority(member.getRole().name())), // ✅ 여기 중요!
          attributes,
          birthYear,
          birthDay,
          mobile);
    }

    // ✅ 신규 회원: 세션 저장 후 예외 던져 회원가입 페이지 유도
    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
        .getRequest();
    HttpSession session = request.getSession();

    MemberDTO dto = MemberDTO.builder()
        .name(name)
        .email(email)
        .phoneNumber(mobile)
        .birthDate(null)
        .oauthProvider(registrationId)
        .oauthId(oauthId)
        .naverBirthYear(birthYear)
        .naverBirthDay(birthDay)
        .build();

    session.setAttribute("socialUser", dto);
    log.info("🧾 소셜 사용자 세션 저장 완료 (provider: {})", registrationId);

    throw new OAuth2AuthenticationException(
        new OAuth2Error("social_signup"),
        "회원가입이 필요합니다.");
  }
}
