package com.cctv.road.weather.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cctv.road.weather.service.AirQualityService;
import com.cctv.road.weather.service.AstroService;
import com.cctv.road.weather.service.HolidayService;
import com.cctv.road.weather.service.KmaWeatherService;
import com.cctv.road.weather.service.WeatherAlertService;
import com.cctv.road.weather.service.WeatherNewsService;
import com.cctv.road.weather.util.GeoUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final KmaWeatherService kmaWeatherService;
    private final AirQualityService airQualityService;
    private final HolidayService holidayService;
    private final AstroService astroService;
    private final WeatherAlertService weatherAlertService;
    private final WeatherNewsService weatherNewsService;

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

    @GetMapping("/holidays")
    public ResponseEntity<?> getHolidays(@RequestParam(defaultValue = "2025") int year) {
        List<String> holidays = holidayService.getHolidayList(year);
        return ResponseEntity.ok(Map.of("year", year, "dates", holidays));
    }

    @GetMapping("/astro")
    public Map<String, String> getAstroInfo(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(required = false, defaultValue = "Y") String dnYn,
            @RequestParam(required = false) String date) {
        String locdate = (date != null) ? date : java.time.LocalDate.now().toString().replace("-", "");
        return astroService.getAstroInfo(lat, lon, locdate, dnYn);
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<Map<String, String>>> getWeatherAlerts() {
        return ResponseEntity.ok(weatherAlertService.getNationwideAlerts());
    }

    
    @GetMapping("/news")
    public ResponseEntity<List<Map<String, Object>>> getWeatherNews() {
        return ResponseEntity.ok(weatherNewsService.getRecentNews(10));
    }
}
