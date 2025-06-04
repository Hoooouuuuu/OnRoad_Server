package com.cctv.road.board.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cctv.road.board.dto.ReplyDTO;
import com.cctv.road.board.entity.Board;
import com.cctv.road.board.entity.Reply;
import com.cctv.road.board.repository.BoardRepository;
import com.cctv.road.board.repository.ReplyRepository;
import com.cctv.road.member.entity.Member;
import com.cctv.road.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReplyService {

  private final ReplyRepository replyRepository;
  private final MemberRepository memberRepository;
  private final BoardRepository boardRepository;

  /**
   * ✅ 댓글 작성
   */
  @Transactional
  public void writeReply(ReplyDTO dto, String userId, String ip) {
    Member member = memberRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

    Board board = boardRepository.findByBoardNum(dto.getBoardNum())
        .orElseThrow(() -> new IllegalArgumentException("게시글 정보를 찾을 수 없습니다."));

    Reply reply = new Reply();
    reply.setMember(member);
    reply.setBoard(board);
    reply.setContent(dto.getContent());
    reply.setIpaddr(ip);
    reply.setWritedate(LocalDateTime.now());

    // 기본값 설정 (일반 댓글)
    reply.setLev(0); // 댓글 단계
    reply.setSeq(0); // 그룹 내 순서
    reply.setGup(0); // 그룹 번호 (댓글 PK 기준)

    // 1차 저장 → ID(PK) 생성됨
    reply = replyRepository.save(reply);

    // 그룹 번호는 본인의 PK 값으로 설정
    reply.setGup(reply.getNum());
    replyRepository.save(reply); // 2차 저장
  }

  /**
   * ✅ 특정 게시글의 댓글 목록 조회
   */
  @Transactional(readOnly = true)
  public List<Reply> getReplies(int boardNum) {
    return replyRepository.findByBoard_BoardNumOrderByGupAscSeqAsc(boardNum);
  }

  @Transactional
public void deleteRepliesByBoard(Board board) {
  replyRepository.deleteByBoard(board);
}
}
