package com.cctv.road.map.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cctv.road.map.entity.BusStop;
import com.cctv.road.map.repository.projection.BusRouteView;

public interface BusStopRepository extends JpaRepository<BusStop, Integer> {

  List<BusStop> findByRouteNameOrderByStationOrderAsc(String routeNumber);

  @Query("SELECT DISTINCT b.routeId FROM BusStop b WHERE b.routeName = :routeNumber")
  String findRouteIdByRouteNumber(@Param("routeNumber") String routeNumber); // ✅ 이게 꼭 필요함

  @Query("SELECT DISTINCT b.routeId AS routeId, b.routeName AS routeName FROM BusStop b WHERE b.nodeId = :stopId")
  List<BusRouteView> findRoutesByStopId(@Param("stopId") String stopId);

  List<BusStop> findByLatitudeBetweenAndLongitudeBetween(double minLat, double maxLat, double minLng, double maxLng);

  @Query("SELECT DISTINCT b.routeId, b.routeName FROM BusStop b WHERE b.nodeId = :nodeId")
  List<Object[]> findRoutesByNodeId(@Param("nodeId") String nodeId);
}
