package com.cctv.road.weather.service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherAlertService {

    private final RestTemplate restTemplate;

    @Value("${kma.api.key}")
    private String kmaApiKey;

    public List<Map<String, String>> getNationwideAlerts() {
        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://apis.data.go.kr/1360000/WthrWrnInfoService/getWthrWrnList")
                    .queryParam("serviceKey", URLEncoder.encode(kmaApiKey, StandardCharsets.UTF_8))
                    .queryParam("pageNo", "1")
                    .queryParam("numOfRows", "1000")
                    .queryParam("dataType", "JSON")
                    .build(true)
                    .toUri();

            log.info("🌐 특보 리스트 요청 URI: {}", uri);

            ResponseEntity<Map> response = restTemplate.getForEntity(uri, Map.class);
            List<Map<String, Object>> itemList = extractItemList(response.getBody());

            if (itemList.isEmpty()) {
                log.info("⚠️ 현재 발효 중인 특보 없음");
                return List.of();
            }

            List<Map<String, String>> results = new ArrayList<>();

            for (Map<String, Object> item : itemList) {
                String stnId = (String) item.get("stnId");
                String tmSeq = String.valueOf(item.get("tmSeq"));

                Map<String, String> detail = getDetailFromMsg(stnId, tmSeq);
                if (detail != null) {
                    results.add(detail);
                }
            }

            return results;

        } catch (Exception e) {
            log.error("❌ 특보 조회 중 오류 발생", e);
            return List.of();
        }
    }

    private Map<String, String> getDetailFromMsg(String stnId, String tmSeq) {
        try {
            URI detailUri = UriComponentsBuilder
                    .fromHttpUrl("https://apis.data.go.kr/1360000/WthrWrnInfoService/getWthrWrnMsg")
                    .queryParam("serviceKey", URLEncoder.encode(kmaApiKey, StandardCharsets.UTF_8))
                    .queryParam("pageNo", "1")
                    .queryParam("numOfRows", "1")
                    .queryParam("dataType", "JSON")
                    .queryParam("stnId", stnId)
                    .queryParam("tmSeq", tmSeq)
                    .build(true)
                    .toUri();

            log.info("📨 상세 특보 요청: {}", detailUri);

            ResponseEntity<Map> response = restTemplate.getForEntity(detailUri, Map.class);
            List<Map<String, Object>> items = extractItemList(response.getBody());
            if (items.isEmpty())
                return null;

            Map<String, Object> item = items.get(0);

            String t1 = (String) item.get("t1"); // 제목
            String t2 = (String) item.get("t2"); // 지역 포함된 문장

            String region = "전국";
            if (t2 != null && t2.contains(":")) {
                String[] parts = t2.split(":");
                if (parts.length >= 2) {
                    region = parts[1].trim();
                }
            }

            Map<String, String> result = new HashMap<>();
            result.put("regionName", region);
            result.put("alertTitle", t1 != null ? t1 : "특보");
            result.put("alertLevel", ""); // 이 API에선 알 수 없음

            return result;

        } catch (Exception e) {
            log.warn("❌ 상세 특보 조회 실패: stnId={}, tmSeq={}, msg={}", stnId, tmSeq, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractItemList(Map responseBody) {
        try {
            if (responseBody == null)
                return List.of();

            Map<String, Object> response = (Map<String, Object>) responseBody.get("response");
            if (response == null)
                return List.of();

            Map<String, Object> body = (Map<String, Object>) response.get("body");
            if (body == null)
                return List.of();

            Map<String, Object> items = (Map<String, Object>) body.get("items");
            if (items == null || !items.containsKey("item"))
                return List.of();

            Object itemObj = items.get("item");
            if (itemObj instanceof List) {
                return (List<Map<String, Object>>) itemObj;
            } else if (itemObj instanceof Map) {
                return List.of((Map<String, Object>) itemObj);
            }

        } catch (Exception e) {
            log.warn("❌ 아이템 리스트 파싱 실패: {}", e.getMessage());
        }
        return List.of();
    }

}
