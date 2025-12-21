package com.multi.mlpenterpriseapprovalsystem.notice.service;

import com.multi.mlpenterpriseapprovalsystem.notice.domain.Notice;
import com.multi.mlpenterpriseapprovalsystem.notice.dto.NoticeReqDto;
import com.multi.mlpenterpriseapprovalsystem.notice.dto.NoticeResAllDto;
import com.multi.mlpenterpriseapprovalsystem.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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


    @Transactional
    public void registNotice(NoticeReqDto dto){

        Notice notice = Notice.builder()
            //    .company(company)
                .isDeleted(dto.getIsDeleted())
                .title(dto.getTitle())
                .contents(dto.getTitle())
                .isPopup(dto.getIsPopup())
                .startedAt(dto.getStartedAt())
                .endedAt(dto.getEndedAt())
             //   .employee(employee)
                .build();

        noticeRepository.save(notice);
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
    public void deleteNotice(Long id) {

        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공지사항 정보가 없습니다")); // 내가 해봄
        noticeRepository.deleteById(id);
    }

    //공지사항 상세 조회
    public NoticeResAllDto detailNotice(Long id) {

        Notice notice = noticeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("공지사항이 존재하지 않습니다"));

        return NoticeResAllDto.builder()
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
                .build();

    }

    @Transactional
    public void updateNotice(Long id, NoticeReqDto dto) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("변경할 공지사항이 없습니다"));

        notice.update(dto);
    }


}
