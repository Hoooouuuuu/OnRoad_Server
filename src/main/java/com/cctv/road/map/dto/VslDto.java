package com.cctv.road.map.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VslDto {
    private String vslId;
    private String roadName;
    private String roadNo;
    private Integer defLmtSpeed;
    private String direction;
    private Integer vslOrder;
    private Double lng;
    private Double lat;
    private String sectionDesc;
    private String enforcement;
    private String roadType; // "ex" or "its"
    private Integer currLmtSpeed; // 실시간 제한속도 (API연동값, 없으면 null)
    private String fullList; // ★ 추가!

}
