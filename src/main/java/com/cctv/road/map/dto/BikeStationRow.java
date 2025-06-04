package com.cctv.road.map.dto;

import lombok.Data;

@Data
public class BikeStationRow {
  private String rackTotCnt;
  private String stationName;
  private String parkingBikeTotCnt;
  private String shared;
  private String stationLatitude;
  private String stationLongitude;
  private String stationId;
}