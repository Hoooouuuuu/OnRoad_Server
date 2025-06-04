package com.cctv.road.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        // 이전 페이지 URL
        String referer = request.getHeader("Referer");

        // 경고 메시지 설정
        redirectAttributes.addFlashAttribute("error", "파일 용량은 최대 5MB까지 업로드할 수 있습니다.");

        // 이전 페이지로 리다이렉트
        return "redirect:" + referer;
    }
}