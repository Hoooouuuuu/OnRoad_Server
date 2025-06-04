package com.cctv.road.board.dto;

import lombok.Data;

@Data
public class BoardImageDTO {
    private Long imageId; // 이미지 고유 ID
    private String imagePath; // 이미지 경로
    private String imageTitle; // 이미지 제목
}
