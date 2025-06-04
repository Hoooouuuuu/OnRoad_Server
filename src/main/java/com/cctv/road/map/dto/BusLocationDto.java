package com.cctv.road.map.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BusLocationDto {
  private String vehicleId;
  private double lat;
  private double lng;
}