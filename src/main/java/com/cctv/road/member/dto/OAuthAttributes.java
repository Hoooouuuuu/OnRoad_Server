package com.cctv.road.member.dto;

import java.util.Map;

import com.cctv.road.member.entity.Member;

import lombok.Builder;
import lombok.Getter;

@Getter
public class OAuthAttributes {
  private Map<String, Object> attributes;
  private String nameAttributeKey;
  private String name;
  private String email;
  private String oauthId;
  private String oauthProvider;
  private String userId; // ✅ 변경됨

  @Builder
  public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey,
      String name, String email, String oauthId, String oauthProvider, String userId) {
    this.attributes = attributes;
    this.nameAttributeKey = nameAttributeKey;
    this.name = name;
    this.email = email;
    this.oauthId = oauthId;
    this.oauthProvider = oauthProvider;
    this.userId = userId; // ✅ 변경
  }

  public static OAuthAttributes of(String registrationId, String userNameAttributeName,
      Map<String, Object> attributes) {

    System.out.println("OAuthAttributes (parsed): " + attributes);

    if ("naver".equalsIgnoreCase(registrationId)) {
      return ofNaver(userNameAttributeName, attributes);
    }

    if ("kakao".equalsIgnoreCase(registrationId)) {
      return ofKakao(userNameAttributeName, attributes);
    }

    return ofGoogle(userNameAttributeName, attributes); // 기본은 구글
  }

  private static OAuthAttributes ofNaver(String userNameAttributeKey, Map<String, Object> attributes) {
    Object responseObj = attributes.get("response");
    if (responseObj instanceof Map) {
      attributes = (Map<String, Object>) responseObj;
    }

    String name = (String) attributes.getOrDefault("name", "");
    String email = (String) attributes.getOrDefault("email", "");
    String oauthId = (String) attributes.getOrDefault("id", "");
    String userId = (!email.isBlank()) ? email : "naver_" + oauthId;

    return OAuthAttributes.builder()
        .name(name)
        .email(email)
        .oauthId(oauthId)
        .attributes(attributes)
        .nameAttributeKey(userNameAttributeKey)
        .oauthProvider("naver")
        .userId(userId)
        .build();
  }

  private static OAuthAttributes ofKakao(String userNameAttributeKey, Map<String, Object> attributes) {
    Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
    Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

    String name = profile != null ? (String) profile.getOrDefault("nickname", "") : "";
    String email = kakaoAccount != null ? (String) kakaoAccount.getOrDefault("email", "") : "";
    String oauthId = String.valueOf(attributes.get("id"));
    String userId = (!email.isBlank()) ? email : "kakao_" + oauthId;

    return OAuthAttributes.builder()
        .name(name)
        .email(email)
        .oauthId(oauthId)
        .attributes(attributes)
        .nameAttributeKey(userNameAttributeKey)
        .oauthProvider("kakao")
        .userId(userId)
        .build();
  }

  private static OAuthAttributes ofGoogle(String userNameAttributeKey, Map<String, Object> attributes) {
    String name = (String) attributes.getOrDefault("name", "");
    String email = (String) attributes.getOrDefault("email", "");
    String oauthId = (String) attributes.getOrDefault(userNameAttributeKey, "");
    String userId = (!email.isBlank()) ? email : "google_" + oauthId;

    return OAuthAttributes.builder()
        .name(name)
        .email(email)
        .oauthId(oauthId)
        .attributes(attributes)
        .nameAttributeKey(userNameAttributeKey)
        .oauthProvider("google")
        .userId(userId)
        .build();
  }

  public Member toEntity() {
    return Member.builder()
        .userId(userId) // ✅ loginId → userId
        .password(null) // 소셜 로그인은 비밀번호 없음
        .name(name)
        .email(email)
        .oauthId(oauthId)
        .oauthProvider(oauthProvider)
        .build();
  }
}
