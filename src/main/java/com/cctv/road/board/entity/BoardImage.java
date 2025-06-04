package com.cctv.road.board.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "board_image")
@NoArgsConstructor
@AllArgsConstructor
public class BoardImage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long imageId; // 이미지 고유 ID

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "board_num", nullable = false)
  private Board board; // 게시글 (연관 관계)

  @Column(name = "image_path", nullable = false)
  private String imagePath; // 이미지 경로

  @Column(name = "image_title")
  private String imageTitle; // 이미지 제목 또는 설명

}
