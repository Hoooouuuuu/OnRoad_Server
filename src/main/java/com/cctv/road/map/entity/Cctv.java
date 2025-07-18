package com.cctv.road.map.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cctvs_2025")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cctv {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @Column(name = "video_url", nullable = false, length = 500)
    private String videoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status;

    @Column(name = "fetched_at", nullable = false)
    private java.sql.Timestamp fetchedAt;

    @Column(name = "road_number", length = 20)
    private String roadNumber;

    @Column(name = "road_name", length = 100)
    private String roadName;

    @Column(name = "cctv_spot", length = 100)
    private String cctvSpot;

    @Column(name = "road_type", nullable = false, length = 10)
    private String roadType;

    @Column(name = "cctv_name_origin", length = 255)
    private String cctvNameOrigin;

    public enum Status {
        ACTIVE, INACTIVE
    }

    @Column(name = "cctv_order")
    private Integer cctvOrder;
}
