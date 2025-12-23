package com.multi.mlpenterpriseapprovalsystem.notice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.company.repository.CompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.notice.domain.Notice;
import com.multi.mlpenterpriseapprovalsystem.notice.dto.NoticeListItemResDto;
import com.multi.mlpenterpriseapprovalsystem.notice.dto.NoticeReqDto;
import com.multi.mlpenterpriseapprovalsystem.notice.dto.NoticeResAllDto;
import com.multi.mlpenterpriseapprovalsystem.notice.repository.NoticeRepository;
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
 * @filename : NoticeService
 * @since : 2025-12-16 화요일
 */
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;

    private static final ObjectMapper om = new ObjectMapper();

    @Transactional
    public void registNotice(NoticeReqDto dto){

        Employee employee = employeeRepository.findByEmpId(dto.getEmpId())
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        Company company = companyRepository.findByComId(dto.getComId())
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

        Notice notice = Notice.builder()
                .company(company)
                .isDeleted(dto.getIsDeleted())
                .title(dto.getTitle())
                .contents(normalizeToJson(dto.getContents()))
               // .contents(dto.getContents())
                .isPopup(dto.getIsPopup())
                .startedAt(dto.getStartedAt())
                .endedAt(dto.getEndedAt())
                .employee(employee)
                .rating(dto.getRating())
                .build();

        noticeRepository.save(notice);
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

    //공지사항 팝업 조회(startedAt , endedAt 사이 팝업 여부가 'Y'인거 검색
    public List<NoticeResAllDto> getAllNotices(String comId) {
        return noticeRepository.findByCompanyAndStatus(comId).stream().map(
                        notice -> NoticeResAllDto.builder()
                                .noticeNo(notice.getNoticeNo())
                                .compId(notice.getCompany().getComId())
                                .isDeleted(notice.getIsDeleted())
                                .title(notice.getTitle())
                                .contents(notice.getContents())
                                .isPopup(notice.getIsPopup())
                                .startedAt(notice.getStartedAt())
                                .endedAt(notice.getEndedAt())
                                .empId(notice.getEmployee().getEmpId())
                                .createdAt(notice.getCreatedAt())
                                .updatedAt(notice.getUpdatedAt())
                                .build())
                .collect(Collectors.toList());

    }

    //회사 구분 없이 조회시 페이징 처리
    public Page<NoticeResAllDto> selectNoticeListWithPagingForAll(Pageable pageable) {
        Page<Notice> notices = noticeRepository.findAll(pageable);

        // 기존 방식
        return notices.map(notice -> NoticeResAllDto.builder()
                .noticeNo(notice.getNoticeNo())
                .compId(notice.getCompany().getComId())
                .isDeleted(notice.getIsDeleted())
                .title(notice.getTitle())
                .contents(notice.getContents())
                .isPopup(notice.getIsPopup())
                .startedAt(notice.getStartedAt())
                .endedAt(notice.getEndedAt())
                .empId(notice.getEmployee().getEmpId())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .build());


    }

    //회사별로 조회시 페이징 처리
    public Page<NoticeResAllDto> selectNoticeListWithPagingForAllByCompany(Pageable pageable, String comId) {
        Page<Notice> notices = noticeRepository.findByCompany_ComIdAndIsDeletedFalse(comId, pageable);

        // 기존 방식
        return notices.map(notice -> NoticeResAllDto.builder()
                .noticeNo(notice.getNoticeNo())
                .compId(notice.getCompany().getComId())
                .isDeleted(notice.getIsDeleted())
                .title(notice.getTitle())
                .contents(notice.getContents())
                .isPopup(notice.getIsPopup())
                .startedAt(notice.getStartedAt())
                .endedAt(notice.getEndedAt())
                .empId(notice.getEmployee().getEmpId())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .build());
    }

    @Transactional
    public void deleteNotice(Long noticeNo) {

        Notice notice = noticeRepository.findById(noticeNo)
                .orElseThrow(() -> new IllegalArgumentException("공지사항 정보가 없습니다")); // 내가 해봄
        noticeRepository.deleteById(noticeNo);
    }

    //공지사항 상세 조회
    @Transactional
    public NoticeResAllDto detailNotice(Long noticeNo) {

        noticeRepository.incrementRating(noticeNo);

        Notice notice = noticeRepository.findById(noticeNo).orElseThrow(() -> new IllegalArgumentException("공지사항이 존재하지 않습니다"));

        return NoticeResAllDto.builder()
                .noticeNo(notice.getNoticeNo())
                .compId(notice.getCompany().getComId())
                .isDeleted(notice.getIsDeleted())
                .title(notice.getTitle())
                .contents(denormalizeFromJson(notice.getContents()))
                .isPopup(notice.getIsPopup())
                .startedAt(notice.getStartedAt())
                .endedAt(notice.getEndedAt())
                .empId(notice.getEmployee().getEmpId())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .rating(notice.getRating())
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

    @Transactional
    public void updateNotice(Long id, NoticeReqDto dto) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("변경할 공지사항이 없습니다"));

        //Contents 데이타를 string -> json 형태로 전환
        String contents = dto.getContents();
        dto.setContents(normalizeToJson(contents));
        System.out.println("dto.getContents() : " + dto.getContents() + "");
        System.out.println("dto : " + dto + "");
        notice.update(dto);
    }

    public Page<NoticeListItemResDto> searchNotices(
            String comId,
            String type,
            String keyword,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    ) {
        Specification<Notice> spec = (root, query, cb) -> cb.conjunction();

        // 회사조건 (Notice.company.comId)
        spec = spec.and((root, query, cb) ->
                cb.equal(root.get("company").get("comId"), comId)
        );

        // 삭제 제외 (isDeleted가 null일 수도 있으니 null OR false)
        spec = spec.and((root, query, cb) ->
                cb.or(cb.isNull(root.get("isDeleted")), cb.isFalse(root.get("isDeleted")))
        );

        if (type != null && !type.isBlank()) {
            switch (type) {
                case "title" -> {
                    if (keyword != null && !keyword.isBlank()) {
                        spec = spec.and((root, query, cb) ->
                                cb.like(root.get("title"), "%" + keyword + "%")
                        );
                    }
                }
                case "writer" -> {
                    if (keyword != null && !keyword.isBlank()) {
                        spec = spec.and((root, query, cb) ->
                                cb.like(root.join("employee", JoinType.LEFT).get("empId"), "%" + keyword + "%")
                        );
                    }
                }
                case "date" -> {
                    // BaseEntity에 createdAt이 있다고 가정 (필드명이 다르면 여기 수정)
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
                }
            }
        }

        Page<Notice> page = noticeRepository.findAll(spec, pageable);

        return page.map(n -> new NoticeListItemResDto(
                n.getNoticeNo(),
                n.getTitle(),
                (n.getEmployee() != null) ? n.getEmployee().getEmpId() : null,
                n.getRating(),
                n.getCreatedAt() // BaseEntity
        ));
    }

}
