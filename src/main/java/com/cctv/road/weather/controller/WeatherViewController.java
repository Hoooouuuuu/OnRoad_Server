package com.cctv.road.weather.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WeatherViewController {

    @GetMapping("/pages/weather")
    public String showWeatherPage() {
        // System.out.println("🚀 정상 작동함!");
        return "pages/weather";
    }
}
