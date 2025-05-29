package com.cctv.road.board.dto;

import java.time.LocalDateTime;

import com.cctv.road.board.entity.Reply;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ReplyDTO {
  private Integer num;
  private String userId;
  private String nickName;
  private Integer boardNum; // ✅ 추가됨
  private String content;
  private LocalDateTime writedate;
  private String ipaddr;
  private Integer gup;
  private Integer lev;
  private Integer seq;

  public static ReplyDTO fromEntity(Reply reply) {
    return ReplyDTO.builder()
        .num(reply.getNum())
        .userId(reply.getMember() != null ? reply.getMember().getUserId() : null)
        .nickName(reply.getMember() != null ? reply.getMember().getNickName() : null)
        .boardNum(reply.getBoard().getBoardNum())
        .content(reply.getContent())
        .writedate(reply.getWritedate())
        .ipaddr(reply.getIpaddr())
        .gup(reply.getGup())
        .lev(reply.getLev())
        .seq(reply.getSeq())
        .build();
  }
}
