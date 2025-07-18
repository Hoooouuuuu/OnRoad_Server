package com.cctv.road.map.service;

import com.cctv.road.map.entity.Road;
import com.cctv.road.map.dto.RoadCoordinateDto;
import com.cctv.road.map.repository.RoadCoordinateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoadCoordinateService {
        private final RoadCoordinateRepository roadCoordinateRepository;

        @Transactional(readOnly = true)
        public List<RoadCoordinateDto> getRoadCoordinatesInBounds(
                        double swLat, double swLng, double neLat, double neLng,
                        String roadName, Road.RoadType roadType, int level) {
                return roadCoordinateRepository.findExInBoundsByRoadName(
                                swLat, swLng, neLat, neLng, roadType, roadName, level).stream()
                                .map(RoadCoordinateDto::from)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<RoadCoordinateDto> getNationalRoadCoordinatesNotHighway(
                        double swLat, double swLng, double neLat, double neLng,
                        String roadNumber, Road.RoadType roadType, int level) {
                return roadCoordinateRepository.findItsInBoundsByRoadNumberNotHighway(
                                swLat, swLng, neLat, neLng, roadType, roadNumber, level, "고속도로").stream()
                                .map(RoadCoordinateDto::from)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<RoadCoordinateDto> getNationalRoadCoordinatesWithoutRoadType(
                        double swLat, double swLng, double neLat, double neLng,
                        String roadNumber, int level) {
                return roadCoordinateRepository.findNationalRoadWithoutRoadType(
                                swLat, swLng, neLat, neLng, roadNumber, level, "고속도로").stream()
                                .map(RoadCoordinateDto::from)
                                .collect(Collectors.toList());
        }
}
