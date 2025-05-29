package com.cctv.road.member.dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDTO {

  @NotBlank(message = "아이디는 필수 항목입니다.")
  private String userId;

  @NotBlank(message = "닉네임은 필수 항목입니다.")
  private String nickName;

  @Size(min = 8, max = 12, message = "비밀번호는 8 ~ 12자 사이여야 합니다.")
  private String password;

  private String confirmPassword;

  private String name;

  @DateTimeFormat(pattern = "yyyy-MM-dd")
  private LocalDate birthDate;

  @Pattern(regexp = "^$|^01([0|1|6|7|8|9])-?([0-9]{3,4})-?([0-9]{4})$", message = "유효한 전화번호 형식이 아닙니다.")
  private String phoneNumber;

  @Email(message = "유효한 이메일 주소를 입력하세요.")
  private String email;

  private String addressCity;
  private String addressDistrict;
  private String addressRoad;
  private String addressNumber;
  private String addressDetail;
  private String address;

  private String oauthProvider;
  private String oauthId;

  private String naverName;
  private String naverEmail;
  private String naverBirthYear;
  private String naverBirthDay;
  private String naverMobile;

  private String kakaoNickname;
  private String kakaoEmail;

  public void combineAddress() {
    StringBuilder sb = new StringBuilder();
    if (StringUtils.hasText(addressCity))
      sb.append(addressCity).append(" ");
    if (StringUtils.hasText(addressDistrict))
      sb.append(addressDistrict).append(" ");
    if (StringUtils.hasText(addressRoad))
      sb.append(addressRoad).append(" ");
    if (StringUtils.hasText(addressNumber))
      sb.append(addressNumber).append(" ");
    if (StringUtils.hasText(addressDetail))
      sb.append(addressDetail);
    this.address = sb.toString().trim();
  }

  public String getFullAddress() {
    return address != null ? address : "";
  }

  // 🔥 Fallback 제거: 기존 getter들 단순화
  // 소셜 fallback은 서비스 계층에서 판단

  public void setBirthDateFromStrings(String year, String monthDay) {
    if (StringUtils.hasText(year) && StringUtils.hasText(monthDay)) {
      try {
        this.birthDate = LocalDate.parse(year + "-" + monthDay, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      } catch (DateTimeParseException e) {
        this.birthDate = null;
      }
    }
  }

  // 🔧 필요한 경우를 위한 fallback 유틸
  public LocalDate getBirthDateFromNaver() {
    if (StringUtils.hasText(naverBirthYear) && StringUtils.hasText(naverBirthDay)) {
      try {
        return LocalDate.parse(naverBirthYear + "-" + naverBirthDay, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      } catch (DateTimeParseException e) {
        return null;
      }
    }
    return null;
  }
}
