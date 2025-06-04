package com.cctv.road.map.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cctv.road.map.entity.Parking;

public interface ParkingRepository extends JpaRepository<Parking, Long> {
    Optional<Parking> findByAddress(String address);
    Optional<Parking> findByName(String name); // 🔧 이름 기준 조회 메서드
}
