package com.cctv.road.weather.service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.cctv.road.weather.util.GeoUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class KmaWeatherService {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final RestTemplate restTemplate = new RestTemplate();

  @Value("${kma.api.key}")
  private String kmaApiKey;

  public String getUltraSrtNcst(double lat, double lon) {
    return callApi("getUltraSrtNcst", lat, lon);
  }

  public String getUltraSrtFcst(double lat, double lon) {
    return callApi("getUltraSrtFcst", lat, lon);
  }

  public String getVilageFcst(double lat, double lon) {
    return callApi("getVilageFcst", lat, lon);
  }

  public Map<String, Object> getUltraSrtNcstAsJson(double lat, double lon) {
    return parseJson(getUltraSrtNcst(lat, lon));
  }

  public Map<String, Object> getUltraSrtFcstAsJson(double lat, double lon) {
    return parseJson(getUltraSrtFcst(lat, lon));
  }

  public Map<String, Object> getVilageFcstAsJson(double lat, double lon) {
    return parseJson(getVilageFcst(lat, lon));
  }

  public Map<String, Object> getMidTaAsJson(String regId) {
    return parseJson(callMidApi("getMidTa", regId));
  }

  public Map<String, Object> getMidLandFcstAsJson(String regId) {
    return parseJson(callMidApi("getMidLandFcst", regId));
  }

  private Map<String, Object> parseJson(String json) {
    try {
        // log.info("✅ 받은 JSON: {}", json);

        // 빠르게 오류 응답 확인
        if (json.contains("\"resultCode\":\"03\"") || json.contains("NODATA_ERROR")) {
            log.warn("⚠️ 기상청 응답에 데이터 없음: {}", json);
            return Map.of("error", "데이터 없음", "raw", json);
        }

        return objectMapper.readValue(json, new TypeReference<>() {});
    } catch (Exception e) {
        log.error("❌ JSON 파싱 실패! 원본 응답:\n{}", json);
        throw new RuntimeException("기상청 JSON 파싱 실패", e);
    }
}

  private String callApi(String type, double lat, double lon) {
    try {
      GeoUtil.GridXY grid = GeoUtil.convertGRID(lat, lon);

      LocalDate baseDate = LocalDate.now();
      LocalTime now = LocalTime.now().minusMinutes(10); // 안전 버퍼

      String baseTimeStr;

      switch (type) {
        case "getUltraSrtNcst":
          // 초단기 실황: HH00
          if (now.getMinute() < 40) {
            now = now.minusHours(1);
            if (now.getHour() == 23 && LocalTime.now().getHour() == 0) {
              baseDate = baseDate.minusDays(1);
            }
          }
          baseTimeStr = now.truncatedTo(ChronoUnit.HOURS).format(DateTimeFormatter.ofPattern("HHmm"));
          break;

        case "getUltraSrtFcst":
          // 초단기 예보: HH30
          if (now.getMinute() < 45) {
            now = now.minusHours(1);
          }
          baseTimeStr = String.format("%02d30", now.getHour());
          if (now.getHour() == 23 && LocalTime.now().getHour() == 0) {
            baseDate = baseDate.minusDays(1);
          }
          break;

        case "getVilageFcst":
          // 단기 예보: 3시간 간격
          int hour = now.getHour();
          int[] validTimes = { 2, 5, 8, 11, 14, 17, 20, 23 };
          int latestValid = 2;

          for (int t : validTimes) {
            if (hour >= t)
              latestValid = t;
          }

          if (hour < 2)
            baseDate = baseDate.minusDays(1);

          baseTimeStr = String.format("%02d00", latestValid);
          break;

        default:
          throw new IllegalArgumentException("지원하지 않는 요청 타입: " + type);
      }

      String baseDateStr = baseDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

      // log.info("🛰️ 요청 타입: {}", type);
      // log.info("📍 위도: {}, 경도: {}", lat, lon);
      // log.info("🧭 변환된 Grid 좌표: nx = {}, ny = {}", grid.nx, grid.ny);
      // log.info("📅 기준 날짜: {}, 기준 시간: {}", baseDateStr, baseTimeStr);
      // log.info("🔑 사용 중인 서비스 키(raw): {}", kmaApiKey);

      String encodedKey = URLEncoder.encode(kmaApiKey, StandardCharsets.UTF_8);

      String rawUrl = UriComponentsBuilder
          .fromHttpUrl("https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/" + type)
          .queryParam("serviceKey", encodedKey) // ✅ 인코딩된 키 사용
          .queryParam("numOfRows", 500)
          .queryParam("pageNo", 1)
          .queryParam("dataType", "JSON")
          .queryParam("base_date", baseDateStr)
          .queryParam("base_time", baseTimeStr)
          .queryParam("nx", grid.nx)
          .queryParam("ny", grid.ny)
          .build(false) // 인코딩 안 하도록 false
          .toUriString();

      // log.info("🌐 최종 호출 URL (String): {}", rawUrl);

      URI uri = URI.create(rawUrl);
      ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);

      // log.info("✅ HTTP 상태코드: {}", response.getStatusCode());
      // log.info("📩 응답 데이터: {}", response.getBody());

      return response.getBody();

    } catch (Exception e) {
      log.error("❌ 기상청 API 호출 실패", e);
      return "{}";
    }
  }

  private String getMidForecastBaseTime() {
    LocalDate now = LocalDate.now();
    int hour = LocalTime.now().getHour();

    if (hour < 6) {
        // 아직 오전 예보도 없음 → 어제 18시 예보 사용
        now = now.minusDays(1);
        hour = 18;
    } else if (hour < 18) {
        hour = 6;
    } else {
        hour = 18;
    }

    return now.atTime(hour, 0)
              .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
}

  private String callMidApi(String type, String regId) {
  try {
    String tmFc = getMidForecastBaseTime();

    String encodedKey = URLEncoder.encode(kmaApiKey, StandardCharsets.UTF_8); // ✅ 인코딩 먼저!

    String url = UriComponentsBuilder.fromHttpUrl("https://apis.data.go.kr/1360000/MidFcstInfoService/" + type)
        .queryParam("serviceKey", encodedKey)
        .queryParam("pageNo", 1)
        .queryParam("numOfRows", 10)
        .queryParam("dataType", "JSON")
        .queryParam("regId", regId)
        .queryParam("tmFc", tmFc)
        .build(false) // ✅ 자동 인코딩 안 함
        .toUriString();

    // log.info("🌐 호출할 중기 API URL: {}", url);

    ResponseEntity<String> response = restTemplate.getForEntity(URI.create(url), String.class);

    // log.info("📨 중기 API 응답 ({}): {}", type, response.getBody());

    return response.getBody();

  } catch (Exception e) {
    log.error("❌ 중기예보 API 호출 실패", e);
    return "{}";
  }
}



}
