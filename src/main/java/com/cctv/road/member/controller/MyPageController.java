package com.cctv.road.member.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cctv.road.member.dto.MemberDTO;
import com.cctv.road.member.security.CustomOAuth2User;
import com.cctv.road.member.security.CustomUserDetails;
import com.cctv.road.member.service.MemberService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/member")
@RequiredArgsConstructor
public class MyPageController {

  private final MemberService memberService;

  @GetMapping("/mypage")
  public String myPage(@AuthenticationPrincipal Object user, Model model) {
    String userId = null;

    if (user instanceof CustomOAuth2User oAuth2User) {
      userId = oAuth2User.getUsername();
    } else if (user instanceof com.cctv.road.member.security.CustomUserDetails userDetails) {
      userId = userDetails.getUsername();
    }

    if (userId == null) {
      return "redirect:/login?expired";
    }

    MemberDTO dto = memberService.getMemberInfo(userId);
    model.addAttribute("memberDTO", dto);
    return "member/mypage";
  }

  @GetMapping("/member/update")
  public String updateForm(@AuthenticationPrincipal Object user, Model model) {
    String userId = null;

    if (user instanceof CustomOAuth2User oAuth2User) {
      userId = oAuth2User.getUsername();
    } else if (user instanceof CustomUserDetails userDetails) {
      userId = userDetails.getUsername();
    }

    if (userId == null)
      return "redirect:/login";

    MemberDTO dto = memberService.getMemberInfo(userId);
    model.addAttribute("memberDTO", dto);
    return "member/update";
  }
}
