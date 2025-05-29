package com.cctv.road.member.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

  // ✅ 기본 홈 화면 렌더링
  @GetMapping("/")
  public String home(Model model) {
    model.addAttribute("darkMode", false); // 기본값 false
    return "home";
  }
}
