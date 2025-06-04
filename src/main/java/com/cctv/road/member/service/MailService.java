package com.cctv.road.member.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailService {

  private final JavaMailSender mailSender;

  public void sendIdByEmail(String toEmail, String userId) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(toEmail);
    message.setSubject("[RoadTraffic] 아이디 찾기 결과");
    message.setText("회원님의 아이디는 '" + userId + "' 입니다.");
    mailSender.send(message);
  }

  public void sendTemporaryPassword(String toEmail, String tempPassword) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(toEmail);
    message.setSubject("[RoadTraffic] 임시 비밀번호 발급 안내");
    message.setText("회원님의 임시 비밀번호는 '" + tempPassword + "' 입니다.\n\n로그인 후 반드시 비밀번호를 변경해주세요!");
    mailSender.send(message);
  }
}
