package com.cctv.road.map.dto;

import com.cctv.road.map.entity.RoadCctvMapping;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoadCctvMappingDto {
    private Long id;
    private Long roadId;
    private Long cctvId;

    public static RoadCctvMappingDto from(RoadCctvMapping m) {
        return RoadCctvMappingDto.builder()
                .id(m.getId())
                .roadId(m.getRoad().getId())
                .cctvId(m.getCctv().getId())
                .build();
    }
}
