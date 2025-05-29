package com.cctv.road.member.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

  @GetMapping("/login")
  public String loginPage(@RequestParam(value = "error", required = false) String error,
      @RequestParam(value = "registered", required = false) String registered,
      @RequestParam(value = "social_info", required = false) String socialInfo,
      Model model) {

    if (error != null) {
      model.addAttribute("message", "아이디 또는 비밀번호가 올바르지 않습니다.");
    }

    if (registered != null) {
      model.addAttribute("message", "회원가입이 완료되었습니다. 로그인 해주세요.");
    }

    if (socialInfo != null) {
      model.addAttribute("message", "소셜 로그인 정보가 부족합니다. 회원가입을 진행해주세요.");
    }

    return "login"; // → templates/login.html
  }
}
