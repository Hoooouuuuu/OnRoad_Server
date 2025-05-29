package com.cctv.road.weather.service;

import com.cctv.road.weather.util.StationMapLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AirQualityService {

    private final StationMapLoader stationMapLoader;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${airkorea.api.key}")
    private String airKoreaApiKey;

    public Map<String, String> getAirQuality(String regionName) {
        String stationName = stationMapLoader.getStation(regionName);
        if (stationName == null) {
            throw new IllegalArgumentException("❌ 측정소 정보 없음: " + regionName);
        }

        try {
            String encodedKey = URLEncoder.encode(airKoreaApiKey, StandardCharsets.UTF_8);
String encodedStation = URLEncoder.encode(stationName, StandardCharsets.UTF_8);

            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty")
                    .queryParam("serviceKey", encodedKey)
                    .queryParam("returnType", "json")
                    .queryParam("numOfRows", 1)
                    .queryParam("pageNo", 1)
                    .queryParam("stationName", encodedStation) // 얘만 인코딩
                    .queryParam("dataTerm", "DAILY")
                    .queryParam("ver", "1.3")
                    .build(true) // 👉 이걸 true로 하면 추가 인코딩 안 함
                    .toUri();

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            String bodyText = response.getBody();

            log.info("📍 측정소: {}", stationName);
            log.info("🌐 요청 URI: {}", uri);
            log.info("📩 원본 응답: {}", bodyText);

            Map<String, Object> root = mapper.readValue(bodyText, Map.class);
            Map<String, Object> responseMap = (Map<String, Object>) root.get("response");
            Map<String, Object> body = (Map<String, Object>) responseMap.get("body");
            List<Map<String, String>> items = (List<Map<String, String>>) body.get("items");

            if (items == null || items.isEmpty()) {
                throw new IllegalStateException("❌ 대기질 데이터가 없습니다 (측정소: " + stationName + ")");
            }

            Map<String, String> item = items.get(0);

            return Map.of(
                    "station", stationName,
                    "pm10Value", item.getOrDefault("pm10Value", "--"),
                    "pm25Value", item.getOrDefault("pm25Value", "--"),
                    "pm10Grade", item.getOrDefault("pm10Grade", "--"),
                    "pm25Grade", item.getOrDefault("pm25Grade", "--"),
                    "khaiValue", item.getOrDefault("khaiValue", "--"),
                    "khaiGrade", item.getOrDefault("khaiGrade", "--"));

        } catch (Exception e) {
            throw new RuntimeException("❌ 대기질 정보 파싱 실패: " + e.getMessage(), e);
        }
    }
}
