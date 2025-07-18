package com.cctv.road.map.controller;

import com.cctv.road.map.dto.RoadCctvMappingDto;
import com.cctv.road.map.dto.CctvDto;
import com.cctv.road.map.dto.RoadCoordinateDto;
import com.cctv.road.map.service.RoadCctvMappingService;
import com.cctv.road.map.service.CctvService;
import com.cctv.road.map.service.RoadCoordinateService;
import com.cctv.road.map.entity.Road; // Road.RoadType 쓸 때 필요
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/road-cctv-mapping")
@RequiredArgsConstructor
public class RoadCctvMappingController {
    private final RoadCctvMappingService mappingService;
    private final CctvService cctvService;
    private final RoadCoordinateService roadCoordinateService;

    @GetMapping("/list")
    public List<RoadCctvMappingDto> getAllMappings() {
        return mappingService.getAllMappings();
    }

    @GetMapping("/road/{roadId}")
    public List<RoadCctvMappingDto> getMappingsByRoad(@PathVariable Long roadId) {
        return mappingService.getMappingsByRoad(roadId);
    }

    @GetMapping("/cctv/{cctvId}")
    public List<RoadCctvMappingDto> getMappingsByCctv(@PathVariable Long cctvId) {
        return mappingService.getMappingsByCctv(cctvId);
    }

    @GetMapping("/map-view-data")
    public Map<String, Object> getMapViewData(
            @RequestParam double swLat,
            @RequestParam double swLng,
            @RequestParam double neLat,
            @RequestParam double neLng,
            @RequestParam String roadType,
            @RequestParam(required = false) String roadName) {
        List<CctvDto> cctvs;
        List<RoadCoordinateDto> coords;
        if (roadName == null || roadName.isBlank()) {
            cctvs = cctvService.getCctvsInBounds(swLat, swLng, neLat, neLng, roadType);
            Road.RoadType roadTypeEnum = Road.RoadType.valueOf(roadType);
            coords = roadCoordinateService.getRoadCoordinatesInBounds(
                    swLat, swLng, neLat, neLng, null, roadTypeEnum, 99);
        } else {
            cctvs = cctvService.getCctvsInBounds(swLat, swLng, neLat, neLng, roadType, roadName);
            Road.RoadType roadTypeEnum = Road.RoadType.valueOf(roadType);
            coords = roadCoordinateService.getRoadCoordinatesInBounds(
                    swLat, swLng, neLat, neLng, roadName, roadTypeEnum, 99);
        }

        Map<String, List<Map<String, Double>>> roadLines = coords.stream()
                .collect(Collectors.groupingBy(
                        RoadCoordinateDto::getRoadUfid,
                        Collectors.mapping(
                                rc -> Map.of("lat", rc.getLat(), "lng", rc.getLng()),
                                Collectors.toList())));

        return Map.of(
                "cctvs", cctvs,
                "roadLines", roadLines);
    }
}
