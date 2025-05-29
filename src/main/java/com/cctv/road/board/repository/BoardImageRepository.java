package com.cctv.road.board.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cctv.road.board.entity.BoardImage;

public interface BoardImageRepository extends JpaRepository<BoardImage, Long> {

    // 게시글 번호(board_num)로 이미지 목록을 조회
    List<BoardImage> findByBoard_BoardNum(Long boardNum);
    
}
