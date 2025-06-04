package com.cctv.road.weather.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
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
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AirQualityService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${airkorea.api.key}")
    private String airKoreaApiKey;

    private List<Map<String, String>> stationList = new ArrayList<>();

    @PostConstruct
    public void loadStations() {
        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://apis.data.go.kr/B552584/MsrstnInfoInqireSvc/getMsrstnList")
                    .queryParam("serviceKey", URLEncoder.encode(airKoreaApiKey, StandardCharsets.UTF_8))
                    .queryParam("returnType", "json")
                    .queryParam("numOfRows", 1000)
                    .queryParam("pageNo", 1)
                    .build(true).toUri();

            String response = restTemplate.getForObject(uri, String.class);
            Map<String, Object> root = mapper.readValue(response, Map.class);
            List<Map<String, String>> items = (List<Map<String, String>>) ((Map) ((Map) root.get("response"))
                    .get("body")).get("items");

            stationList = items;
            log.info("✅ 대기질 측정소 {}개 로드 완료", items.size());
        } catch (Exception e) {
            log.error("❌ 측정소 목록 로드 실패", e);
        }
    }

    public Map<String, String> getAirQualityByLocation(double lat, double lon) {
        try {
            if (stationList.isEmpty()) {
                throw new IllegalStateException("❌ 측정소 목록이 비어있습니다.");
            }

            List<Map<String, String>> nearbyStations = findNearestStations(lat, lon, 5);
            for (Map<String, String> station : nearbyStations) {
                String stationName = station.get("stationName");
                Map<String, String> result = getAirQualityByStation(stationName);
                if (!isAllValuesEmpty(result)) {
                    return result;
                }
            }

            return buildEmptyResult(nearbyStations.get(0).get("stationName"));

        } catch (Exception e) {
            log.error("❌ 대기질 정보 실패", e);
            return Map.of("error", "대기질 정보 실패", "message", e.getMessage());
        }
    }

    public Map<String, String> getAirQualityByStation(String stationName) {
        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty")
                    .queryParam("serviceKey", URLEncoder.encode(airKoreaApiKey, StandardCharsets.UTF_8))
                    .queryParam("returnType", "json")
                    .queryParam("stationName", URLEncoder.encode(stationName, StandardCharsets.UTF_8)) // 인코딩 추가
                    .queryParam("dataTerm", "DAILY")
                    .queryParam("ver", "1.3")
                    .queryParam("numOfRows", 1)
                    .build(true).toUri();

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            Map<String, Object> root = mapper.readValue(response.getBody(), Map.class);

            List<Map<String, String>> items = (List<Map<String, String>>)
                    ((Map) ((Map) root.get("response")).get("body")).get("items");

            if (items == null || items.isEmpty()) {
                return buildEmptyResult(stationName);
            }

            Map<String, String> item = items.get(0);
            return Map.of(
                    "station", stationName,
                    "pm10Value", item.getOrDefault("pm10Value", "--"),
                    "pm25Value", item.getOrDefault("pm25Value", "--"),
                    "pm10Grade", item.getOrDefault("pm10Grade", "--"),
                    "pm25Grade", item.getOrDefault("pm25Grade", "--"),
                    "khaiValue", item.getOrDefault("khaiValue", "--"),
                    "khaiGrade", item.getOrDefault("khaiGrade", "--")
            );

        } catch (Exception e) {
            log.error("❌ 대기질 정보 파싱 실패: {}", stationName, e);
            return buildEmptyResult(stationName);
        }
    }

    private Map<String, String> buildEmptyResult(String stationName) {
        return Map.of(
                "station", stationName,
                "pm10Value", "--", "pm25Value", "--",
                "pm10Grade", "--", "pm25Grade", "--",
                "khaiValue", "--", "khaiGrade", "--"
        );
    }

    private List<Map<String, String>> findNearestStations(double lat, double lon, int limit) {
        return stationList.stream()
                .filter(st -> st.get("dmX") != null && st.get("dmY") != null)
                .sorted(Comparator.comparingDouble(st -> {
                    double sLat = Double.parseDouble(st.get("dmX"));
                    double sLon = Double.parseDouble(st.get("dmY"));
                    return getDistance(lat, lon, sLat, sLon);
                }))
                .limit(limit)
                .toList();
    }

    private boolean isAllValuesEmpty(Map<String, String> result) {
        return result == null ||
                (result.get("pm10Value").equals("--") &&
                        result.get("pm25Value").equals("--") &&
                        result.get("khaiValue").equals("--"));
    }

    private double getDistance(double lat1, double lon1, double lat2, double lon2) {
        double dx = lat1 - lat2;
        double dy = lon1 - lon2;
        return dx * dx + dy * dy;
    }
}
