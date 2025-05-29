package com.cctv.road.map.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cctv.road.map.service.ParkingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ParkingService parkingService;

    @GetMapping("/init-parking")
    public String initParkingData() {
        parkingService.initializeParkingData();
        return "🚀 주차장 초기화 실행됨!";
    }
}
