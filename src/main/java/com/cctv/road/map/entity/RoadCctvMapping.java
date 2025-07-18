package com.cctv.road.map.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "road_cctv_mapping_2025", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "road_id", "cctv_id" })
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoadCctvMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "road_id", nullable = false)
    private Road road;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cctv_id", nullable = false)
    private Cctv cctv;

    @Column(name = "cctv_order")
    private Integer cctvOrder;
}
