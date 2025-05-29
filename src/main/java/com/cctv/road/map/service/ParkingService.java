package com.cctv.road.map.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import com.cctv.road.map.repository.ParkingRepository;
import com.cctv.road.map.entity.Parking;

@Service
@RequiredArgsConstructor
public class ParkingService {

  private final ParkingRepository parkingRepository;

  @Value("${seoul.city.parking.api-key}")
  private String seoulParkingApiKey;

  @Value("${naver.map.client-id}")
  private String naverClientId;

  @Value("${naver.map.client-secret}")
  private String naverClientSecret;

  public void initializeParkingData() {
    String url = "http://openapi.seoul.go.kr:8088/" + seoulParkingApiKey + "/json/GetParkingInfo/1/1000/";

    WebClient.create().get()
        .uri(url)
        .retrieve()
        .bodyToMono(JsonNode.class)
        .doOnError(err -> {
          System.err.println("❌ API 호출 에러: " + err.getMessage());
        })
        .map(json -> {
          JsonNode rows = json.path("GetParkingInfo").path("row");
          if (!rows.isArray()) {
            System.err.println("❌ 'row' 배열이 없음! 응답 구조가 예상과 다름");
          } else {
            System.out.println("✅ 응답된 주차장 수: " + rows.size());
          }
          return rows;
        })
        .flatMapMany(Flux::fromIterable)
        .flatMap(row -> {
          String address = row.path("ADDR").asText();
          String name = row.path("PKLT_NM").asText();

          System.out.println("📍 주차장 처리 중: " + name + " (" + address + ")");

          return geocode(address)
              .doOnError(e -> System.err.println("❌ 지오코딩 실패: " + address + " → " + e.getMessage()))
              .map(coord -> {
                Parking parking = parkingRepository.findByAddress(address).orElse(new Parking());

                parking.setName(name);
                parking.setAddress(address);
                parking.setTel(row.path("TELNO").asText());
                parking.setPayType(row.path("PAY_YN_NM").asText());
                parking.setParkingType(row.path("PRK_TYPE_NM").asText());
                parking.setCapacity(row.path("TPKCT").asInt());
                int current = row.path("NOW_PRK_VHCL_CNT").asInt();
                parking.setCurrentCount(current);
                parking.setAvailableCount(parking.getCapacity() - current);
                parking.setBaseCharge(row.path("BSC_PRK_CRG").asInt());
                parking.setBaseMinutes(row.path("BSC_PRK_HR").asInt());
                parking.setAddCharge(row.path("ADD_PRK_CRG").asInt());
                parking.setAddMinutes(row.path("ADD_PRK_HR").asInt());
                parking.setDayMaxCharge(row.path("DAY_MAX_CRG").asInt());
                parking.setWeekdayStart(row.path("WD_OPER_BGNG_TM").asText());
                parking.setWeekdayEnd(row.path("WD_OPER_END_TM").asText());
                parking.setHolidayStart(row.path("LHLDY_OPER_BGNG_TM").asText());
                parking.setHolidayEnd(row.path("LHLDY_OPER_END_TM").asText());
                parking.setLat(coord.get("lat"));
                parking.setLng(coord.get("lng"));
                parking.setSourceType("SEOUL");

                return parking;
              });
        })
        .doOnNext(p -> {
          parkingRepository.save(p);
          System.out.println("✅ 저장 완료: " + p.getName());
        })
        .doOnError(e -> System.err.println("❌ 전체 플로우 중 에러 발생: " + e.getMessage()))
        .subscribe();
  }

  @Scheduled(fixedRate = 60000) // ⏱️ 1분마다 실행
public void updateRealTimeParkingCount() {
    String url = "http://openapi.seoul.go.kr:8088/" + seoulParkingApiKey + "/json/GetParkingInfo/1/1000/";

    WebClient.create().get()
        .uri(url)
        .retrieve()
        .bodyToMono(JsonNode.class)
        .map(json -> json.path("GetParkingInfo").path("row"))
        .flatMapMany(Flux::fromIterable)
        .doOnNext(row -> {
            String name = row.path("PKLT_NM").asText();
            int current = row.path("NOW_PRK_VHCL_CNT").asInt();
            int capacity = row.path("TPKCT").asInt();

            parkingRepository.findByName(name).ifPresent(parking -> {
                parking.setCurrentCount(current);
                parking.setCapacity(capacity);
                parking.setAvailableCount(capacity - current);
                parkingRepository.save(parking);
                // System.out.println("✅ 실시간 갱신: " + name + " (" + current + "/" + capacity + ")");
            });
        })
        .doOnError(e -> System.err.println("❌ 실시간 갱신 실패: " + e.getMessage()))
        .subscribe();
}


  private Mono<Map<String, Double>> geocode(String query) {
    return WebClient.create("https://naveropenapi.apigw.ntruss.com")
        .get()
        .uri(uriBuilder -> uriBuilder
            .path("/map-geocode/v2/geocode")
            .queryParam("query", query)
            .build())
        .header("X-NCP-APIGW-API-KEY-ID", naverClientId)
        .header("X-NCP-APIGW-API-KEY", naverClientSecret)
        .retrieve()
        .bodyToMono(JsonNode.class)
        .flatMap(json -> {
          JsonNode addresses = json.path("addresses");
          if (!addresses.isArray() || addresses.isEmpty()) {
            System.err.println("❌ 지오코딩 실패: 응답에 주소 없음 → query = " + query);
            return Mono.error(new RuntimeException("지오코딩 실패: 주소 없음"));
          }

          JsonNode addr = addresses.get(0);
          Map<String, Double> coord = new HashMap<>();
          coord.put("lat", addr.path("y").asDouble());
          coord.put("lng", addr.path("x").asDouble());
          return Mono.just(coord);
        });
  }
}
