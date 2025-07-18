package com.cctv.road.map.service;

import com.cctv.road.map.entity.Cctv;
import com.cctv.road.map.entity.RoadCctvMapping;
import com.cctv.road.map.dto.CctvDto;
import com.cctv.road.map.repository.CctvRepository;
import com.cctv.road.map.repository.RoadCctvMappingRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CctvService {
    private final CctvRepository cctvRepository;

    @Autowired
    private RoadCctvMappingRepository mappingRepository;

    @Transactional(readOnly = true)
    public List<CctvDto> getAllCctvs() {
        return cctvRepository.findAll().stream()
                .map(CctvDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CctvDto> getCctvsByRoadName(String roadName) {
        List<CctvDto> cctvs = cctvRepository.findByRoadName(roadName)
                .stream()
                .map(CctvDto::from)
                .collect(Collectors.toList());
        return sortCctvListWithNullCenter(cctvs);
    }

    private List<CctvDto> sortCctvListWithNullCenter(List<CctvDto> cctvList) {
        List<CctvDto> notNullList = cctvList.stream()
                .filter(c -> c.getCctvOrder() != null)
                .sorted(Comparator.comparingInt(CctvDto::getCctvOrder))
                .collect(Collectors.toList());

        List<CctvDto> nullList = cctvList.stream()
                .filter(c -> c.getCctvOrder() == null)
                .collect(Collectors.toList());

        int half = (int) Math.ceil(notNullList.size() / 2.0);
        List<CctvDto> result = new ArrayList<>();
        result.addAll(notNullList.subList(0, half));
        result.addAll(nullList);
        result.addAll(notNullList.subList(half, notNullList.size()));
        return result;
    }

    @Transactional(readOnly = true)
    public List<CctvDto> getCctvsByRoadNumber(String roadNumber) {
        return cctvRepository.findByRoadNumber(roadNumber).stream()
                .map(CctvDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CctvDto> getCctvsByStatus(String status) {
        return cctvRepository.findByStatus(Cctv.Status.valueOf(status)).stream()
                .map(CctvDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CctvDto> getCctvsByCctvSpot(String cctvSpot) {
        return cctvRepository.findByCctvSpot(cctvSpot).stream()
                .map(CctvDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CctvDto> searchCctvsByName(String keyword) {
        return cctvRepository.findByNameContaining(keyword).stream()
                .map(CctvDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CctvDto getCctvById(Long id) {
        return cctvRepository.findById(id).map(CctvDto::from).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<CctvDto> getCctvsByRoadNameAndNumber(String roadName, String roadNumber) {
        return cctvRepository.findByRoadNameAndRoadNumber(roadName, roadNumber)
                .stream().map(CctvDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CctvDto> getDistinctRoadListByType(String roadType) {
        List<Object[]> rows = cctvRepository.findDistinctRoadsByRoadType(roadType);
        for (Object[] row : rows) {
        }
        List<CctvDto> list = new ArrayList<>();
        for (Object[] row : rows) {
            String roadNumber = (String) row[0];
            String roadName = (String) row[1];
            String type = (String) row[2];
            if (roadNumber == null || roadName == null || type == null)
                continue;
            list.add(new CctvDto(roadNumber, roadName, type));
        }
        list.sort(Comparator.comparingInt(dto -> {
            try {
                return Integer.parseInt(dto.getRoadNumber().replaceAll("\\D", ""));
            } catch (Exception e) {
                return 9999;
            }
        }));
        return list;
    }

    @Transactional(readOnly = true)
    public List<CctvDto> getCctvsByRoad(String roadNumber, String roadName, String roadType) {
        return cctvRepository.findByRoadNumberAndRoadNameAndRoadType(roadNumber, roadName, roadType)
                .stream().map(CctvDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CctvDto> getCctvsInBounds(double swLat, double swLng, double neLat, double neLng, String roadType) {
        return cctvRepository.findByBoundsAndRoadType(swLat, swLng, neLat, neLng, roadType)
                .stream().map(CctvDto::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CctvDto> getCctvsInBounds(double swLat, double swLng, double neLat, double neLng, String roadType,
            String roadName) {
        return cctvRepository.findByBoundsAndRoadTypeAndRoadName(swLat, swLng, neLat, neLng, roadType, roadName)
                .stream().map(CctvDto::from).collect(Collectors.toList());
    }
}
