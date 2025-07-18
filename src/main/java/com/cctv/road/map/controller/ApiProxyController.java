package com.cctv.road.map.controller;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.cctv.road.map.dto.BusArrivalDto;
import com.cctv.road.map.dto.BusRouteDto;
import com.cctv.road.map.dto.UnifiedBusStopDto;
import com.cctv.road.map.repository.BusStopRepository;
import com.cctv.road.weather.util.GeoUtil;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/proxy")
public class ApiProxyController {

  @Value("${naver.map.client-id}")
  private String clientId;

  @Value("${naver.map.client-secret}")
  private String clientSecret;

  @Value("${kakao.rest-api-key}")
  private String kakaoApiKey;

  private final BusStopRepository busStopRepository;

  private final Map<String, Map<String, String>> routeTimeCache = new ConcurrentHashMap<>();

  private final RestTemplate restTemplate = new RestTemplate();

  private WebClient naverClient;
  private WebClient seoulBusClient;
  private WebClient kakaoClient;
  private WebClient seoulOpenApiClient;
  private WebClient itsClient;
  private WebClient defaultClient;

  private final WebClient.Builder builder;

  private final Dotenv dotenv = Dotenv.configure().load();

  @Autowired
  public ApiProxyController(WebClient.Builder builder, BusStopRepository busStopRepository) {
    this.builder = builder;
    this.busStopRepository = busStopRepository;
  }

  @PostConstruct
  public void initNaverClient() {

    this.naverClient = WebClient.builder()
        .baseUrl("https://maps.apigw.ntruss.com")
        .defaultHeader("X-NCP-APIGW-API-KEY-ID", clientId)
        .defaultHeader("X-NCP-APIGW-API-KEY", clientSecret)
        .build();

    this.seoulBusClient = builder
        .baseUrl("http://ws.bus.go.kr")
        .build();

    this.kakaoClient = builder
        .baseUrl("https://dapi.kakao.com")
        .defaultHeader("Authorization", "KakaoAK " + System.getenv("KAKAO_REST_API_KEY"))
        .build();

    this.seoulOpenApiClient = builder
        .baseUrl("http://openapi.seoul.go.kr:8088")
        .build();

    this.itsClient = builder
        .baseUrl("https://openapi.its.go.kr:9443")
        .exchangeStrategies(ExchangeStrategies.builder()
            .codecs(config -> config.defaultCodecs().maxInMemorySize(3 * 1024 * 1024))
            .build())
        .build();

    this.defaultClient = builder.build();
  }

  @GetMapping("/naver-direction")
  public ResponseEntity<?> proxyDirectionApi(
      @RequestParam double startLat,
      @RequestParam double startLng,
      @RequestParam double goalLat,
      @RequestParam double goalLng) {
    String url = UriComponentsBuilder.fromHttpUrl("https://maps.apigw.ntruss.com/map-direction/v1/driving")
        .queryParam("start", startLng + "," + startLat)
        .queryParam("goal", goalLng + "," + goalLat)
        .queryParam("option", "trafast")
        .toUriString();

    HttpHeaders headers = new HttpHeaders();
    headers.set("X-NCP-APIGW-API-KEY-ID", clientId); // @Value로 주입된 client-id
    headers.set("X-NCP-APIGW-API-KEY", clientSecret); // client-secret

    HttpEntity<String> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
      return ResponseEntity.ok(response.getBody());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("API 호출 실패: " + e.getMessage());
    }
  }

  @GetMapping("/naver-geocode")
  public Mono<String> geocode(@RequestParam String query) {
    return naverClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/map-geocode/v2/geocode")
            .queryParam("query", query)
            .build())
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(String.class)
        .onErrorMap(e -> new RuntimeException("네이버 지오코딩 API 호출 실패", e));
  }

  @GetMapping("/naver-place")
  public Mono<String> searchPlace(@RequestParam String query) {
    // 2글자 이상 필터링 (너무 짧거나 초성만 들어오면 403 가능)
    if (query == null || query.trim().length() < 2) {
      return Mono.just("{\"error\":\"검색어는 2글자 이상이어야 합니다.\"}");
    }

    return naverClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/map-place/v1/search")
            .queryParam("query", query)
            .queryParam("coordinate", "127.1054328,37.3595953")
            .build())
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(String.class)
        .onErrorMap(e -> new RuntimeException("네이버 장소 검색 API 호출 실패", e));
  }

  @GetMapping("/kakao-place")
  public Mono<String> searchKakaoPlace(@RequestParam String query) {
    return kakaoClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/v2/local/search/keyword.json")
            .queryParam("query", query)
            .build())
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(String.class)
        .onErrorMap(e -> new RuntimeException("카카오 장소 검색 API 호출 실패", e));
  }

  @GetMapping("/busPosByNumber")
  public String getBusPositionsByNumber(@RequestParam String routeNumber) {
    // 1) DB에서 routeId 꺼내기
    String routeId = busStopRepository.findRouteIdByRouteNumber(routeNumber);
    if (routeId == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "routeId 없음");
    }
    // 2) 기존 로직 재사용
    return fetchBusPositionsFromSeoulApi(routeId);
  }

  @GetMapping("/busPos")
  public String getBusPositions(@RequestParam String routeId) {
    return fetchBusPositionsFromSeoulApi(routeId);
  }

  private String fetchBusPositionsFromSeoulApi(String routeId) {
    String key = dotenv.get("SEOUL_BUS_API_KEY");
    if (key == null || key.trim().isEmpty()) {
      throw new RuntimeException("API 키 누락");
    }

    String url = "http://ws.bus.go.kr/api/rest/buspos/getBusPosByRtid"
        + "?serviceKey=" + key.trim()
        + "&busRouteId=" + routeId
        + "&resultType=json";

    try {
      HttpResponse<String> resp = HttpClient.newHttpClient()
          .send(
              HttpRequest.newBuilder()
                  .uri(URI.create(url))
                  .header("Accept", "application/json")
                  .header("User-Agent", "Java-HttpClient")
                  .GET()
                  .build(),
              HttpResponse.BodyHandlers.ofString());

      if (resp.statusCode() != 200) {
        throw new RuntimeException("서울시 API 오류: " + resp.statusCode());
      }

      // 🔽 그대로 JSON 문자열 반환
      return resp.body();

    } catch (Exception e) {
      throw new RuntimeException("버스 위치 API 호출 실패: " + e.getMessage(), e);
    }
  }

  @GetMapping("/bus/routes")
  public ResponseEntity<?> getRoutesOrStops(
      @RequestParam(required = false) String stopId,
      @RequestParam(required = false) String routeNumber) {

    // 1. 정류장 ID로 경유 노선 조회 (도착 정보 창에서 사용)
    if (stopId != null) {
      List<BusRouteDto> routes = busStopRepository.findRoutesByStopId(stopId)
          .stream()
          .map(view -> new BusRouteDto(view.getRouteId(), view.getRouteName()))
          .toList();

      return ResponseEntity.ok(routes);
    }

    // 2. 노선 번호로 정류장 목록 조회 (노선 상세 패널에서 사용)
    if (routeNumber != null) {
      List<UnifiedBusStopDto> stops = busStopRepository.findByRouteNameOrderByStationOrderAsc(routeNumber)
          .stream()
          .map(stop -> new UnifiedBusStopDto(
              stop.getNodeId(),
              stop.getStationName(),
              stop.getArsId(),
              stop.getLatitude(),
              stop.getLongitude(),
              stop.getRouteId(),
              stop.getRouteName(),
              stop.getStationOrder()))
          .toList();

      return ResponseEntity.ok(stops);
    }

    return ResponseEntity.badRequest().body("stopId 또는 routeNumber 중 하나는 필수입니다.");
  }

  @GetMapping("/bus/stops")
  public ResponseEntity<List<UnifiedBusStopDto>> getBusStopsByRegion(@RequestParam String region) {
    if (!"서울특별시".equals(region)) {
      return ResponseEntity.ok(List.of());
    }

    List<UnifiedBusStopDto> stops = busStopRepository.findAll().stream()
        .limit(1000)
        .map(stop -> new UnifiedBusStopDto(
            stop.getNodeId(),
            stop.getStationName(),
            stop.getArsId(),
            stop.getLatitude(),
            stop.getLongitude(),
            null, // 노선 ID 없음
            null, // 노선 번호 없음
            null // 정류소 순서 없음
        ))
        .collect(Collectors.toList());

    return ResponseEntity.ok(stops);
  }

  @GetMapping("/bus/stops/nearby")
  public ResponseEntity<List<UnifiedBusStopDto>> getNearbyStops(
      @RequestParam double lat,
      @RequestParam double lng,
      @RequestParam(defaultValue = "500") double radius // 단위: 미터
  ) {
    // 서울 정류소만 대상
    List<UnifiedBusStopDto> nearbyStops = busStopRepository.findAll().stream()
        .filter(stop -> stop.getLatitude() != null && stop.getLongitude() != null)
        .filter(stop -> {
          double distance = calculateDistance(lat, lng, stop.getLatitude(), stop.getLongitude());
          return distance <= radius;
        })
        .limit(1000)
        .map(stop -> new UnifiedBusStopDto(
            stop.getNodeId(),
            stop.getStationName(),
            stop.getArsId(),
            stop.getLatitude(),
            stop.getLongitude(),
            null, null, null))
        .collect(Collectors.toList());

    return ResponseEntity.ok(nearbyStops);
  }

  private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
    final int R = 6371000; // 지구 반지름 (단위: 미터)
    double dLat = Math.toRadians(lat2 - lat1);
    double dLng = Math.toRadians(lng2 - lng1);

    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLng / 2) * Math.sin(dLng / 2);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  @GetMapping("/bus/regions")
  public ResponseEntity<List<String>> getAvailableBusRegions() {
    List<String> regions = List.of(
        "서울특별시", "부산광역시", "대구광역시", "인천광역시", "광주광역시", "대전광역시", "울산광역시",
        "세종특별자치시", "경기도", "강원특별자치도", "충청북도", "충청남도", "전라북도",
        "전라남도", "경상북도", "경상남도", "제주특별자치도");
    return ResponseEntity.ok(regions);
  }

  @GetMapping("/bus/arrivals")
  public ResponseEntity<List<BusArrivalDto>> getArrivals(
      @RequestParam String stopId,
      @RequestParam String arsId) {

    String encodedKey = dotenv.get("SEOUL_BUS_API_KEY").trim();

    String url = String.format(
        "http://ws.bus.go.kr/api/rest/arrive/getLowArrInfoByStId?serviceKey=%s&stId=%s&arsId=%s",
        encodedKey, stopId, arsId);

    try {
      HttpResponse<String> resp = HttpClient.newHttpClient()
          .send(HttpRequest.newBuilder()
              .uri(URI.create(url))
              .header("Accept", "application/xml")
              .GET()
              .build(),
              HttpResponse.BodyHandlers.ofString());

      String body = resp.body();
      if (!body.trim().startsWith("<?xml")) {
        throw new RuntimeException("응답이 XML이 아닙니다:\n" + body);
      }

      DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document doc = builder.parse(new InputSource(new StringReader(body)));

      NodeList itemList = doc.getElementsByTagName("itemList");
      List<BusArrivalDto> results = new ArrayList<>();

      for (int i = 0; i < itemList.getLength(); i++) {
        Element item = (Element) itemList.item(i);

        String routeNumber = getTagValue("rtNm", item);
        String routeTypeCode = getTagValue("routeType", item);
        String routeType = switch (routeTypeCode) {
          case "1" -> "공항";
          case "2" -> "마을";
          case "3" -> "간선";
          case "4" -> "지선";
          case "5" -> "순환";
          case "6" -> "광역";
          case "7" -> "인천";
          case "8" -> "경기";
          case "9" -> "폐지";
          case "10" -> "공용";
          case "11" -> "청주";
          case "12" -> "세종";
          case "13" -> "기타";
          default -> "기타";
        };

        // 🕒 운행 시간 확인
        boolean addedAsEnded = false;
        boolean isNBus = routeNumber != null && routeNumber.startsWith("N");
        boolean isLateNight = LocalTime.now().isAfter(LocalTime.of(23, 0))
            || LocalTime.now().isBefore(LocalTime.of(4, 0));

        String routeId = busStopRepository.findRouteIdByRouteNumber(routeNumber);
        if (routeId != null) {
          Map<String, String> timeInfo = fetchRouteTimes(routeId);
          if (timeInfo != null) {
            String first = timeInfo.get("firstTime");
            String last = timeInfo.get("lastTime");
            if (!isNowInServiceTime(first, last)) {
              results.add(new BusArrivalDto(routeNumber, "운행 종료", "운행 종료", stopId, arsId, routeType));
              addedAsEnded = true;
            }
          }
        } else {
          // 🔸 routeId가 없으면 N버스 + 새벽 시간 외에는 운행 종료로 처리
          if (!isNBus || !isLateNight) {
            results.add(new BusArrivalDto(routeNumber, "운행 종료", "운행 종료", stopId, arsId, routeType));
            addedAsEnded = true;
          }
        }

        if (addedAsEnded)
          continue;

        // 🚍 차량 1, 2 도착 정보
        for (int j = 1; j <= 2; j++) {
          String arrivalMsg = getTagValue("arrmsg" + j, item);
          String congestionCode = getTagValue("reride_Num" + j, item);
          String plainNo = getTagValue("plainNo" + j, item);

          if ((arrivalMsg == null || arrivalMsg.isBlank()) &&
              (congestionCode == null || congestionCode.isBlank()) &&
              (plainNo == null || plainNo.isBlank())) {
            continue;
          }

          String status;
          if (arrivalMsg.contains("회차지")) {
            status = "회차 대기";
          } else if (arrivalMsg.equalsIgnoreCase("운행대기") ||
              arrivalMsg.equalsIgnoreCase("도착정보 없음") ||
              arrivalMsg.isBlank()) {
            status = "운행 대기";
          } else {
            status = arrivalMsg;
          }

          String congestion = switch (congestionCode) {
            case "3" -> "여유";
            case "4" -> "보통";
            case "5" -> "혼잡";
            default -> "정보 없음";
          };

          results.add(new BusArrivalDto(routeNumber, status, congestion, stopId, arsId, routeType));
        }
      }

      return ResponseEntity.ok(results);

    } catch (Exception e) {
      // System.err.println("❌ 버스 도착 정보 호출 실패: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(List.of(new BusArrivalDto("오류", "도착 정보 파싱 실패", "정보 없음")));
    }
  }

  private int parseArrivalSeconds(String msg) {
    if (msg == null)
      return -1;
    msg = msg.replaceAll("\\s+", "");

    try {
      if (msg.contains("분") && msg.contains("초")) {
        String[] parts = msg.split("분|초");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
      } else if (msg.contains("분")) {
        return Integer.parseInt(msg.split("분")[0]) * 60;
      } else if (msg.contains("초")) {
        return Integer.parseInt(msg.split("초")[0]);
      } else if (msg.contains("곧도착")) {
        return 30;
      }
    } catch (Exception e) {
      return -1;
    }

    return -1;
  }

  private Map<String, String> fetchRouteTimes(String routeId) {
    // ✅ 캐시에 있으면 바로 반환
    if (routeTimeCache.containsKey(routeId)) {
      return routeTimeCache.get(routeId);
    }

    try {
      String key = dotenv.get("SEOUL_BUS_API_KEY").trim();
      String url = String.format(
          "http://ws.bus.go.kr/api/rest/busRouteInfo/getBusRouteInfo?serviceKey=%s&busRouteId=%s",
          key, routeId);

      HttpResponse<String> resp = HttpClient.newHttpClient()
          .send(HttpRequest.newBuilder()
              .uri(URI.create(url))
              .header("Accept", "application/xml")
              .GET()
              .build(),
              HttpResponse.BodyHandlers.ofString());

      String body = resp.body();
      if (!body.trim().startsWith("<?xml")) {
        throw new RuntimeException("운행시간 응답이 XML이 아닙니다:\n" + body);
      }

      DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document doc = builder.parse(new InputSource(new StringReader(body)));

      NodeList nodeList = doc.getElementsByTagName("itemList");
      if (nodeList.getLength() == 0) {
        // System.err.println("❗ 운행시간 정보 없음 (노선ID: " + routeId + ")");
        return null;
      }

      Element item = (Element) nodeList.item(0);
      String firstRaw = getTagValue("firstBusTm", item);
      String lastRaw = getTagValue("lastBusTm", item);

      String firstTime = formatTime(firstRaw);
      String lastTime = formatTime(lastRaw);

      // ✅ 결과 캐시에 저장 후 반환
      Map<String, String> result = Map.of("firstTime", firstTime, "lastTime", lastTime);
      routeTimeCache.put(routeId, result);

      return result;

    } catch (Exception e) {
      // System.err.println("❌ 운행시간 조회 실패 (" + routeId + "): " + e.getMessage());
      return null;
    }
  }

  private boolean isNowInServiceTime(String first, String last) {
    try {
      LocalTime now = LocalTime.now();
      LocalTime start = LocalTime.parse(first);
      LocalTime end = LocalTime.parse(last);

      if (end.isBefore(start)) {
        // 자정을 넘긴 경우 (예: 23:30 ~ 04:00)
        return now.isAfter(start) || now.isBefore(end);
      } else {
        return !now.isBefore(start) && !now.isAfter(end);
      }
    } catch (Exception e) {
      return true;
    }
  }

  @GetMapping("/bus/stops/in-bounds")
  public ResponseEntity<List<UnifiedBusStopDto>> getStopsInBounds(
      @RequestParam double minLat,
      @RequestParam double maxLat,
      @RequestParam double minLng,
      @RequestParam double maxLng) {

    List<UnifiedBusStopDto> stops = busStopRepository
        .findByLatitudeBetweenAndLongitudeBetween(minLat, maxLat, minLng, maxLng)
        .stream()
        .map(stop -> new UnifiedBusStopDto(
            stop.getNodeId(),
            stop.getStationName(),
            stop.getArsId(),
            stop.getLatitude(),
            stop.getLongitude(),
            null, null, null))
        .limit(1000)
        .toList();

    return ResponseEntity.ok(stops);
  }

  @GetMapping("/bus/detail")
  public ResponseEntity<Map<String, String>> getRouteDetail(
      @RequestParam(required = false) String routeId,
      @RequestParam(required = false) String routeNumber) {

    try {
      String encodedKey = dotenv.get("SEOUL_BUS_API_KEY").trim();

      // 🔍 routeNumber로 정확히 일치하는 routeId 조회
      if ((routeId == null || routeId.isBlank()) && routeNumber != null) {
        String listUrl = String.format(
            "http://ws.bus.go.kr/api/rest/busRouteInfo/getBusRouteList?serviceKey=%s&strSrch=%s",
            encodedKey, routeNumber);

        HttpResponse<String> resp = HttpClient.newHttpClient()
            .send(HttpRequest.newBuilder()
                .uri(URI.create(listUrl))
                .header("Accept", "application/xml")
                .GET()
                .build(),
                HttpResponse.BodyHandlers.ofString());

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(resp.body())));

        NodeList routeItems = doc.getElementsByTagName("itemList");
        String matchedRouteId = null;

        for (int i = 0; i < routeItems.getLength(); i++) {
          Element el = (Element) routeItems.item(i);
          String busRouteNm = getTagValue("busRouteNm", el);

          // System.out.printf("? 찾고자 하는 routeNumber: [%s]%n", routeNumber);
          // System.out.printf("? 응답 받은 busRouteNm: [%s]%n", busRouteNm);

          if (routeNumber.equals(busRouteNm)) {
            matchedRouteId = getTagValue("busRouteId", el);
            break;
          }
        }

        if (matchedRouteId == null) {
          return ResponseEntity.ok(Map.of(
              "routeNumber", routeNumber,
              "interval", "정보 없음",
              "firstTime", "정보 없음",
              "lastTime", "정보 없음"));
        }

        routeId = matchedRouteId;
      }

      if (routeId == null || routeId.isBlank()) {
        return ResponseEntity.ok(Map.of(
            "routeNumber", routeNumber != null ? routeNumber : "알 수 없음",
            "interval", "정보 없음",
            "firstTime", "정보 없음",
            "lastTime", "정보 없음"));
      }

      // ✅ 노선 상세정보 조회
      String detailUrl = String.format(
          "http://ws.bus.go.kr/api/rest/busRouteInfo/getRouteInfo?serviceKey=%s&busRouteId=%s",
          encodedKey, routeId);

      HttpResponse<String> detailResp = HttpClient.newHttpClient()
          .send(HttpRequest.newBuilder()
              .uri(URI.create(detailUrl))
              .header("Accept", "application/xml")
              .GET()
              .build(),
              HttpResponse.BodyHandlers.ofString());

      Document detailDoc = DocumentBuilderFactory.newInstance()
          .newDocumentBuilder()
          .parse(new InputSource(new StringReader(detailResp.body())));

      NodeList nodeList = detailDoc.getElementsByTagName("itemList");

      // ✅ itemList가 없어도 정보 없음으로 응답 (404 아님)
      if (nodeList.getLength() == 0) {
        return ResponseEntity.ok(Map.of(
            "routeNumber", routeNumber != null ? routeNumber : "알 수 없음",
            "interval", "정보 없음",
            "firstTime", "정보 없음",
            "lastTime", "정보 없음"));
      }

      Element item = (Element) nodeList.item(0);

      String routeNm = getTagValue("busRouteNm", item);
      String interval = getTagValue("term", item);
      String firstTime = formatTime(getTagValue("firstBusTm", item));
      String lastTime = formatTime(getTagValue("lastBusTm", item));

      return ResponseEntity.ok(Map.of(
          "routeNumber", routeNm,
          "interval", interval.isBlank() ? "정보 없음" : interval + "분",
          "firstTime", firstTime,
          "lastTime", lastTime));

    } catch (Exception e) {
      // System.err.println("❌ 버스 상세정보 조회 실패: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "API 호출 실패: " + e.getMessage()));
    }
  }

  private String getTagValue(String tag, Element element) {
    NodeList list = element.getElementsByTagName(tag);
    if (list.getLength() > 0 && list.item(0).getFirstChild() != null) {
      return list.item(0).getFirstChild().getNodeValue();
    }
    return "";
  }

  private String formatTime(String raw) {
    if (raw == null || raw.length() < 12)
      return "정보 없음";
    try {
      String hour = raw.substring(8, 10);
      String min = raw.substring(10, 12);
      return hour + ":" + min;
    } catch (Exception e) {
      return "정보 없음";
    }
  }

  @GetMapping("/road-event-all")
  public Mono<String> getAllRoadEvents() {
    return itsClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/eventInfo")
            .queryParam("apiKey", dotenv.get("ITS_API_KEY"))
            .queryParam("type", "all")
            .queryParam(
                "eventType", "all")
            .queryParam("getType", "json")
            .build())
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(String.class)
        .onErrorMap(e -> new RuntimeException("전체 도로 이벤트 API 호출 실패", e));
  }

  @GetMapping("/road-event")
  public Mono<String> getRoadEventInBounds(
      @RequestParam double minX,
      @RequestParam double minY,
      @RequestParam double maxX,
      @RequestParam double maxY) {
    return itsClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/eventInfo")
            .queryParam("apiKey", dotenv.get("ITS_API_KEY"))
            .queryParam("type", "all")
            .queryParam("eventType", "all")
            .queryParam("getType", "json")
            .queryParam("minX", minX)
            .queryParam("maxX", maxX)
            .queryParam("minY", minY)
            .queryParam("maxY", maxY)
            .build())
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(String.class)
        .onErrorMap(e -> new RuntimeException("도로 이벤트 API 호출 실패", e));
  }

  @GetMapping("/subway/arrival")
  public Mono<String> getSubwayArrival() {
    return defaultClient.get()
        .uri("http://swopenapi.seoul.go.kr/api/subway/{key}/xml/realtimeStationArrival/0/1000/",
            dotenv.get("SEOUL_SUBWAY_API_KEY"))
        .retrieve()
        .onStatus(status -> !status.is2xxSuccessful(),
            response -> response.bodyToMono(String.class).flatMap(body -> {
              // System.err.println("❌ [지하철] 오류 상태코드: " + response.statusCode());
              // System.err.println("❌ [지하철] 오류 응답:\n" + body);
              return Mono.error(new RuntimeException(
                  "지하철 도착 정보 API 실패: " + body));
            }))
        .bodyToMono(String.class)
        .onErrorMap(e -> new RuntimeException("지하철 도착 정보 API 호출 실패", e));
  }

  @GetMapping("/bike-list")
  public Mono<String> getBikeList() {
    return seoulOpenApiClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/{apiKey}/json/bikeList/1/1000/")
            .build(dotenv.get("SEOUL_BIKE_API_KEY")))
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(String.class)
        .onErrorMap(e -> new RuntimeException("서울 따릉이 정보 API 호출 실패", e));
  }

  @GetMapping("/parking/seoul-city")
  public Mono<String> getSeoulCityParkingData() {
    return seoulOpenApiClient.get()
        .uri("/{apiKey}/json/GetParkingInfo/1/1000/",
            dotenv.get("SEOUL_CITY_PARKING_API_KEY"))
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .onStatus(status -> !status.is2xxSuccessful(),
            response -> response.bodyToMono(String.class).flatMap(body -> {
              // System.err.println("❌ [주차장] 오류 상태코드: " + response.statusCode());
              // System.err.println("❌ [주차장] 오류 응답:\n" + body);
              return Mono.error(
                  new RuntimeException("주차장 정보 API 실패: " + body));
            }))
        .bodyToMono(String.class)
        .onErrorMap(e -> new RuntimeException("서울 주차장 정보 API 호출 실패", e));
  }

  @GetMapping("/kma-weather")
  public Mono<String> getKmaWeather(@RequestParam double lat, @RequestParam double lon) {
    String serviceKey = dotenv.get("KMA_API_KEY");

    System.out.println("🌐 [기상청] 날씨 요청 수신");
    System.out.println("📍 위도: " + lat + ", 경도: " + lon);
    System.out.println("🔑 serviceKey = " + serviceKey);
    System.out.println("✅ ApiProxyController.getKmaWeather 실행됨");

    // 위도/경도 → 격자
    GeoUtil.GridXY grid = GeoUtil.convertGRID(lat, lon);

    // 날짜/시간 계산
    LocalTime now = LocalTime.now().minusMinutes(10);
    if (now.getMinute() < 40)
      now = now.minusHours(1);

    String baseDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String baseTime = now.truncatedTo(ChronoUnit.HOURS).format(DateTimeFormatter.ofPattern("HHmm"));

    String url = UriComponentsBuilder
        .fromHttpUrl("https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst")
        .queryParam("serviceKey", serviceKey)
        .queryParam("numOfRows", 100)
        .queryParam("pageNo", 1)
        .queryParam("dataType", "JSON")
        .queryParam("base_date", baseDate)
        .queryParam("base_time", baseTime)
        .queryParam("nx", grid.nx)
        .queryParam("ny", grid.ny)
        .build(false)
        .toUriString();

    System.out.println("🌐 최종 호출 URL: " + url);

    // ✅ 이 부분이 핵심: URI 객체로 직접 넣는다
    URI uri = URI.create(url);

    return defaultClient.get()
        .uri(uri) // 여기가 중요!!
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .onStatus(status -> !status.is2xxSuccessful(), response -> response.bodyToMono(String.class).flatMap(body -> {
          System.err.println("❌ [기상청] 오류 상태코드: " + response.statusCode());
          System.err.println("❌ [기상청] 오류 응답:\n" + body);
          return Mono.error(new RuntimeException("기상청 API 호출 실패"));
        }))
        .bodyToMono(String.class);
  }

  // 주변 검색
  @GetMapping("/kakao-nearby")
  public ResponseEntity<String> getNearbyPlaces(
      @RequestParam double lat,
      @RequestParam double lng,
      @RequestParam(defaultValue = "FD6") String category,
      @RequestParam(defaultValue = "500") int radius) {
    String url = UriComponentsBuilder
        .fromHttpUrl("https://dapi.kakao.com/v2/local/search/category.json")
        .queryParam("category_group_code", category)
        .queryParam("x", lng)
        .queryParam("y", lat)
        .queryParam("radius", radius)
        .build()
        .toUriString();

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "KakaoAK " + kakaoApiKey);

    HttpEntity<Void> entity = new HttpEntity<>(headers);
    return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
  }

  // 도로 중심선 버스 경로 찍기 봉 인 !!
/* 
  @GetMapping("/naver-driving-path")
  public ResponseEntity<?> getSmoothedPath(

      @RequestParam double startLat,

      @RequestParam double startLng,

      @RequestParam double goalLat,

      @RequestParam double goalLng) {

    try {
      String response = naverClient.get()
          .uri(uriBuilder -> uriBuilder
              .path("/map-direction-15/v1/driving")
              .queryParam("start", startLng + "," + startLat)
              .queryParam("goal", goalLng + "," + goalLat)
              .queryParam("option", "trafast")
              .build())
          .retrieve()
          .bodyToMono(String.class)
          .block(); // Mono -> String 동기 처리

      ObjectMapper mapper = new ObjectMapper();
      JsonNode root = mapper.readTree(response);
      JsonNode pathArray = root.at("/route/trafast/0/path");

      List<Map<String, Double>> coordinates = new ArrayList<>();
      for (JsonNode coord : pathArray) {
        double lng = coord.get(0).asDouble();
        double lat = coord.get(1).asDouble();
        Map<String, Double> point = new HashMap<>();
        point.put("lat", lat);
        point.put("lng", lng);
        coordinates.add(point);
      }

      return ResponseEntity.ok(coordinates);

    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "경로 처리 실패: " + e.getMessage()));
    }
  }
 */
}