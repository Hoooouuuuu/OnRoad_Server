package com.cctv.road.member.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.cctv.road.member.dto.MemberDTO;
import com.cctv.road.member.security.CustomOAuth2User;
import com.cctv.road.member.security.CustomUserDetails;
import com.cctv.road.member.service.MemberService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@RequestMapping("/register")
@Slf4j
public class MemberController {

  private final MemberService memberService;

  @GetMapping("/oauth2")
  public String showOAuth2RegisterForm(HttpSession session, Model model, HttpServletRequest request) {
    MemberDTO socialUser = (MemberDTO) session.getAttribute("socialUser");

    if (socialUser == null) {
      return "redirect:/login?social_info";
    }

    if (socialUser.getBirthDate() == null &&
        socialUser.getNaverBirthYear() != null &&
        socialUser.getNaverBirthDay() != null) {
      try {
        String birth = socialUser.getNaverBirthYear() + "-" + socialUser.getNaverBirthDay();
        socialUser.setBirthDate(LocalDate.parse(birth, DateTimeFormatter.ofPattern("yyyy-MM-dd")));
      } catch (Exception e) {
        // 무시
      }
    }

    model.addAttribute("memberDTO", socialUser);

    // ✅ CSRF 토큰 수동 주입
    CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf");
    model.addAttribute("_csrf", csrfToken);

    return "register/oauth2";
  }

  @PostMapping("/oauth2/submit")
  public String submitOAuth2RegisterForm(@Valid @ModelAttribute("memberDTO") MemberDTO memberDTO,
      BindingResult bindingResult,
      HttpSession session) {
  
    log.debug("📥 제출된 DTO: {}", memberDTO); // 추가
    log.debug("❗ 유효성 오류: {}", bindingResult); // 추가
  
    validatePassword(memberDTO, bindingResult);
  
    if (memberService.isUserIdDuplicate(memberDTO.getUserId())) {
      bindingResult.rejectValue("userId", "duplicate", "이미 사용 중인 아이디입니다.");
    }
  
    if (memberService.isNickNameDuplicate(memberDTO.getNickName())) {
      bindingResult.rejectValue("nickName", "duplicate", "이미 사용 중인 닉네임입니다.");
    }
  
    if (bindingResult.hasErrors()) {
      log.warn("❗ 검증 실패. 다시 폼으로 리턴됨.");
      bindingResult.getAllErrors().forEach(e -> log.warn(" - {}", e.getDefaultMessage()));
      return "register/oauth2";
    }
  
    memberDTO.combineAddress();
  
    if (memberDTO.getBirthDate() == null &&
        memberDTO.getNaverBirthYear() != null &&
        memberDTO.getNaverBirthDay() != null) {
      try {
        String birth = memberDTO.getNaverBirthYear() + "-" + memberDTO.getNaverBirthDay();
        memberDTO.setBirthDate(LocalDate.parse(birth, DateTimeFormatter.ofPattern("yyyy-MM-dd")));
      } catch (Exception e) {
        bindingResult.rejectValue("birthDate", "invalid", "생년월일 형식이 잘못되었습니다.");
        return "register/oauth2";
      }
    }
  
    memberService.registerOAuth2Member(memberDTO);
    session.removeAttribute("socialUser");
  
    return "redirect:/login?registered";
  }  

  @GetMapping("/checkIdDuplicate")
  @ResponseBody
  public boolean checkIdDuplicate(@RequestParam String userId) {
    return memberService.isUserIdDuplicate(userId);
  }

  @GetMapping("/checkNickNameDuplicate")
  @ResponseBody
  public boolean checkNickNameDuplicate(@RequestParam String nickName) {
    return memberService.isNickNameDuplicate(nickName);
  }

  @GetMapping("/update")
  public String showUpdateForm(@AuthenticationPrincipal CustomUserDetails user, Model model) {
    if (user == null || user.getUsername() == null) {
      return "redirect:/login?expired";
    }

    MemberDTO memberDTO = memberService.getMemberInfo(user.getUsername());
    model.addAttribute("memberDTO", memberDTO);
    return "member/update";
  }

  @PostMapping("/update")
  public String processUpdateForm(
      @ModelAttribute("memberDTO") MemberDTO dto,
      @AuthenticationPrincipal CustomUserDetails user,
      Model model) {
  
    // 1. 로그인 유효성 검사
    if (user == null || user.getUsername() == null) {
      return "redirect:/login?expired";
    }
  
    // 2. 사용자 ID 및 주소 정리
    dto.setUserId(user.getUsername());
    dto.combineAddress();
  
    // 3. 현재 닉네임과 비교하여 중복 검사
    MemberDTO currentInfo = memberService.getMemberInfo(user.getUsername());
    if (!dto.getNickName().equals(currentInfo.getNickName()) &&
        memberService.isNickNameDuplicate(dto.getNickName())) {
      model.addAttribute("memberDTO", dto);
      model.addAttribute("nickNameError", "이미 사용 중인 닉네임입니다.");
      return "member/update";
    }
  
    // 4. 비밀번호가 있다면 비밀번호 포함 수정, 없다면 일반 정보만 수정
    if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
      memberService.updateMemberInfoWithNewPassword(dto, dto.getPassword());
    } else {
      memberService.updateMemberInfo(dto);
    }
  
    // 5. 마이페이지로 리디렉션
    return "redirect:/member/mypage";
  }
  

  @PostMapping("/delete")
  public String deleteMember(@AuthenticationPrincipal Object principal, HttpSession session) {
    String userId = null;
  
    if (principal instanceof CustomOAuth2User oAuth2User) {
      userId = oAuth2User.getUsername();
    } else if (principal instanceof CustomUserDetails userDetails) {
      userId = userDetails.getUsername();
    }
  
    log.info("🔥 deleteMember 컨트롤러 진입: {}", userId);
  
    // ✅ 삭제 먼저
    memberService.deleteMemberByUserId(userId);
  
    // ✅ 세션 나중에 끊기
    session.invalidate();
  
    // ✅ 모달 띄우기 위한 파라미터 포함해서 리디렉션
    return "redirect:/member/mypage?deleted=true";
  }  

  @PostMapping("/check-password")
  public ResponseEntity<Void> checkPassword(@RequestParam String currentPassword,
      @AuthenticationPrincipal CustomUserDetails user) {

    String userId = user.getUsername();
    boolean match = memberService.checkPassword(userId, currentPassword);

    if (match) {
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
  }

  private void validatePassword(MemberDTO memberDTO, BindingResult bindingResult) {
    String password = memberDTO.getPassword();
    String confirmPassword = memberDTO.getConfirmPassword();

    if (password != null && !password.equals(confirmPassword)) {
      bindingResult.rejectValue("confirmPassword", "mismatch", "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
    }

    if (password != null &&
        (!password.matches("^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\",.<>/?]).*$")
            || password.length() < 8 || password.length() > 12)) {
      bindingResult.rejectValue("password", "invalid", "비밀번호는 8 ~ 12자 사이의 영문, 숫자, 특수문자를 포함해야 합니다.");
    }
  }
}
