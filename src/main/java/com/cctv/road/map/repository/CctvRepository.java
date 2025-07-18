package com.cctv.road.map.repository;

import com.cctv.road.map.entity.Cctv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CctvRepository extends JpaRepository<Cctv, Long> {

        List<Cctv> findByRoadNumber(String roadNumber);

        List<Cctv> findByRoadName(String roadName);

        List<Cctv> findByStatus(Cctv.Status status);

        List<Cctv> findByCctvSpot(String cctvSpot);

        List<Cctv> findByNameContaining(String keyword);

        List<Cctv> findByRoadNameAndRoadNumber(String roadName, String roadNumber);

        List<Cctv> findByRoadType(String roadType);

        List<Cctv> findByRoadNumberAndRoadNameAndRoadType(String roadNumber, String roadName, String roadType);

        @Query("SELECT DISTINCT c.roadNumber, c.roadName, c.roadType FROM Cctv c WHERE c.roadType = :roadType AND c.roadNumber IS NOT NULL AND c.roadName IS NOT NULL AND c.roadType IS NOT NULL")
        List<Object[]> findDistinctRoadsByRoadType(@Param("roadType") String roadType);

        @Query("SELECT c FROM Cctv c WHERE " +
                        "c.lat BETWEEN :swLat AND :neLat AND c.lng BETWEEN :swLng AND :neLng " +
                        "AND c.roadType = :roadType")
        List<Cctv> findByBoundsAndRoadType(
                        @Param("swLat") double swLat,
                        @Param("swLng") double swLng,
                        @Param("neLat") double neLat,
                        @Param("neLng") double neLng,
                        @Param("roadType") String roadType);

        @Query("SELECT c FROM Cctv c WHERE " +
                        "c.lat BETWEEN :swLat AND :neLat AND c.lng BETWEEN :swLng AND :neLng " +
                        "AND c.roadType = :roadType AND c.roadName = :roadName")
        List<Cctv> findByBoundsAndRoadTypeAndRoadName(
                        @Param("swLat") double swLat,
                        @Param("swLng") double swLng,
                        @Param("neLat") double neLat,
                        @Param("neLng") double neLng,
                        @Param("roadType") String roadType,
                        @Param("roadName") String roadName);

        @Query("SELECT c FROM Cctv c WHERE c.roadName = :roadName ORDER BY c.cctvOrder ASC, c.id ASC")
        List<Cctv> findByRoadNameOrderByCctvOrderAsc(@Param("roadName") String roadName);

        @Query("SELECT c FROM Cctv c WHERE c.roadName = :roadName AND c.roadNumber = :roadNumber ORDER BY c.cctvOrder ASC, c.id ASC")
        List<Cctv> findByRoadNameAndRoadNumberOrderByCctvOrderAsc(
                        @Param("roadName") String roadName,
                        @Param("roadNumber") String roadNumber);
}
