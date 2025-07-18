package com.cctv.road.map.controller;

import com.cctv.road.map.dto.CctvDto;
import com.cctv.road.map.service.CctvService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cctvs")
@RequiredArgsConstructor
public class CctvController {
    private final CctvService cctvService;

    // 전체 CCTV
    @GetMapping
    public List<CctvDto> getAllCctvs() {
        return cctvService.getAllCctvs();
    }

    // 도로명별 CCTV
    @GetMapping("/by-road-name")
    public List<CctvDto> getCctvsByRoadName(@RequestParam String roadName) {
        return cctvService.getCctvsByRoadName(roadName);
    }

    // 도로번호별 CCTV
    @GetMapping("/by-road-number")
    public List<CctvDto> getCctvsByRoadNumber(@RequestParam String roadNumber) {
        return cctvService.getCctvsByRoadNumber(roadNumber);
    }

    // 상태별 CCTV (ACTIVE/INACTIVE)
    @GetMapping("/by-status")
    public List<CctvDto> getCctvsByStatus(@RequestParam String status) {
        return cctvService.getCctvsByStatus(status);
    }

    // CCTV 위치 spot별
    @GetMapping("/by-spot")
    public List<CctvDto> getCctvsBySpot(@RequestParam String spot) {
        return cctvService.getCctvsByCctvSpot(spot);
    }

    // 이름 Like 검색
    @GetMapping("/search")
    public List<CctvDto> searchCctvs(@RequestParam String keyword) {
        return cctvService.searchCctvsByName(keyword);
    }

    // CCTV 상세(ID)
    @GetMapping("/{id}")
    public CctvDto getCctvById(@PathVariable Long id) {
        return cctvService.getCctvById(id);
    }

    // 도로명+도로번호 조합 CCTV
    @GetMapping("/by-road-name-and-number")
    public List<CctvDto> getCctvsByRoadNameAndNumber(
            @RequestParam String roadName, @RequestParam String roadNumber) {
        return cctvService.getCctvsByRoadNameAndNumber(roadName, roadNumber);
    }

    // ✅ 도로리스트 (고속/국도 구분, 중복제거)
    @GetMapping("/roads")
    public List<CctvDto> getRoadListByType(@RequestParam String roadType) {
        return cctvService.getDistinctRoadListByType(roadType);
    }

    // ✅ 도로/번호/타입별 CCTV 리스트(도로 패널용)
    @GetMapping("/spots")
    public List<CctvDto> getCctvsByRoad(
            @RequestParam String roadNumber,
            @RequestParam String roadName,
            @RequestParam String roadType) {
        return cctvService.getCctvsByRoad(roadNumber, roadName, roadType);
    }

    // ✅ 바운드(뷰포트) 내 CCTV만
    @GetMapping("/in-bounds")
    public List<CctvDto> getCctvsInBounds(
            @RequestParam double swLat,
            @RequestParam double swLng,
            @RequestParam double neLat,
            @RequestParam double neLng,
            @RequestParam(required = false) String roadType, // 도로타입 옵션(필요시)
            @RequestParam(required = false) String roadName // 도로명 옵션
    ) {
        // 파라미터 순서! (roadType, roadName 순서)
        return cctvService.getCctvsInBounds(swLat, swLng, neLat, neLng, roadType, roadName);
    }
}
