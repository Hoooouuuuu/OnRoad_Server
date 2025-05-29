package com.cctv.road.map.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cctv.road.map.entity.Parking;
import com.cctv.road.map.repository.ParkingRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/parking")
@RequiredArgsConstructor
public class ParkingController {

    private final ParkingRepository parkingRepository;

    @GetMapping
    public List<Parking> getAll() {
        return parkingRepository.findAll();
    }
}
