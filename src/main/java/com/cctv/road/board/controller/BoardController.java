package com.cctv.road.board.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cctv.road.board.dto.BoardDTO;
import com.cctv.road.board.entity.Reply;
import com.cctv.road.board.service.BoardService;
import com.cctv.road.board.service.ReplyService;
import com.cctv.road.member.entity.Member;
import com.cctv.road.member.security.CustomUserDetails;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/board")
public class BoardController {

  private final BoardService boardService;
  private final ReplyService replyService;

  public BoardController(BoardService boardService, ReplyService replyService) {
    this.boardService = boardService;
    this.replyService = replyService;
  }

  @GetMapping("/write")
  public String writeForm(@RequestParam("categoryId") int categoryId,
      @RequestParam(name = "page", defaultValue = "1") int page,
      Model model) {
    BoardDTO boardDTO = new BoardDTO();
    boardDTO.setCategoryId(categoryId);
    model.addAttribute("categoryId", categoryId);
    model.addAttribute("boardDTO", boardDTO);
    model.addAttribute("pageNum", page);
    return "board/write";
  }

  @PostMapping("/write")
  public String writeSubmit(@ModelAttribute BoardDTO boardDTO,
      @RequestParam("images") List<MultipartFile> images,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    log.info("🔥 글쓰기 요청 받음 - notice: {}", boardDTO.isNotice());

    if (userDetails == null) {
      return "redirect:/login";
    }

    Member member = userDetails.getMember();
    boardDTO.setNickName(member.getNickName());

    try {
      boardService.writeBoardWithImages(boardDTO, member.getUserId(), images);
    } catch (Exception e) {
      log.error("게시글 작성 중 오류 발생", e);
      return "error";
    }

    return "redirect:/board/list?categoryId=" + boardDTO.getCategoryId();
  }

  @GetMapping("/list")
  public String list(@RequestParam(name = "categoryId", required = false) int categoryId,
      @RequestParam(name = "page", defaultValue = "1") int page, // 사용자 기준 1부터 시작
      RedirectAttributes redirectAttributes,
      Model model) {

    // ✅ categoryId가 2나 3이 아니면 기본값을 자유게시판(2)로 설정
    if (categoryId != 2 && categoryId != 3) {
      categoryId = 2;
    }

    int totalCount = boardService.getTotalCountByCategory(categoryId);
    int totalPages = (int) Math.ceil((double) totalCount / 10); // 페이지 크기 10 기준

    // ✅ 유효하지 않은 페이지 요청 시 마지막 페이지로 리디렉션
    if (page > totalPages && totalPages > 0) {
      redirectAttributes.addAttribute("categoryId", categoryId);
      redirectAttributes.addAttribute("page", totalPages);
      return "redirect:/board/list";
    }

    int pageIndex = Math.max(page - 1, 0); // 내부 Pageable은 0부터 시작
    Pageable pageable = PageRequest.of(pageIndex, 10, Sort.by(Sort.Direction.DESC, "boardSeq"));

    List<BoardDTO> notices = boardService.getNoticeList();
    Page<BoardDTO> postPage = boardService.getPagedPosts(categoryId, pageable);

    String categoryName = switch (categoryId) {
      case 2 -> "자유게시판";
      case 3 -> "민원게시판";
      default -> "자유게시판";
    };

    model.addAttribute("notices", notices);
    model.addAttribute("posts", postPage.getContent());
    model.addAttribute("page", postPage);
    model.addAttribute("pageNum", page);
    model.addAttribute("categoryId", categoryId);
    model.addAttribute("categoryName", categoryName);
    model.addAttribute("totalCount", totalCount);

    return "board/list";
  }

  @GetMapping("/view/{boardNum}")
  public String viewPost(@PathVariable("boardNum") Integer boardNum,
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "originCategoryId", required = false) Integer originCategoryId,
      Model model,
      Authentication authentication) {

    boardService.increaseHit(boardNum);
    BoardDTO post = boardService.getBoard(boardNum);

    log.info("🔍 이미지 개수: {}", post.getImageFileNames().size());
    post.getImageFileNames().forEach(image -> log.info("📷 {}", image));

    List<Reply> replies = replyService.getReplies(post.getBoardNum());

    if (originCategoryId == null) {
      originCategoryId = post.getCategoryId();
    }

    // ✅ 작성자 본인 여부 및 관리자 권한 확인
    boolean isOwner = false;
    boolean isAdmin = false;

    if (authentication != null && authentication.isAuthenticated()) {
      String loginUsername = authentication.getName(); // 기본 UserDetails.getUsername()

      isOwner = loginUsername.equals(post.getUserId()); // 또는 post.getMember().getUsername()
      isAdmin = authentication.getAuthorities().stream()
          .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }

    model.addAttribute("post", post);
    model.addAttribute("pageNum", page);
    model.addAttribute("originCategoryId", originCategoryId);
    model.addAttribute("replies", replies);
    model.addAttribute("canModify", isOwner || isAdmin); // 👈 View 조건 판단용

    return "board/view";
  }

  @GetMapping("/delete/{boardNum}")
  @Transactional
  public String deletePost(@PathVariable("boardNum") int boardNum,
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "originCategoryId", required = false) Integer originCategoryId,
      RedirectAttributes redirectAttributes) {

    BoardDTO post = boardService.getBoard(boardNum);
    boardService.deletePostWithReplies(boardNum);

    // ✅ originCategoryId fallback 처리
    if (originCategoryId == null) {
      originCategoryId = post.getCategoryId();
    }

    redirectAttributes.addAttribute("categoryId", originCategoryId);
    redirectAttributes.addAttribute("page", page);
    return "redirect:/board/list";
  }

  @GetMapping("/update/{categoryId}/{boardSeq}")
  public String showUpdateForm(@PathVariable("categoryId") int categoryId,
      @PathVariable("boardSeq") int boardSeq,
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "originCategoryId", required = false) Integer originCategoryId,
      Model model) {

    BoardDTO post = boardService.getBoardBySeq(categoryId, boardSeq);
    if (post == null) {
      throw new IllegalArgumentException("해당 게시글이 존재하지 않습니다.");
    }

    // ✅ 여기가 핵심: originCategoryId가 null이면 현재 게시글의 원래 카테고리로 대체
    if (originCategoryId == null) {
      originCategoryId = post.getCategoryId();
    }

    model.addAttribute("post", post);
    model.addAttribute("pageNum", page);
    model.addAttribute("originCategoryId", originCategoryId); // ✅ 반드시 있어야 함

    return "board/update";
  }

  @PostMapping("/update/{categoryId}/{boardSeq}")
  public String updatePost(@PathVariable("categoryId") int categoryId,
      @PathVariable("boardSeq") int boardSeq,
      @ModelAttribute BoardDTO boardDTO,
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "originCategoryId", required = false) Integer originCategoryId,
      @RequestParam(value = "deleteImages", required = false) List<Long> deleteImages,
      @RequestParam(value = "newImages", required = false) List<MultipartFile> newImages,
      RedirectAttributes redirectAttributes) {

    // ✅ originCategoryId가 null이면 fallback
    if (originCategoryId == null) {
      originCategoryId = boardDTO.getCategoryId(); // 또는 categoryId로 대체 가능
    }

    log.info("🔄 수정 요청 받음 - notice: {}", boardDTO.isNotice());
    log.info("📦 originCategoryId: {}", originCategoryId);

    try {
      boardService.updatePostBySeq(categoryId, boardSeq, boardDTO, deleteImages, newImages);
    } catch (IOException e) {
      log.error("파일 저장 중 오류 발생: {}", e.getMessage());
      redirectAttributes.addFlashAttribute("error", "파일 업로드 중 오류가 발생했습니다.");
      return "redirect:/board/update/" + categoryId + "/" + boardSeq;
    }

    BoardDTO updated = boardService.getBoardBySeq(categoryId, boardSeq);

    // ✅ 목록 이동에 필요한 정보 유지
    redirectAttributes.addAttribute("page", page);
    redirectAttributes.addAttribute("originCategoryId", originCategoryId);

    return "redirect:/board/view/" + updated.getBoardNum();
  }

}
