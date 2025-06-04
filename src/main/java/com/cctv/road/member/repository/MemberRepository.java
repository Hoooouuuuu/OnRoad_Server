package com.cctv.road.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cctv.road.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, String> { // 🔥 PK 타입도 String으로 바꿔야 함!

  // 🔹 유저 ID 기반 조회 (소셜 로그인 및 마이페이지 조회용)
  Optional<Member> findByUserId(String userId);

  // 🔹 유저 ID 중복 여부 확인
  boolean existsByUserId(String userId);

  // 🔹 소셜 로그인 (OAuth2)용 - provider와 id로 조회
  Optional<Member> findByOauthProviderAndOauthId(String oauthProvider, String oauthId);

  // 🔹 소셜 로그인 ID 중복 여부 확인
  boolean existsByOauthProviderAndOauthId(String oauthProvider, String oauthId);

  // 🔹 닉네임 중복 확인
  boolean existsByNickName(String nickName);

  // 🔹 이메일로 회원 조회 (아이디 찾기용)
  Optional<Member> findByEmail(String email);

  // 🔹 아이디 + 이메일 + 전화번호로 회원 조회 (비밀번호 찾기용)
  Optional<Member> findByUserIdAndEmailAndPhoneNumber(String userId, String email, String phoneNumber);
}