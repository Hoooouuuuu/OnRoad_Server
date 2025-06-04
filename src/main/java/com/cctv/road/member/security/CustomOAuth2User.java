package com.cctv.road.member.security;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import lombok.Getter;

@Getter
public class CustomOAuth2User extends User implements OAuth2User {

  private final String name;
  private final String email;
  private final String oauthProvider;
  private final String oauthId;
  private final Map<String, Object> attributes;

  private final String birthYear;
  private final String birthDay;
  private final String phoneNumber;

  public CustomOAuth2User(
      String userId,
      String password,
      String name,
      String email,
      String oauthProvider,
      String oauthId,
      Collection<? extends GrantedAuthority> authorities,
      Map<String, Object> attributes,
      String birthYear,
      String birthDay,
      String phoneNumber) {

    super(
        userId != null ? userId : "temp_user",
        password != null ? password : "temp_password",
        authorities);

    this.name = name;
    this.email = email;
    this.oauthProvider = oauthProvider;
    this.oauthId = oauthId;
    this.attributes = attributes;
    this.birthYear = birthYear;
    this.birthDay = birthDay;
    this.phoneNumber = phoneNumber;
  }

  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * 생년월일을 LocalDate 형태로 반환
   * 예: 1992-08-20
   */
  public LocalDate getBirthDate() {
    if (birthYear != null && birthDay != null) {
      try {
        return LocalDate.parse(
            birthYear + "-" + birthDay,
            DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      } catch (Exception e) {
        return null;
      }
    }
    return null;
  }
}
