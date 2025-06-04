package com.cctv.road.board.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.cctv.road.board.entity.Board;

import lombok.Data;

@Data
public class BoardDTO {
    private Integer boardNum; // 새로 추가된 PK
    private String userId;
    private String nickName;
    private Integer categoryId;
    private Integer boardSeq;
    private String subject;
    private String content;
    private Integer hit;
    private LocalDateTime writedate;
    private boolean notice;
    private List<BoardImageDTO> imageFileNames = new ArrayList<>(); // 빈 리스트로 초기화

    public static BoardDTO fromEntity(Board board) {
        BoardDTO dto = new BoardDTO();
        dto.setBoardNum(board.getBoardNum());
        dto.setUserId(board.getMember() != null ? board.getMember().getUserId() : null);
        dto.setNickName(board.getNickName());
        dto.setCategoryId(board.getCategoryId());
        dto.setBoardSeq(board.getBoardSeq());
        dto.setSubject(board.getSubject());
        dto.setContent(board.getContent());
        dto.setHit(board.getHit());
        dto.setWritedate(board.getWritedate());
        dto.setNotice(board.isNotice());

        // 이미지 리스트 세팅
        if (board.getImages() != null) {
            List<BoardImageDTO> imageDTOList = board.getImages().stream()
                .map(image -> {
                    BoardImageDTO dtoImg = new BoardImageDTO();
                    dtoImg.setImageId(image.getImageId());
                    dtoImg.setImagePath(image.getImagePath());
                    dtoImg.setImageTitle(image.getImageTitle()); // 필요 없으면 생략
                    return dtoImg;
                })
                .collect(Collectors.toList());
        
            dto.setImageFileNames(imageDTOList);
        
            // 디버깅용 출력
            // System.out.println("이미지 DTO 리스트: " + imageDTOList);
        }

        return dto;
    }
}