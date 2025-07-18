package com.cctv.road.map.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "road_coordinates_2025")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoadCoordinate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "road_id", nullable = false)
    private Road road;

    @Column(name = "road_ufid", nullable = false, length = 255)
    private String roadUfid;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @Column(nullable = false)
    private Integer level;

    @Column(name = "line_index", nullable = false)
    private Integer lineIndex;

    @Column(name = "point_index", nullable = false)
    private Integer pointIndex;
}
