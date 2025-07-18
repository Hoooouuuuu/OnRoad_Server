package com.cctv.road.map.repository;

import com.cctv.road.map.entity.Vsl;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface VslRepository extends JpaRepository<Vsl, Long> {
    Optional<Vsl> findByVslId(String vslId);
    List<Vsl> findByRoadType(Vsl.RoadType roadType);
    List<Vsl> findByRoadName(String roadName);
    List<Vsl> findByRoadNo(String roadNo);
    List<Vsl> findAllByOrderByRoadNoAsc();
}
