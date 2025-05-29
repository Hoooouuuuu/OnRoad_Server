package com.cctv.road.map.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "bus_stop")
public class BusStop {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "route_id", nullable = false)
  private String routeId;

  @Column(name = "route_name", nullable = false)
  private String routeName;

  @Column(name = "station_order", nullable = false)
  private Integer stationOrder;

  @Column(name = "node_id", nullable = false)
  private String nodeId;

  @Column(name = "ars_id", nullable = false)
  private String arsId;

  @Column(name = "station_name", nullable = false)
  private String stationName;

  @Column(name = "x", nullable = false)
  private Double longitude;

  @Column(name = "y", nullable = false)
  private Double latitude;

  @Column(name = "created_at")
  private LocalDateTime createdAt;
}
