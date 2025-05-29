package com.cctv.road.member.service;

import java.util.Optional;
import java.util.Random;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cctv.road.member.dto.MemberDTO;
import com.cctv.road.member.entity.Member;
import com.cctv.road.member.entity.Role; // ✅ 추가
import com.cctv.road.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

  private final MemberRepository memberRepository;
  private final PasswordEncoder passwordEncoder;
  private final MailService mailService;

  @Transactional(readOnly = true)
  public boolean isNickNameDuplicate(String nickName) {
    return memberRepository.existsByNickName(nickName);
  }

  @Transactional
  public Member registerSocialMember(MemberDTO dto) {
    String userId = dto.getUserId();

    if (memberRepository.existsByUserId(userId)) {
      throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
    }

    if (dto.getAddress() == null || dto.getAddress().isBlank()) {
      try {
        dto.combineAddress();
      } catch (Exception e) {
        throw new RuntimeException("주소 처리 중 오류가 발생했습니다.");
      }
    }

    String encodedPassword = passwordEncoder.encode(dto.getPassword());

    Role role = Role.ROLE_USER;

    Member member = Member.builder()
        .userId(userId)
        .nickName(dto.getNickName())
        .password(encodedPassword)
        .name(dto.getName() != null ? dto.getName() : dto.getNaverName())
        .email(dto.getEmail() != null ? dto.getEmail() : dto.getNaverEmail())
        .phoneNumber(dto.getPhoneNumber() != null ? dto.getPhoneNumber() : dto.getNaverMobile())
        .birthDate(dto.getBirthDate())
        .address(dto.getAddress())
        .oauthProvider(dto.getOauthProvider())
        .oauthId(dto.getOauthId())
        .role(role)
        .build();

    Member saved = memberRepository.save(member);

    return saved;
  }

  // ✅ 소셜 회원가입
  @Transactional
  public void registerOAuth2Member(MemberDTO dto) {
    registerSocialMember(dto);
  }

  // ✅ 아이디 중복 확인
  @Transactional(readOnly = true)
  public boolean isUserIdDuplicate(String userId) {
    return memberRepository.existsByUserId(userId);
  }

  // ✅ 마이페이지 조회용
  @Transactional(readOnly = true)
  public MemberDTO getMemberInfo(String userId) {
    Member member = memberRepository.findByUserId(userId)
        .orElseThrow(() -> new IllegalArgumentException("해당 회원을 찾을 수 없습니다."));

    return MemberDTO.builder()
        .userId(member.getUserId())
        .nickName(member.getNickName())
        .name(member.getName())
        .email(member.getEmail())
        .phoneNumber(member.getPhoneNumber())
        .birthDate(member.getBirthDate())
        .address(member.getAddress())
        .oauthProvider(member.getOauthProvider())
        .build();
  }

  // ✅ 회원 정보 수정 처리
  @Transactional
  public void updateMemberInfo(MemberDTO dto) {
    Member member = memberRepository.findByUserId(dto.getUserId())
        .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

    member.setName(dto.getName() != null ? dto.getName() : member.getName());
    member.setNickName(dto.getNickName() != null ? dto.getNickName() : member.getNickName());
    member.setEmail(dto.getEmail() != null ? dto.getEmail() : member.getEmail());
    member.setPhoneNumber(dto.getPhoneNumber() != null ? dto.getPhoneNumber() : member.getPhoneNumber());
    member.setBirthDate(dto.getBirthDate() != null ? dto.getBirthDate() : member.getBirthDate());
    member.setAddress(dto.getAddress() != null ? dto.getAddress() : member.getAddress());

    log.info("✏️ 회원 정보 수정됨: {}", member.getUserId());
  }

  // ✅ 회원 탈퇴
  @Transactional
  public void deleteMemberByUserId(String userId) {
    Member member = memberRepository.findByUserId(userId)
        .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

    memberRepository.delete(member);
    log.warn("🗑 회원 탈퇴됨: {}", userId);
  }

  // ✅ 이메일로 아이디 찾기
  @Transactional(readOnly = true)
  public String findUserIdByEmail(String email) {
      String cleanedEmail = email.trim().toLowerCase(); // ✅ 여기서도 정리
      return memberRepository.findByEmail(cleanedEmail)
          .map(Member::getUserId)
          .orElse(null);
  }  

  @Transactional(readOnly = true)
  public boolean checkPassword(String userId, String currentPassword) {
    Member member = memberRepository.findByUserId(userId)
        .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

    return passwordEncoder.matches(currentPassword, member.getPassword());
  }

  @Transactional
  public void updateMemberInfoWithNewPassword(MemberDTO dto, String newPassword) {
    Member member = memberRepository.findByUserId(dto.getUserId())
        .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

    member.setName(dto.getName() != null ? dto.getName() : member.getName());
    member.setNickName(dto.getNickName() != null ? dto.getNickName() : member.getNickName());
    member.setEmail(dto.getEmail() != null ? dto.getEmail() : member.getEmail());
    member.setPhoneNumber(dto.getPhoneNumber() != null ? dto.getPhoneNumber() : member.getPhoneNumber());
    member.setBirthDate(dto.getBirthDate() != null ? dto.getBirthDate() : member.getBirthDate());
    member.setAddress(dto.getAddress() != null ? dto.getAddress() : member.getAddress());

    // ✅ 새 비밀번호 암호화 후 저장
    member.setPassword(passwordEncoder.encode(newPassword));
  }

  @Transactional
  public boolean resetPasswordAndSendEmail(String userId, String email, String phoneNumber) {
    Optional<Member> optionalMember = memberRepository.findByUserIdAndEmailAndPhoneNumber(userId, email, phoneNumber);

    if (optionalMember.isPresent()) {
      Member member = optionalMember.get();

      String tempPassword = generateTemporaryPassword();
      mailService.sendTemporaryPassword(email, tempPassword);

      member.setPassword(passwordEncoder.encode(tempPassword));
      memberRepository.saveAndFlush(member);

      return true;
    } else {
      return false;
    }
  }

  private String generateTemporaryPassword() {
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+";
    Random random = new Random();

    int length = 8 + random.nextInt(5);
    StringBuilder tempPassword = new StringBuilder();

    for (int i = 0; i < length; i++) {
      tempPassword.append(chars.charAt(random.nextInt(chars.length())));
    }

    return tempPassword.toString();
  }
}