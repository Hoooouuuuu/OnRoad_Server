package com.cctv.road.map.dto;

import com.cctv.road.map.entity.Road;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoadDto {
    private Long id;
    private String ufid;
    private String rdnu;
    private String name;
    private String scls;
    private String roadType;

    public static RoadDto from(Road entity) {
        return RoadDto.builder()
                .id(entity.getId())
                .ufid(entity.getUfid())
                .rdnu(entity.getRdnu())
                .name(entity.getName())
                .scls(entity.getScls())
                .roadType(entity.getRoadType().name())
                .build();
    }
}
