package com.cctv.road.map.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roads_2025")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Road {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String ufid;

    @Column(nullable = false, length = 25)
    private String rdnu;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String scls;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RoadType roadType;

    public enum RoadType { ex, its }
}
