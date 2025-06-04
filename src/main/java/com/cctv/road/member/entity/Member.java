package com.cctv.road.member.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class Member {

  @Id
  @Column(name = "user_id")
  private String userId;

  @Column(name = "nick_name", unique = true, nullable = false)
  private String nickName;

  @Column(name = "password")
  private String password;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "birth_date")
  private LocalDate birthDate;

  @Column(name = "phone_number")
  private String phoneNumber;

  @Column(name = "address")
  private String address;

  @Column(name = "provider")
  private String oauthProvider;

  @Column(name = "oauth_id")
  private String oauthId;

  @Column(name = "email")
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private Role role; // ✅ enum 필드

  @CreationTimestamp
  @Column(name = "create_at")
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "update_at")
  private LocalDateTime updatedAt;

  @Builder
  public Member(String userId, String nickName, String password, String name,
      LocalDate birthDate, String phoneNumber, String address,
      String oauthProvider, String oauthId, String email, Role role) {
    this.userId = userId;
    this.nickName = nickName;
    this.password = password;
    this.name = name;
    this.birthDate = birthDate;
    this.phoneNumber = phoneNumber;
    this.address = address;
    this.oauthProvider = oauthProvider;
    this.oauthId = oauthId;
    this.email = email;
    this.role = role; // ✅ 누락된 부분 추가
  }

  public String getUsername() {
    return userId;
  }

  public boolean isSocialUser() {
    return oauthProvider != null && oauthId != null;
  }
}
