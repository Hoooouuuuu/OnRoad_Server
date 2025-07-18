package com.cctv.road.map.repository;

import com.cctv.road.map.entity.RoadCoordinate;
import com.cctv.road.map.entity.Road;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RoadCoordinateRepository extends JpaRepository<RoadCoordinate, Long> {
        @Query("SELECT rc FROM RoadCoordinate rc JOIN rc.road r " +
                        "WHERE rc.lat BETWEEN :swLat AND :neLat " +
                        "AND rc.lng BETWEEN :swLng AND :neLng " +
                        "AND rc.level = :level " +
                        "AND r.roadType = :roadType " +
                        "AND r.name = :roadName")
        List<RoadCoordinate> findExInBoundsByRoadName(
                        @Param("swLat") double swLat,
                        @Param("swLng") double swLng,
                        @Param("neLat") double neLat,
                        @Param("neLng") double neLng,
                        @Param("roadType") Road.RoadType roadType,
                        @Param("roadName") String roadName,
                        @Param("level") int level);

        @Query("SELECT rc FROM RoadCoordinate rc JOIN rc.road r " +
                        "WHERE rc.lat BETWEEN :swLat AND :neLat " +
                        "AND rc.lng BETWEEN :swLng AND :neLng " +
                        "AND rc.level = :level " +
                        "AND r.roadType = :roadType " +
                        "AND r.rdnu = :roadNumber " +
                        "AND r.name NOT LIKE %:highwayKeyword%")
        List<RoadCoordinate> findItsInBoundsByRoadNumberNotHighway(
                        @Param("swLat") double swLat,
                        @Param("swLng") double swLng,
                        @Param("neLat") double neLat,
                        @Param("neLng") double neLng,
                        @Param("roadType") Road.RoadType roadType,
                        @Param("roadNumber") String roadNumber,
                        @Param("level") int level,
                        @Param("highwayKeyword") String highwayKeyword // "고속도로"
        );

        @Query("SELECT rc FROM RoadCoordinate rc JOIN rc.road r " +
                        "WHERE rc.lat BETWEEN :swLat AND :neLat " +
                        "AND rc.lng BETWEEN :swLng AND :neLng " +
                        "AND rc.level = :level " +
                        "AND r.rdnu = :roadNumber " +
                        "AND r.name NOT LIKE %:highwayKeyword%")
        List<RoadCoordinate> findNationalRoadWithoutRoadType(
                        @Param("swLat") double swLat,
                        @Param("swLng") double swLng,
                        @Param("neLat") double neLat,
                        @Param("neLng") double neLng,
                        @Param("roadNumber") String roadNumber,
                        @Param("level") int level,
                        @Param("highwayKeyword") String highwayKeyword // "고속도로"
        );
}
