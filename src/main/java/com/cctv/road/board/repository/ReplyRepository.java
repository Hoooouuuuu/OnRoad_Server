package com.cctv.road.board.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cctv.road.board.entity.Board;
import com.cctv.road.board.entity.Reply;

public interface ReplyRepository extends JpaRepository<Reply, Integer> {

  void deleteByBoard(Board board);

  // 특정 게시글에 달린 모든 댓글/대댓글 조회 (gup, lev, seq로 정렬)
  List<Reply> findByBoardOrderByGupAscSeqAsc(Board board);

  // 댓글 목록 (카테고리 ID + 게시글 순번 기준, gup 오름차순, seq 오름차순 정렬)
  List<Reply> findByBoard_CategoryIdAndBoard_BoardSeqOrderByGupAscSeqAsc(int categoryId, int boardSeq);

  List<Reply> findByBoard_BoardNumOrderByGupAscSeqAsc(Integer boardNum);
}
