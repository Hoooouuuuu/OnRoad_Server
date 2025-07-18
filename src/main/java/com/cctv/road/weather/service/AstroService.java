package com.cctv.road.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AstroService {

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  @Value("${kma.api.key}")
  private String kmaApiKey;

  public Map<String, String> getAstroInfo(double lat, double lon, String date, String dnYn) {
    try {
      String encodedKey = URLEncoder.encode(kmaApiKey, StandardCharsets.UTF_8);
      String baseUrl = "http://apis.data.go.kr/B090041/openapi/service/RiseSetInfoService/getLCRiseSetInfo?ServiceKey=" + encodedKey;

      URI uri = UriComponentsBuilder
          .fromHttpUrl(baseUrl)
          .queryParam("locdate", date)
          .queryParam("latitude", (int) lat)
          .queryParam("longitude", (int) lon)
          .queryParam("dnYn", dnYn)
          .build(true)
          .toUri();

      // log.info("📡 최종 요청 URI: {}", uri);

      ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
      String json = response.getBody();

      // log.info("📨 천문 API 응답 본문: {}", json);

      return parseAstroJson(json);

    } catch (Exception e) {
      log.error("❌ 천문 API 호출 실패", e);
      return Map.of("error", "API 호출 실패", "message", e.getMessage());
    }
  }

  private Map<String, String> parseAstroJson(String json) {
    Map<String, String> result = new HashMap<>();
    try {
      JsonNode root = objectMapper.readTree(json);
      JsonNode item = root.path("response").path("body").path("items").path("item");

      if (item.isMissingNode() || !item.isObject()) {
        log.warn("⚠️ JSON 응답에 item이 없습니다.");
        return Map.of("warning", "no item");
      }

      item.fieldNames().forEachRemaining(field -> result.put(field, item.get(field).asText()));

    } catch (Exception e) {
      log.error("❌ JSON 파싱 실패", e);
      result.put("error", "JSON 파싱 실패");
      result.put("message", e.getMessage());
    }

    return result;
  }
}
