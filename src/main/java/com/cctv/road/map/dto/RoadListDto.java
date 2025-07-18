package com.cctv.road.map.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoadListDto {
    private String roadNumber;
    private String roadName;
    private String roadType;
}
