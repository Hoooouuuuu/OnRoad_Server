package com.cctv.road.map.dto;

import com.cctv.road.map.entity.RoadCoordinate;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoadCoordinateDto {
    private Long id;
    private String roadUfid;
    private Double lat;
    private Double lng;
    private Integer level;

    public static RoadCoordinateDto from(RoadCoordinate rc) {
        return RoadCoordinateDto.builder()
            .id(rc.getId())
            .roadUfid(rc.getRoadUfid())
            .lat(rc.getLat())
            .lng(rc.getLng())
            .level(rc.getLevel())
            .build();
    }
}
