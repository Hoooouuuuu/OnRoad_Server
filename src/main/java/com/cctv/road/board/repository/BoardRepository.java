package com.cctv.road.board.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cctv.road.board.entity.Board;

public interface BoardRepository extends JpaRepository<Board, Integer> {

  // 📋 카테고리 기준 내림차순 정렬 (일반글)
  List<Board> findByCategoryIdOrderByBoardSeqDesc(int categoryId);

  // 📋 카테고리 기준 오름차순 정렬 (재정렬용)
  List<Board> findByCategoryIdOrderByBoardSeqAsc(int categoryId);

  // 📊 카테고리별 게시글 수
  int countByCategoryId(int categoryId);

  // 👤 Member 포함 조회 (닉네임 출력용)
  // 👇 공지 아닌 글만 가져오기
  @Query("SELECT b FROM Board b JOIN FETCH b.member WHERE b.categoryId = :categoryId AND b.notice = false ORDER BY b.boardSeq DESC")
  Page<Board> findNonNoticeByCategoryIdWithMember(@Param("categoryId") int categoryId, Pageable pageable);

  // 🔍 boardNum 기준 단일 조회
  Optional<Board> findByBoardNum(Integer boardNum);

  // 🔍 카테고리 + 글순번 기준 조회
  Optional<Board> findByCategoryIdAndBoardSeq(int categoryId, int boardSeq);

  // 🆕 공지글만 필터링 (카테고리 = 1, 공지 = true)
  List<Board> findByNoticeTrueOrderByBoardSeqDesc();

  // ❌ 게시글 삭제
  void deleteByBoardNum(Integer boardNum);
}
