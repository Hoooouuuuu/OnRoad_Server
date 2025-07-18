package com.cctv.road.map.controller;

import com.cctv.road.map.dto.RoadDto;
import com.cctv.road.map.service.RoadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/roads")
@RequiredArgsConstructor
public class RoadController {
    private final RoadService roadService;

    @GetMapping("/list")
    public List<RoadDto> getAllRoads(@RequestParam(required = false, name = "type") String type) {
        if (type != null && !type.isEmpty()) {
            return roadService.getRoadsByRoadType(type);
        }
        return roadService.getAllRoads();
    }

    @GetMapping("/by-type")
    public List<RoadDto> getRoadsByRoadType(@RequestParam String roadType) {
        return roadService.getRoadsByRoadType(roadType);
    }

    @GetMapping("/by-name")
    public Optional<RoadDto> getRoadByName(@RequestParam String name) {
        return roadService.getRoadByName(name);
    }

    @GetMapping("/by-number")
    public List<RoadDto> getRoadsByRdnu(@RequestParam String roadNumber) {
        return roadService.getRoadsByRdnu(roadNumber);
    }

    @GetMapping("/by-ufid")
    public Optional<RoadDto> getRoadByUfid(@RequestParam String ufid) {
        return roadService.getRoadByUfid(ufid);
    }

    @GetMapping("/search")
    public List<RoadDto> searchRoadsByName(@RequestParam String keyword) {
        return roadService.getRoadsByNameContaining(keyword);
    }
}
