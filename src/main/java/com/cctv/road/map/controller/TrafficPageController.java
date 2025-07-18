package com.cctv.road.map.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TrafficPageController {

  @Value("${its.api.key}")
  private String itsApiKey;

  @Value("${naver.map.client-id}")
  private String naverMapClientId;

  @GetMapping("pages/map/traffic")
  public String trafficPage(Model model) {
    model.addAttribute("itsApiKey", itsApiKey);
    model.addAttribute("naverMapClientId", naverMapClientId); // ✅ 반드시 추가해야 함
    return "pages/map/traffic"; // => templates/pages/map/traffic.html
  }
}
