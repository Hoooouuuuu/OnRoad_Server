package com.cctv.road.map.service;

import com.cctv.road.map.dto.RoadDto;
import com.cctv.road.map.entity.Road;
import com.cctv.road.map.repository.RoadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoadService {
    private final RoadRepository roadRepository;

    @Transactional(readOnly = true)
    public List<RoadDto> getAllRoads() {
        return roadRepository.findAll().stream().map(RoadDto::from).collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public List<RoadDto> getRoadsByRoadType(String roadType) {
        return roadRepository.findByRoadType(Road.RoadType.valueOf(roadType)).stream()
                .map(RoadDto::from).collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public Optional<RoadDto> getRoadByName(String name) {
        return roadRepository.findByName(name).map(RoadDto::from);
    }
    @Transactional(readOnly = true)
    public List<RoadDto> getRoadsByRdnu(String rdnu) {
        return roadRepository.findByRdnu(rdnu).stream().map(RoadDto::from).collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public Optional<RoadDto> getRoadByUfid(String ufid) {
        return roadRepository.findByUfid(ufid).map(RoadDto::from);
    }
    @Transactional(readOnly = true)
    public List<RoadDto> getRoadsByNameContaining(String keyword) {
        return roadRepository.findByNameContaining(keyword).stream().map(RoadDto::from).collect(Collectors.toList());
    }
}
