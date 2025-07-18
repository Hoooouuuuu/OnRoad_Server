package com.cctv.road.map.controller;

import com.cctv.road.map.dto.RoadCoordinateDto;
import com.cctv.road.map.entity.Road;
import com.cctv.road.map.service.RoadCoordinateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/road-coordinates")
@RequiredArgsConstructor
public class RoadCoordinateController {
    private final RoadCoordinateService roadCoordinateService;

    @GetMapping("/in-bounds")
    public List<RoadCoordinateDto> getRoadCoordinatesInBounds(
            @RequestParam double swLat,
            @RequestParam double swLng,
            @RequestParam double neLat,
            @RequestParam double neLng,
            @RequestParam(required = false) String roadName,
            @RequestParam(required = false) String roadType,
            @RequestParam(required = false) String roadNumber,
            @RequestParam(defaultValue = "10") int level) {
        Road.RoadType roadTypeEnum = null;
        if (roadType != null && !roadType.isEmpty()) {
            roadTypeEnum = Road.RoadType.valueOf(roadType);
        }

        // 고속도로 (roadName/roadType 모두 있을 때)
        if (roadTypeEnum == Road.RoadType.ex && roadName != null && !roadName.isEmpty()) {
            return roadCoordinateService.getRoadCoordinatesInBounds(
                    swLat, swLng, neLat, neLng, roadName, roadTypeEnum, level);
        }

        // 국도/지방도 (roadType=its, roadNumber 있을 때)
        if (roadTypeEnum == Road.RoadType.its && roadNumber != null && !roadNumber.isEmpty()) {
            return roadCoordinateService.getNationalRoadCoordinatesNotHighway(
                    swLat, swLng, neLat, neLng, roadNumber, roadTypeEnum, 99);
        }

        return Collections.emptyList();
    }

    @GetMapping("/nationalroad-centerline-in-bounds")
    public List<RoadCoordinateDto> getNationalRoadCenterlineInBounds(
            @RequestParam double swLat,
            @RequestParam double swLng,
            @RequestParam double neLat,
            @RequestParam double neLng,
            @RequestParam String roadNumber) {
        return roadCoordinateService.getNationalRoadCoordinatesWithoutRoadType(
                swLat, swLng, neLat, neLng, roadNumber, 99);
    }
}
