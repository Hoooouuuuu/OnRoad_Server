package com.cctv.road.map.dto;

import java.util.List;

import lombok.Data;

@Data
public class RentBikeStatus {
  private int list_total_count;
  private BikeResultMeta RESULT;
  private List<BikeStationRow> row;
}