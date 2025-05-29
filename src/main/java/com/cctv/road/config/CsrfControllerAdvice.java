package com.cctv.road.config;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class CsrfControllerAdvice {

  @ModelAttribute("_csrf")
  public CsrfToken csrfToken(HttpServletRequest request) {
    return (CsrfToken) request.getAttribute("_csrf");
  }
}
