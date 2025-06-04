package com.cctv.road.member.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.cctv.road.member.service.MailService;
import com.cctv.road.member.service.MemberService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class FindAccountController {

  private final MemberService memberService;
  private final MailService mailService;

  // ✅ 아이디 찾기 POST
  @PostMapping("/find-id")
  @ResponseBody
  public String findIdAjax(@RequestParam String email) {
    // log.info("📨 요청받은 email: [{}]", email); // ✅ email 찍기

    String cleanedEmail = email.trim().toLowerCase();
    // log.info("📨 정리된 email: [{}]", cleanedEmail); // ✅ 정리된 email 찍기

    String userId = memberService.findUserIdByEmail(cleanedEmail);
    // log.info("📨 찾은 userId: [{}]", userId); // ✅ 찾은 userId 찍기

    if (userId != null && !userId.isBlank()) {
      mailService.sendIdByEmail(cleanedEmail, userId);
      return "success";
    } else {
      return "fail";
    }
  }

  // ✅ 비밀번호 찾기 POST
  @PostMapping("/find-password")
  public String findPassword(
      @RequestParam String userId,
      @RequestParam String email,
      @RequestParam String phoneNumber,
      Model model) {

    boolean result = memberService.resetPasswordAndSendEmail(userId, email, phoneNumber);

    if (result) {
      model.addAttribute("message", "임시 비밀번호를 이메일로 발송했습니다. 로그인 후 변경해주세요.");
      model.addAttribute("redirectAfterSuccess", true);
    } else {
      model.addAttribute("error", "아이디, 이메일, 전화번호 정보가 일치하지 않습니다.");
    }
    return "redirect:/login";
  }
}
