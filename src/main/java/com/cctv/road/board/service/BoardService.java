package com.cctv.road.board.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cctv.road.board.dto.BoardDTO;
import com.cctv.road.board.entity.Board;
import com.cctv.road.board.entity.BoardImage;
import com.cctv.road.board.repository.BoardCategoryRepository;
import com.cctv.road.board.repository.BoardImageRepository;
import com.cctv.road.board.repository.BoardRepository;
import com.cctv.road.member.entity.Member;
import com.cctv.road.member.repository.MemberRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BoardService {

  private final BoardRepository boardRepository;
  private final MemberRepository memberRepository;
  private final BoardCategoryRepository categoryRepository;
  private final ReplyService replyService;
  private final BoardImageRepository boardImageRepository;

  public BoardService(BoardRepository boardRepository,
      MemberRepository memberRepository,
      BoardCategoryRepository categoryRepository,
      ReplyService replyService,
      BoardImageRepository boardImageRepository) {
    this.boardRepository = boardRepository;
    this.memberRepository = memberRepository;
    this.categoryRepository = categoryRepository;
    this.replyService = replyService;
    this.boardImageRepository = boardImageRepository;
  }

  /**
   * 게시글 작성 (이미지 포함)
   */
  @Transactional
  public void writeBoardWithImages(BoardDTO dto, String userId, List<MultipartFile> images) throws IOException {
    int categoryId = dto.isNotice() ? 1 : dto.getCategoryId(); // 공지글이면 categoryId 무조건 1로 설정

    Integer maxSeq = boardRepository.findByCategoryIdOrderByBoardSeqDesc(categoryId)
        .stream()
        .map(Board::getBoardSeq)
        .max(Integer::compareTo)
        .orElse(0);

    Member member = memberRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("사용자 정보를 찾을 수 없습니다."));

    Board board = new Board();
    board.setCategoryId(categoryId); // 여기서 실제 저장되는 카테고리 ID 결정
    board.setBoardSeq(maxSeq + 1);
    board.setSubject(dto.getSubject());
    board.setContent(dto.getContent());
    board.setNotice(dto.isNotice());
    board.setMember(member);
    board.setNickName(member.getNickName());

    Board saved = boardRepository.save(board); // 게시글 저장

    // 이미지 저장
    List<String> imagePaths = new ArrayList<>();
    if (images != null && !images.isEmpty()) {
      for (MultipartFile image : images) {
        if (image != null && !image.isEmpty()) {
          String imagePath = saveImage(image);
          if (imagePath != null && !imagePath.trim().isEmpty()) {
            imagePaths.add(imagePath);

            // 🎯 board_image 테이블에 insert할 BoardImage 객체 생성 및 저장
            BoardImage boardImage = new BoardImage();
            boardImage.setBoard(saved); // 게시글 연결 (saved는 방금 insert한 Board)
            boardImage.setImagePath(imagePath);
            boardImageRepository.save(boardImage);
          }
        }
      }
    }
  }

  // 이미지 저장 메서드 (예시)
  private String saveImage(MultipartFile image) throws IOException {
    if (image == null || image.isEmpty()) {
        log.warn("⚠️ 업로드된 이미지가 비어 있음");
        return null;
    }

    String uploadDir = "C:/project/upload/images";
    File dir = new File(uploadDir);
    if (!dir.exists()) {
        boolean created = dir.mkdirs();
        log.info("📁 디렉토리 생성됨: {} → {}", uploadDir, created);
    }

    String originalFilename = image.getOriginalFilename();
    if (originalFilename == null || !originalFilename.contains(".")) {
        throw new IOException("올바르지 않은 파일 이름입니다.");
    }

    String sanitizedFilename = originalFilename.trim().replaceAll("[^a-zA-Z0-9.-]", "_");
    String extension = sanitizedFilename.substring(sanitizedFilename.lastIndexOf("."));
    String uniqueFileName = UUID.randomUUID().toString() + extension;
    String filePath = uploadDir + "/" + uniqueFileName;

    // ✅ 여기 로그 추가 (실제 저장 경로)
    log.info("💾 이미지 저장 경로: {}", filePath);

    try {
        image.transferTo(new File(filePath));
        log.info("✅ 이미지 저장 완료: {}", filePath);
    } catch (IOException e) {
        log.error("❌ 이미지 저장 실패: {}", e.getMessage());
        throw e;
    }

    return "/images/" + uniqueFileName;
}

  /**
   * 게시글 조회수 증가
   */
  @Transactional
  public void increaseHit(int boardNum) {
    Board board = boardRepository.findByBoardNum(boardNum)
        .orElseThrow(() -> new RuntimeException("게시글이 존재하지 않습니다."));
    board.setHit(board.getHit() + 1);
  }

  /**
   * 게시글 조회 - boardNum 기준
   */
  public BoardDTO getBoard(int boardNum) {
    Board board = boardRepository.findByBoardNum(boardNum)
        .orElseThrow(() -> new RuntimeException("게시글이 존재하지 않습니다."));
    return BoardDTO.fromEntity(board);
  }

  /**
   * 게시글 조회 - categoryId + boardSeq 기준
   */
  public BoardDTO getBoardBySeq(int categoryId, int boardSeq) {
    Board board = boardRepository.findByCategoryIdAndBoardSeq(categoryId, boardSeq)
        .orElseThrow(() -> new RuntimeException("해당 게시글이 존재하지 않습니다."));
    return BoardDTO.fromEntity(board);
  }

  /**
   * 게시글 수정
   */
  @Transactional
  public void updatePostBySeq(int categoryId, int boardSeq, BoardDTO dto, List<Long> deleteImages, List<MultipartFile> newImages) throws IOException {
    Board board = boardRepository.findByCategoryIdAndBoardSeq(categoryId, boardSeq)
        .orElseThrow(() -> new RuntimeException("해당 게시글이 존재하지 않습니다."));

    boolean isNotice = dto.isNotice();

    // 공지글 전환 시 카테고리 및 boardSeq 변경 필요
    if (isNotice && board.getCategoryId() != 1) {
      // 기존 boardSeq 충돌 방지: 공지용 seq 새로 부여
      int maxNoticeSeq = boardRepository.findByCategoryIdOrderByBoardSeqDesc(1)
          .stream()
          .map(Board::getBoardSeq)
          .max(Integer::compareTo)
          .orElse(0);

      board.setCategoryId(1);
      board.setBoardSeq(maxNoticeSeq + 1);
    }

    board.setSubject(dto.getSubject());
    board.setContent(dto.getContent());
    board.setNotice(isNotice);

    // 기존 이미지 삭제
    if (deleteImages != null) {
      for (Long imageId : deleteImages) {
          BoardImage image = boardImageRepository.findById(imageId)
              .orElseThrow(() -> new RuntimeException("이미지 없음"));

          // 1. DB 관계에서 제거
          board.getImages().remove(image);
          boardImageRepository.delete(image);

          // 2. 파일 삭제 - BoardService 내부에서 직접
          String path = image.getImagePath();
          File file = new File("/your/upload/path", path.substring(path.lastIndexOf("/") + 1));
          if (file.exists()) file.delete();
      }
  }

  // 새 이미지 추가
  if (newImages != null) {
      for (MultipartFile file : newImages) {
          String savedPath = saveImage(file); // 아래 메서드 참고
          BoardImage newImage = new BoardImage();
          newImage.setImagePath(savedPath);
          newImage.setBoard(board);
          boardImageRepository.save(newImage);
      }
  }
  }

  /**
   * 공지사항 목록 조회 (카테고리 관계없이 모든 공지)
   */
  public List<BoardDTO> getNoticeList() {
    return boardRepository.findByNoticeTrueOrderByBoardSeqDesc()
        .stream()
        .map(BoardDTO::fromEntity)
        .collect(Collectors.toList());
  }

  /**
   * 게시글 페이징 목록
   */
  public Page<BoardDTO> getPagedPosts(int categoryId, Pageable pageable) {
    return boardRepository.findNonNoticeByCategoryIdWithMember(categoryId, pageable).map(BoardDTO::fromEntity);
}

  /**
   * 카테고리별 게시글 총 개수
   */
  public int getTotalCountByCategory(int categoryId) {
    return boardRepository.countByCategoryId(categoryId);
  }

  /**
   * 게시글 + 댓글 삭제
   */
  @Transactional
  public void deletePostWithReplies(int boardNum) {
    Board board = boardRepository.findByBoardNum(boardNum)
        .orElseThrow(() -> new RuntimeException("게시글이 존재하지 않습니다."));

    int categoryId = board.getCategoryId(); // 🧠 삭제 전에 카테고리 확인

    replyService.deleteRepliesByBoard(board); // 댓글 먼저 삭제
    boardRepository.delete(board); // 게시글 삭제

    reorderBoardSeq(categoryId); // ✅ 순번 재정렬
  }

  /**
   * 게시글 순번 재정렬
   */
  @Transactional
  public void reorderBoardSeq(int categoryId) {
    List<Board> boards = boardRepository.findByCategoryIdOrderByBoardSeqAsc(categoryId);
    int seq = 1;
    for (Board board : boards) {
      board.setBoardSeq(seq++);
    }
    boardRepository.saveAll(boards); // 🔥 이 방식이 성능에 더 좋아!
  }

}
