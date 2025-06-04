package com.cctv.road.member.security;

import java.util.Collection;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class CustomAuthenticationToken extends UsernamePasswordAuthenticationToken {
  public CustomAuthenticationToken(Object principal, Collection<? extends GrantedAuthority> authorities) {
    super(principal, null, authorities);
  }
}
