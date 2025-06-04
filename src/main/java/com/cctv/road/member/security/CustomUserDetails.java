package com.cctv.road.member.security;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.cctv.road.member.entity.Member;

import lombok.Getter;

@Getter
public class CustomUserDetails implements UserDetails {

  private final Member member;

  public CustomUserDetails(Member member) {
    this.member = member;
  }

  // ✅ ROLE 반환 (Spring Security 권한 체크용)
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    // Spring Security는 ROLE_ 접두사를 권한 이름에 요구함
    return Collections.singletonList(() -> member.getRole().name());
  }

  // ✅ 로그인 시 사용되는 아이디
  @Override
  public String getUsername() {
    return member.getUserId();
  }

  @Override
  public String getPassword() {
    return member.getPassword();
  }

  // ✅ 계정 상태 관련 기본 설정 (true로 고정)
  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  // ✅ 추가 정보 (템플릿 등에서 쉽게 접근 가능)
  public String getNickname() {
    return member.getNickName();
  }

  public String getEmail() {
    return member.getEmail();
  }

  public boolean isAdmin() {
    return member.getRole().name().equals("ROLE_ADMIN");
  }

  // ✅ Member 객체 전체 제공 (컨트롤러에서 활용 가능)
  public Member getMember() {
    return this.member;
  }
}
