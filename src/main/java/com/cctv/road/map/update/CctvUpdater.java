package com.cctv.road.map.update;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
public class CctvUpdater {

  @Value("${its.api.key}")
  private String itsApiKey;

  private final JdbcTemplate jdbcTemplate;
  private final RestTemplate restTemplate = new RestTemplate();

  public CctvUpdater(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    System.out.println("[CCTV] 생성자 호출됨, jdbcTemplate=" + (jdbcTemplate != null));
  }

  // 서버 시작 후 1초 뒤 1회 실행 (ex/its 각각 최신 날짜 따로 체크)
  @Scheduled(initialDelay = 1000, fixedDelay = Long.MAX_VALUE)
  public void initialUpdate() {
    try {
      System.out.println("[CCTV] initialUpdate 실행");
      LocalDate today = LocalDate.now();

      LocalDate lastFetchedEx = getLastFetchedDate("ex");
      LocalDate lastFetchedIts = getLastFetchedDate("its");
      boolean exDone = lastFetchedEx != null && lastFetchedEx.isEqual(today);
      boolean itsDone = lastFetchedIts != null && lastFetchedIts.isEqual(today);

      if (exDone && itsDone) {
        System.out.printf("[CCTV] 오늘(%s) 고속도로/국도 모두 이미 업데이트됨, 최초 실행 생략!%n", today);
        return;
      }
      if (!exDone) {
        System.out.println("[CCTV] 고속도로(ex) 업데이트 필요, 바로 실행");
        updateCctvUrlsByType("ex");
      } else {
        System.out.println("[CCTV] 고속도로(ex)는 이미 오늘자임");
      }
      if (!itsDone) {
        System.out.println("[CCTV] 국도(its) 업데이트 필요, 바로 실행");
        updateCctvUrlsByType("its");
      } else {
        System.out.println("[CCTV] 국도(its)는 이미 오늘자임");
      }
    } catch (Exception e) {
      System.out.println("[CCTV] initialUpdate 예외");
      e.printStackTrace();
    }
  }

  // 매일 00:01 자동 실행 (ex/its 각각 체크)
  @Scheduled(cron = "0 1 0 * * *")
  public void scheduledUpdate() {
    try {
      System.out.println("[CCTV] scheduledUpdate 실행");
      LocalDate today = LocalDate.now();

      LocalDate lastFetchedEx = getLastFetchedDate("ex");
      LocalDate lastFetchedIts = getLastFetchedDate("its");
      boolean exDone = lastFetchedEx != null && lastFetchedEx.isEqual(today);
      boolean itsDone = lastFetchedIts != null && lastFetchedIts.isEqual(today);

      if (exDone && itsDone) {
        System.out.printf("[CCTV] 오늘(%s) 고속도로/국도 모두 이미 업데이트됨, 스케줄 생략!%n", today);
        return;
      }
      if (!exDone) {
        System.out.println("[CCTV] 고속도로(ex) 업데이트 필요, 스케줄에서 실행");
        updateCctvUrlsByType("ex");
      } else {
        System.out.println("[CCTV] 고속도로(ex)는 이미 오늘자임 (스케줄)");
      }
      if (!itsDone) {
        System.out.println("[CCTV] 국도(its) 업데이트 필요, 스케줄에서 실행");
        updateCctvUrlsByType("its");
      } else {
        System.out.println("[CCTV] 국도(its)는 이미 오늘자임 (스케줄)");
      }
    } catch (Exception e) {
      System.out.println("[CCTV] scheduledUpdate 예외");
      e.printStackTrace();
    }
  }

  // 각 도로타입별 fetched_at 최신값 반환
  private LocalDate getLastFetchedDate(String roadType) {
    try {
      String sql = "SELECT MAX(DATE(fetched_at)) FROM cctvs_2025 WHERE road_type=?";
      return jdbcTemplate.query(con -> {
        var ps = con.prepareStatement(sql);
        ps.setString(1, roadType);
        return ps;
      }, (ResultSet rs) -> {
        if (rs.next()) {
          java.sql.Date d = rs.getDate(1);
          return d != null ? d.toLocalDate() : null;
        }
        return null;
      });
    } catch (Exception e) {
      System.out.println("[CCTV] getLastFetchedDate 예외 (" + roadType + "): " + e.getMessage());
      return null;
    }
  }

  private void updateCctvUrlsByType(String roadType) {
    System.out.println("[CCTV] updateCctvUrlsByType 진입: " + roadType);
    String url = "https://openapi.its.go.kr:9443/cctvInfo";
    String params = String.format(
        "?apiKey=%s&type=%s&cctvType=1&minX=124&maxX=132&minY=33&maxY=39&getType=json",
        itsApiKey, roadType);

    try {
      Map result = restTemplate.getForObject(url + params, Map.class);
      if (result == null || result.get("response") == null) {
        System.out.println("[CCTV] API 응답 또는 response 필드가 null임!");
        return;
      }

      List<Map> cctvList = (List<Map>) ((Map) result.get("response")).get("data");
      if (cctvList == null || cctvList.isEmpty()) {
        System.out.println("[CCTV] 받은 CCTV 데이터가 없음! roadType=" + roadType);
        return;
      }

      // ⚠️ null 좌표 제거
      List<Map> filteredList = cctvList.stream()
          .filter(row -> tryParseDouble(row.get("coordy"), row.get("lat")) != null
              && tryParseDouble(row.get("coordx"), row.get("lng")) != null)
          .toList();

      if (filteredList.isEmpty()) {
        System.out.println("[CCTV] 좌표가 유효한 CCTV 데이터가 없음");
        return;
      }

      String sql = "UPDATE cctvs_2025 SET video_url=?, fetched_at=NOW() " +
          "WHERE road_type=? AND ROUND(lat, 5)=ROUND(?, 5) AND ROUND(lng, 5)=ROUND(?, 5)";

      jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
        @Override
        public void setValues(java.sql.PreparedStatement ps, int i) throws SQLException {
          Map row = filteredList.get(i);
          String videoUrl = (String) row.get("cctvurl");
          Double lat = tryParseDouble(row.get("coordy"), row.get("lat"));
          Double lng = tryParseDouble(row.get("coordx"), row.get("lng"));
          ps.setString(1, videoUrl);
          ps.setString(2, roadType);
          ps.setDouble(3, lat);
          ps.setDouble(4, lng);
        }

        @Override
        public int getBatchSize() {
          return filteredList.size();
        }
      });

      System.out.printf("[%s][%s] CCTV 배치 업데이트 완료: %d건%n",
          java.time.LocalDateTime.now(), roadType, filteredList.size());

    } catch (Exception e) {
      System.out.println("[CCTV] updateCctvUrlsByType 예외 발생 (" + roadType + "): " + e.getMessage());
      e.printStackTrace();
    }
  }

  private Double tryParseDouble(Object... values) {
    for (Object v : values) {
      if (v != null) {
        try {
          return Double.parseDouble(v.toString());
        } catch (Exception ignore) {
        }
      }
    }
    return null;
  }
}
