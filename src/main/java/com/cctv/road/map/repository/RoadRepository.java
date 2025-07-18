package com.cctv.road.map.repository;

import com.cctv.road.map.entity.Road;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RoadRepository extends JpaRepository<Road, Long> {
    List<Road> findByRoadType(Road.RoadType roadType);
    Optional<Road> findByName(String name);
    List<Road> findByRdnu(String rdnu);
    Optional<Road> findByUfid(String ufid);
    List<Road> findByUfidIn(List<String> ufids);
    List<Road> findByNameContaining(String keyword);
}
