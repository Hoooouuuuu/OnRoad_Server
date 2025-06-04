package com.cctv.road.board.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cctv.road.board.entity.BoardCategory;

public interface BoardCategoryRepository extends JpaRepository<BoardCategory, Integer> {

}
