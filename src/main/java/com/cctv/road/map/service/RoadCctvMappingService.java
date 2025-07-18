package com.cctv.road.map.service;

import com.cctv.road.map.dto.RoadCctvMappingDto;
import com.cctv.road.map.repository.RoadCctvMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoadCctvMappingService {
    private final RoadCctvMappingRepository mappingRepository;

    @Transactional(readOnly = true)
    public List<RoadCctvMappingDto> getAllMappings() {
        return mappingRepository.findAll().stream()
                .map(RoadCctvMappingDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoadCctvMappingDto> getMappingsByRoad(Long roadId) {
        return mappingRepository.findByRoadIdOrderByCctvOrderAscIdAsc(roadId)
                .stream().map(RoadCctvMappingDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoadCctvMappingDto> getMappingsByCctv(Long cctvId) {
        return mappingRepository.findByCctvId(cctvId)
                .stream().map(RoadCctvMappingDto::from).collect(Collectors.toList());
    }
}
