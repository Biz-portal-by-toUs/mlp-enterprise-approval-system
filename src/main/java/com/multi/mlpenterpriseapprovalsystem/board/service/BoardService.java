package com.multi.mlpenterpriseapprovalsystem.board.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.multi.mlpenterpriseapprovalsystem.board.domain.Board;
import com.multi.mlpenterpriseapprovalsystem.board.domain.BoardCat;
import com.multi.mlpenterpriseapprovalsystem.board.dto.*;
import com.multi.mlpenterpriseapprovalsystem.board.repository.BoardCatRepository;
import com.multi.mlpenterpriseapprovalsystem.board.repository.BoardRepository;
import com.multi.mlpenterpriseapprovalsystem.board.repository.CommentRepository;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.company.repository.CompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Please explain the class!!!
 *
 * @author : kim youngkwan
 * @filename : BoardService
 * @since : 2025-12-18 목요일
 */
@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardCatRepository boardCatRepository;
    private final CommentRepository commentRepository;
    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;

    private static final ObjectMapper om = new ObjectMapper();

    public List<BoardCatDto> getAllCategory() {

        // 기존 방식
        return boardCatRepository.findAll().stream().map(
                        boardCat -> BoardCatDto.builder()
                                .boardCatNo(boardCat.getBoardCatNo())
                                .catCode(boardCat.getCatCode())
                                .catDescript(boardCat.getCatDescript())
                                .build())
                .collect(Collectors.toList());


    }

    public Page<BoardResAllDto> selectBoardListWithPagingForAll(Pageable pageable) {
        Page<Board> boards = boardRepository.findAll(pageable);

        // 기존 방식
        return boards.map( board -> BoardResAllDto.builder()
                .boardNo(board.getBoardNo())
                .compId(board.getCompany().getComId())
                .isDeleted(board.getIsDeleted())
                .title(board.getTitle())
                .contents(board.getContents())
                .catCode(board.getCatCode())
                .rating(board.getRating())
                .empId(board.getEmployee().getEmpId())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .build());

    }

    //회사별로 조회시 페이징 처리
    public Page<BoardResAllDto> selectBoardListWithPagingForAllByCompany(Pageable pageable, String comId) {
        Page<Board> boards = boardRepository.findByCompany_ComIdAndIsDeletedFalse(comId, pageable);

        // 기존 방식
        return boards.map( board -> BoardResAllDto.builder()
                .boardNo(board.getBoardNo())
                .compId(board.getCompany().getComId())
                .isDeleted(board.getIsDeleted())
                .title(board.getTitle())
                .contents(board.getContents())
                .catCode(board.getCatCode())
                .rating(board.getRating())
                .empId(board.getEmployee().getEmpId())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .build());
    }

    //특정 게시판에 달린 댓글 전체 조회
    public List<CommentDto> getCommentByBoardNo(Long boardNo) {
        Board board = boardRepository.findById(boardNo)
                .orElseThrow(() -> new IllegalArgumentException("게시판이 존재하지 않습니다"));

        return commentRepository.findByBoard_BoardNo(boardNo).stream()
                .map(c -> CommentDto.builder()
                        .commentNo(c.getCommentNo())
                        .comId(c.getCompany().getComId())
                        .boardNo(c.getBoard().getBoardNo())
                        .contents(c.getContents())
                        .empId(c.getEmployee().getEmpId())
                        .empName(c.getEmployee().getEmpName())
                        .depName(c.getEmployee().getDepartment().getDepName())
                        .createdAt(c.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

    }

    //특정게시판 삭제
    @Transactional
    public void deleteBoard(Long boardNo) {

        Board board = boardRepository.findById(boardNo)
                .orElseThrow(() -> new IllegalArgumentException("게시판 정보가 없습니다")); // 내가 해봄
        boardRepository.deleteById(boardNo);
    }

    @Transactional
    public void updateBoard(Long boardNo, BoardReqDto dto) {
        Board board = boardRepository.findById(boardNo)
                .orElseThrow(() -> new IllegalArgumentException("게시판 정보가 없습니다"));

        board.update(dto);
    }

    @Transactional
    public BoardResAllDto registBoard(BoardReqDto dto) {

        Employee employee = employeeRepository.findByEmpId(dto.getEmpId())
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        Company company = companyRepository.findByComId(dto.getComId())
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));


        Board board = Board.builder()
                .company(company)
                .isDeleted(dto.getIsDeleted())
                .title(dto.getTitle())
                .contents(normalizeToJson(dto.getContents()))
                .catCode(dto.getCatCode())
                .employee(employee)
                .rating(dto.getRating())
                .build();

        Board regiBoard = boardRepository.save(board);

        return BoardResAllDto.builder()
                .boardNo(regiBoard.getBoardNo())
                .compId(regiBoard.getCompany().getComId())
                .isDeleted(regiBoard.getIsDeleted())
                .title(regiBoard.getTitle())
                .contents(denormalizeFromJson(regiBoard.getContents()))
                .catCode(regiBoard.getCatCode())
                .empId(regiBoard.getEmployee().getEmpId())
                .createdAt(regiBoard.getCreatedAt())
                .updatedAt(regiBoard.getUpdatedAt())
                .rating(regiBoard.getRating())
                .build();
    }

    private String normalizeToJson(String raw) {
        if (raw == null) return null;

        try {
            om.readTree(raw);     // 이미 JSON이면 그대로
            return raw;
        } catch (Exception ignore) {
        }

        ObjectNode node = om.createObjectNode();
        node.put("text", raw);
        return node.toString();
    }

    //자유게시판 상세 조회
    @Transactional
    public BoardResAllDto detailBoard(Long boardNo) {

        boardRepository.incrementRating(boardNo);

        Board board = boardRepository.findById(boardNo)
                      .orElseThrow(() -> new IllegalArgumentException("자유게시판이 존재하지 않습니다"));
        BoardCat boardCat = boardCatRepository.findByCatCode(board.getCatCode())
                       .orElseThrow(() -> new IllegalArgumentException("카테고리가 존재하지 않습니다"));
        return BoardResAllDto.builder()
                .boardNo(board.getBoardNo())
                .compId(board.getCompany().getComId())
                .isDeleted(board.getIsDeleted())
                .title(board.getTitle())
                .contents(denormalizeFromJson(board.getContents()))
                .catCode(board.getCatCode())
                .catDescript(boardCat.getCatDescript())
                .empId(board.getEmployee().getEmpId())
                .empName(board.getEmployee().getEmpName())
                .depName(board.getEmployee().getDepartment().getDepName())
                .createdAt(board.getCreatedAt())
                .rating(board.getRating())
                .build();

    }

    private String denormalizeFromJson(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }

        try {
            JsonNode node = om.readTree(json);

            // normalizeToJson에서 감싼 {"text": "..."} 인 경우
            if (node.isObject() && node.size() == 1 && node.has("text")) {
                return node.get("text").asText();
            }

            // 그 외(JSON Object/Array)는 그대로 문자열로 반환
            return node.toString();

        } catch (Exception e) {
            // JSON 파싱 실패 = 이미 그냥 문자열일 가능성
            return json;
        }
    }

    //===================================================================

    public Page<BoardListItemResDto> searchBoards(
            String comId,
            String type,
            String keyword,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    ) {
        Specification<Board> spec = (root, query, cb) -> cb.conjunction();

        // ✅ 회사조건 (Notice.company.comId)
        spec = spec.and((root, query, cb) ->
                cb.equal(root.get("company").get("comId"), comId)
        );

        // ✅ 삭제 제외 (isDeleted null OR false)
        spec = spec.and((root, query, cb) ->
                cb.or(cb.isNull(root.get("isDeleted")), cb.isFalse(root.get("isDeleted")))
        );

        // ✅ type/keyword normalize
        String t = (type == null) ? "" : type.trim();
        String k = (keyword == null) ? "" : keyword.trim();

        // ✅ 조건별 검색
        if ("date".equals(t)) {
            // 날짜 범위는 기존 로직 유지(끝은 다음날 0시 미만)
            LocalDateTime fromDt = (from != null) ? from.atStartOfDay() : null;
            LocalDateTime toExclusive = (to != null) ? to.plusDays(1).atStartOfDay() : null;

            if (fromDt != null) {
                spec = spec.and((root, query, cb) ->
                        cb.greaterThanOrEqualTo(root.get("createdAt"), fromDt)
                );
            }
            if (toExclusive != null) {
                spec = spec.and((root, query, cb) ->
                        cb.lessThan(root.get("createdAt"), toExclusive)
                );
            }

        } else {
            // date가 아닌 경우: keyword 기반 검색
            if (!k.isBlank()) {
                switch (t) {
                    case "" -> {
                        // ✅ 전체: title + contents (OR)
                        spec = spec.and((root, query, cb) -> cb.or(
                                cb.like(root.get("title"), "%" + k + "%"),
                                cb.like(root.get("contents"), "%" + k + "%")
                        ));
                    }
                    case "title" -> {
                        spec = spec.and((root, query, cb) ->
                                cb.like(root.get("title"), "%" + k + "%")
                        );
                    }
                    case "empName" -> {
                        spec = spec.and((root, query, cb) ->
                                cb.like(root.join("employee", JoinType.LEFT).get("empName"), "%" + k + "%")
                        );
                    }
                    case "depName" -> {
                        spec = spec.and((root, query, cb) -> {
                            Join<Object, Object> emp = root.join("employee", JoinType.LEFT);
                            Join<Object, Object> dep = emp.join("department", JoinType.LEFT);
                            return cb.like(dep.get("depName"), "%" + k + "%");
                        });
                    }
                    default -> {
                        // 알 수 없는 type이면 전체검색으로
                        spec = spec.and((root, query, cb) -> cb.or(
                                cb.like(root.get("title"), "%" + k + "%"),
                                cb.like(root.get("contents"), "%" + k + "%")
                        ));
                    }
                }
            }
        }

        // ✅ N+1 방지: employee/department fetch join (count 쿼리 방해 방지)
        Specification<Board> fetchSpec = (root, query, cb) -> {
            // count 쿼리에는 fetch join 하면 안됨
            if (!Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                root.fetch("employee", JoinType.LEFT).fetch("department", JoinType.LEFT);
                query.distinct(true);
            }
            return cb.conjunction();
        };

        Page<Board> page = boardRepository.findAll(spec.and(fetchSpec), pageable);

        return page.map(n -> new BoardListItemResDto(
                n.getBoardNo(),
                n.getTitle(),
                (n.getEmployee() != null) ? n.getEmployee().getEmpId() : null, // ✅ DTO 생성자 유지 때문에 남김(화면에 안 쓰면 됨)
                (n.getEmployee() != null) ? n.getEmployee().getEmpName() : null,
                (n.getEmployee() != null && n.getEmployee().getDepartment() != null)
                        ? n.getEmployee().getDepartment().getDepName()
                        : null,
                n.getRating(),
                n.getCreatedAt()
        ));
    }




}
