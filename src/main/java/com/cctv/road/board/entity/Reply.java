package com.cctv.road.board.entity;

import java.time.LocalDateTime;

import com.cctv.road.member.entity.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "reply")
public class Reply {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer num;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private Member member;

  // ✅ 단일 키 board_num으로 변경
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "board_num", nullable = false)
  private Board board;

  @Column(length = 400, nullable = false)
  private String content;

  @Column(name = "writedate", insertable = false, updatable = false, columnDefinition = "datetime default current_timestamp")
  private LocalDateTime writedate;

  @Column(length = 20)
  private String ipaddr;

  @Column(nullable = false)
  private int gup;

  @Column(nullable = false)
  private int lev = 0;

  @Column(nullable = false)
  private int seq = 0;
}
