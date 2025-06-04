package com.cctv.road.map.dto;

import lombok.Data;

@Data
public class BusArrivalDto {
  private String routeNumber;
  private String arrivalTime;
  private String congestion;
  private String stopId;
  private String arsId;
  private String routeType;

  public BusArrivalDto(String routeNumber, String arrivalTime, String congestion,
      String stopId, String arsId, String rawRouteType) {
    this.routeNumber = routeNumber;
    this.arrivalTime = arrivalTime;
    this.congestion = congestion;
    this.stopId = stopId;
    this.arsId = arsId;
    this.routeType = normalizeType(rawRouteType);
  }

  public BusArrivalDto(String routeNumber, String arrivalTime, String congestion) {
    this(routeNumber, arrivalTime, congestion, null, null, null);
  }

  private String normalizeType(String raw) {
    if (raw == null)
      return "기타";
    return switch (raw.replace("버스", "")) {
      case "간선" -> "간선";
      case "지선" -> "지선";
      case "광역" -> "광역";
      case "마을" -> "마을";
      case "순환" -> "순환";
      case "공항" -> "공항";
      case "경기" -> "경기";
      case "인천" -> "인천";
      default -> "기타";
    };
  }
}
