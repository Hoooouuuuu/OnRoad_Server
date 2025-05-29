package com.cctv.road.statistics.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChartPageController {

  @GetMapping("/chart-view")
  public String chartViewPage() {
    return "pages/statistics/chart"; // Thymeleaf 템플릿 경로
  }
}
