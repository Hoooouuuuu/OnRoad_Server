package com.cctv.road.board.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.cctv.road.member.entity.Member;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "board")
public class Board {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "board_num")
  private Integer boardNum;

  @Column(name = "board_seq", nullable = false)
  private Integer boardSeq;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private Member member;

  @Column(name = "nick_name")
  private String nickName;

  @Column(name = "category_id", nullable = false)
  private Integer categoryId;

  @Column(nullable = false, length = 100)
  private String subject;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String content;

  @Column(nullable = false, columnDefinition = "int default 0", insertable = false)
  private Integer hit;

  @Column(name = "writedate", insertable = false, updatable = false, columnDefinition = "datetime default current_timestamp")
  private LocalDateTime writedate;

  // ✅ 공지 여부 (boolean)
  @Column(name = "notice", nullable = false)
  private boolean notice = false;

  // 이미지 연관관계 (게시글 1:N 이미지)
  @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<BoardImage> images;
}
