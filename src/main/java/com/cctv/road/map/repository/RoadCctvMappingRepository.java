package com.cctv.road.map.repository;

import com.cctv.road.map.entity.RoadCctvMapping;
import com.cctv.road.map.entity.Road;
import com.cctv.road.map.entity.Cctv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoadCctvMappingRepository extends JpaRepository<RoadCctvMapping, Long> {
    List<RoadCctvMapping> findByRoad(Road road);

    List<RoadCctvMapping> findByCctv(Cctv cctv);

    List<RoadCctvMapping> findByCctvId(Long cctvId);

    List<RoadCctvMapping> findByRoadIdOrderByCctvOrderAscIdAsc(Long roadId);

    @Query("SELECT m FROM RoadCctvMapping m JOIN FETCH m.cctv WHERE m.road.name = :roadName ORDER BY m.cctvOrder ASC, m.id ASC")
    List<RoadCctvMapping> findByRoadNameOrdered(@Param("roadName") String roadName);
}
