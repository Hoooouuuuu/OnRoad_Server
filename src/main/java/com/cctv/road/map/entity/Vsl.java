package com.cctv.road.map.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vsl", uniqueConstraints = @UniqueConstraint(name = "uniq_vsl", columnNames = "vsl_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Vsl {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vsl_id", nullable = false, length = 32)
    private String vslId;

    @Column(name = "road_name", length = 100)
    private String roadName;

    @Column(name = "road_no", length = 20)
    private String roadNo;

    @Column(name = "def_lmt_speed")
    private Integer defLmtSpeed;

    @Column(name = "direction", length = 10)
    private String direction;

    @Column(name = "vsl_order")
    private Integer vslOrder;

    @Column(name = "lng")
    private Double lng;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "section_desc", length = 255)
    private String sectionDesc;

    @Column(name = "enforcement", length = 16)
    private String enforcement;

    @Enumerated(EnumType.STRING)
    @Column(name = "road_type", nullable = false, length = 8)
    private RoadType roadType;

    public enum RoadType { ex, its }
}