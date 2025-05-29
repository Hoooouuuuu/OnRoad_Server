package com.cctv.road.map.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.cctv.road.map.dto.UnifiedBusStopDto;
import com.cctv.road.map.entity.BusStop;
import com.cctv.road.map.repository.BusStopRepository;

import jakarta.transaction.Transactional;

@Service
public class BusService {

  private final BusStopRepository busStopRepository;

  public BusService(BusStopRepository busStopRepository) {
    this.busStopRepository = busStopRepository;
  }

  @Transactional
  public List<UnifiedBusStopDto> getStopsByBounds(double minLat, double maxLat, double minLng, double maxLng) {
    List<BusStop> stops = busStopRepository.findByLatitudeBetweenAndLongitudeBetween(minLat, maxLat, minLng, maxLng);

    return stops.stream()
        .limit(500)
        .map(stop -> new UnifiedBusStopDto(
            stop.getNodeId(), // stopId
            stop.getStationName(), // name
            stop.getArsId(), // arsId
            stop.getLatitude(),
            stop.getLongitude(),
            null, // routeId 없음
            null, // routeNumber 없음
            null // stationOrder 없음
        ))
        .collect(Collectors.toList());
  }
}
