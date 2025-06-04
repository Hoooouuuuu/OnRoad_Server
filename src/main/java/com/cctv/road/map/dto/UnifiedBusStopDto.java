package com.cctv.road.map.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnifiedBusStopDto {
  private String stopId; // 정류소 ID (nodeId)
  private String name; // 정류소 이름
  private String arsId; // ARS ID
  private double lat;
  private double lng;

  private String routeId; // 노선 ID (nullable)
  private String routeNumber; // 노선 번호 (nullable)
  private Integer stationOrder; // 순서 (nullable)
}