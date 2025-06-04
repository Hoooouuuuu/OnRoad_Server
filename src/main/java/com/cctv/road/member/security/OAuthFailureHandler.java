package com.cctv.road.member.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * ✅ OAuth2 로그인 실패 핸들러
 * - 신규 소셜 유저: 회원가입 페이지로 리다이렉트
 * - 일반 실패: 로그인 페이지로 리다이렉트
 */
@Component
public class OAuthFailureHandler implements AuthenticationFailureHandler {

  @Override
  public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException exception) throws IOException {

    if (exception instanceof OAuth2AuthenticationException oauthEx) {
      OAuth2Error error = oauthEx.getError();

      // ✅ 소셜 회원가입 예외 처리
      if ("social_signup".equals(error.getErrorCode())
          || request.getSession().getAttribute("socialUser") != null) {
        response.sendRedirect("/register/oauth2");
        return;
      }
    }

    // ❌ 기본 로그인 실패 처리
    response.sendRedirect("/login?error=oauth2");
  }
}
