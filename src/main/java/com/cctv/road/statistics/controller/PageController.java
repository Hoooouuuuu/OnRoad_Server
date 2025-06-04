package com.cctv.road.statistics.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController { // 여러 페이지 보여줄 때 범용적으로 가장 적절

    @GetMapping("/news-view")
    public String newsWrapper() {
        return "pages/statistics/news-wrapper"; // templates/news-wrapper.html
    }
}
