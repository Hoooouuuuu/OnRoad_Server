package com.cctv.road.weather.controller;

import com.cctv.road.weather.service.AirQualityService;
import com.cctv.road.weather.service.KmaWeatherService;
import com.cctv.road.weather.util.GeoUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final KmaWeatherService kmaWeatherService;
    private final AirQualityService airQualityService;

    @GetMapping("/current") // 현재 실시간 날씨
    public ResponseEntity<?> getCurrentWeather(@RequestParam double lat, @RequestParam double lon) {
        Map<String, Object> json = kmaWeatherService.getUltraSrtNcstAsJson(lat, lon);
        return ResponseEntity.ok(json);
    }

    @GetMapping("/forecast") // 1~6시간 예보
    public ResponseEntity<?> getHourlyForecast(@RequestParam double lat, @RequestParam double lon) {
        Map<String, Object> json = kmaWeatherService.getUltraSrtFcstAsJson(lat, lon);
        return ResponseEntity.ok(json);
    }

    @GetMapping("/daily") // 오늘~모레 예보
    public ResponseEntity<?> getDailyForecast(@RequestParam double lat, @RequestParam double lon) {
        Map<String, Object> json = kmaWeatherService.getVilageFcstAsJson(lat, lon);
        return ResponseEntity.ok(json);
    }

    @GetMapping("/full") // 전체 날씨 종합
    public ResponseEntity<?> getFullWeather(@RequestParam double lat, @RequestParam double lon) {
        try {
            Map<String, Object> current = kmaWeatherService.getUltraSrtNcstAsJson(lat, lon);
            Map<String, Object> forecast = kmaWeatherService.getUltraSrtFcstAsJson(lat, lon);
            Map<String, Object> daily = kmaWeatherService.getVilageFcstAsJson(lat, lon);

            GeoUtil.RegionCodes codes = GeoUtil.getRegionCodes(lat, lon);
            log.info("🧭 중기예보 지역코드: land={}, ta={}", codes.landRegId, codes.taRegId);

            Map<String, Object> middleTa = kmaWeatherService.getMidTaAsJson(codes.taRegId);
            Map<String, Object> middleLand = kmaWeatherService.getMidLandFcstAsJson(codes.landRegId);

            return ResponseEntity.ok(Map.of(
                    "current", current,
                    "forecast", forecast,
                    "daily", daily,
                    "middleTa", middleTa,
                    "middleLand", middleLand));
        } catch (Exception e) {
            log.error("❌ 날씨 정보 조회 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "날씨 조회 중 오류 발생",
                    "message", e.getMessage()));
        }
    }

    @GetMapping("/quality")
    public ResponseEntity<?> getAirQuality(@RequestParam Double lat, @RequestParam Double lon) {
        Map<String, String> airData = airQualityService.getAirQualityByLocation(lat, lon);
        return ResponseEntity.ok(airData);
    }

}
