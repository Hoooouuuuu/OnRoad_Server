package com.cctv.road.map.dto;

import com.cctv.road.map.entity.Cctv;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CctvDto {
    private Long id;
    private String name;
    private String cctvSpot;
    private Double lat;
    private Double lng;
    private String videoUrl;
    private String status;
    private String roadNumber;
    private String roadName;
    private String roadType;
    private Integer cctvOrder;

    public static CctvDto from(Cctv entity) {
        return from(entity, null);
    }

    public static CctvDto from(Cctv entity, Integer order) {
        CctvDto dto = CctvDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .cctvSpot(entity.getCctvSpot())
                .lat(entity.getLat())
                .lng(entity.getLng())
                .videoUrl(entity.getVideoUrl())
                .cctvOrder(entity.getCctvOrder())
                .status(entity.getStatus().name())
                .roadNumber(entity.getRoadNumber())
                .roadName(entity.getRoadName())
                .roadType(entity.getRoadType())
                .build();
        return dto;
    }

    public CctvDto(String roadNumber, String roadName, String roadType) {
        this.roadNumber = roadNumber;
        this.roadName = roadName;
        this.roadType = roadType;
    }
}
