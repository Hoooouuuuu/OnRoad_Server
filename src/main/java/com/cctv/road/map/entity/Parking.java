package com.cctv.road.map.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "parking")
public class Parking {
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;
  private String address;
  private String tel;
  private String payType; // 유료 / 무료
  private String parkingType; // 노상 / 노외 / 부설

  private Integer capacity;
  private Integer currentCount;
  private Integer availableCount;

  private Integer baseCharge;
  private Integer baseMinutes;
  private Integer addCharge;
  private Integer addMinutes;
  private Integer dayMaxCharge;

  private String weekdayStart;
  private String weekdayEnd;
  private String holidayStart;
  private String holidayEnd;

  private Double lat;
  private Double lng;

  private String sourceType;

  @UpdateTimestamp
  private LocalDateTime lastUpdated;
}
