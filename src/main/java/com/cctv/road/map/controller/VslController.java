package com.cctv.road.map.controller;

import com.cctv.road.map.dto.VslDto;
import com.cctv.road.map.service.VslService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vsl")
@RequiredArgsConstructor
public class VslController {
    private final VslService vslService;

    // 실시간 VSL 리스트 (5분 캐싱)
    @GetMapping("/realtime")
    public List<VslDto> getVslRealtimeList() {
        return vslService.getVslRealtimeList();
    }

    // 노선명별 VSL 목록 (정적, 옵션)
    @GetMapping("/list")
    public List<VslDto> getVslListByRoadName(@RequestParam("roadName") String roadName) {
        // roadName으로만 단순 필터
        return vslService.getVslRealtimeList().stream()
                .filter(dto -> dto.getRoadName() != null && dto.getRoadName().equals(roadName))
                .toList();
    }

    // 🟢 가공된 노선 리스트 API
    @GetMapping("/roads")
    public List<Map<String, Object>> getVslRoadInfos(
        @RequestParam(value = "roadType", required = false) String roadType
    ) {
        return vslService.getVslRoadInfos(roadType);
    }
}
