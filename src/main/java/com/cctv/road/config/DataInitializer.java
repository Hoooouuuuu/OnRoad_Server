package com.cctv.road.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cctv.road.board.entity.BoardCategory;
import com.cctv.road.board.repository.BoardCategoryRepository;

@Configuration
public class DataInitializer {

  @Bean
  public CommandLineRunner initBoardCategories(BoardCategoryRepository repository) {
    return args -> {
      if (repository.count() == 0) {
        BoardCategory category1 = new BoardCategory();
        category1.setName("공지");

        BoardCategory category2 = new BoardCategory();
        category2.setName("자유");

        BoardCategory category3 = new BoardCategory();
        category3.setName("민원");

        repository.save(category1);
        repository.save(category2);
        repository.save(category3);

        System.out.println("✅ 기본 게시판 카테고리 3개 등록 완료!");
      }
    };
  }
}
