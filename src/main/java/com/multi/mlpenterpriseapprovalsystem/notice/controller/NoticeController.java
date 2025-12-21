package com.multi.mlpenterpriseapprovalsystem.notice.controller;

import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.notice.dto.NoticeReqDto;
import com.multi.mlpenterpriseapprovalsystem.notice.dto.NoticeResAllDto;
import com.multi.mlpenterpriseapprovalsystem.notice.service.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Please explain the class!!!
 *
 * @author : kim youngkwan
 * @filename : NoticeController
 * @since : 2025-12-16 화요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class NoticeController {

    private final NoticeService noticeService;

    //공지사항 등록
    @PostMapping("/notice")
    public ResponseEntity<String> registNotice(@RequestBody @Valid NoticeReqDto dto){
        noticeService.registNotice(dto);
        return ResponseEntity.ok("공지사항이 등록되었습니다.");
    }

    //회사별 전체 조회 (페이징 처리 없음
    @GetMapping("/notices-popup")
    public ResponseEntity<ResponseDto<List<NoticeResAllDto>>> getAll() {
        String comId = "A05";
        return ResponseEntity.ok(new ResponseDto<List<NoticeResAllDto>>(HttpStatus.OK, "공지 사항 팝업 조회 성공", noticeService.getAllNotices(comId)));
    }

    // 회사별 페이징 처리 전체 조회
    @GetMapping("/notices-all")
    public ResponseEntity<ResponseDto<Page<NoticeResAllDto>>> getNoticesForAll(@RequestParam(name = "page", defaultValue = "0") int page,
                                                        @RequestParam(name = "size", defaultValue = "10") int size) {
        System.out.println("page : " + page + ", size : " + size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("noticeNo").descending());
        Page<NoticeResAllDto> notices = noticeService.selectNoticeListWithPagingForAll(pageable);

        return ResponseEntity.ok().body(new ResponseDto<Page<NoticeResAllDto>>(HttpStatus.OK, "조회 성공", notices));
    }

    //회사별 페이징처리
    @GetMapping("/notices-all/{comid}")
    public ResponseEntity<ResponseDto<Page<NoticeResAllDto>>> getNoticesForAllByCompany(@RequestParam(name = "page", defaultValue = "0") int page,
                                                        @RequestParam(name = "size", defaultValue = "10") int size,
                                                        @PathVariable("comid") String comId  ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("noticeNo").descending());
        Page<NoticeResAllDto> notices = noticeService.selectNoticeListWithPagingForAllByCompany(pageable,comId);

        return ResponseEntity.ok().body(new ResponseDto<Page<NoticeResAllDto>>(HttpStatus.OK, "조회 성공", notices));
    }

    //공지사항 삭제
    @DeleteMapping("/notices/{id}")
    public ResponseEntity<String> delete(@PathVariable("id") Long id) {
        noticeService.deleteNotice(id);
        return ResponseEntity.ok("공지사항이 삭제되었습니다.");
    }

    //공지사항 상세조회  --- 일련번호(key로 조회)
    @GetMapping("/notice/{id}")
    public ResponseEntity<ResponseDto<NoticeResAllDto>> detail(@PathVariable("id") Long id) {
        return ResponseEntity.ok().body(new ResponseDto<NoticeResAllDto>(HttpStatus.OK, "조회 성공", noticeService.detailNotice(id)));
    }

    //공지사항 수정  --- 일련번호(key로 수정)
    @PutMapping("/notice/{id}")
    public ResponseEntity<String> update(@PathVariable("id") Long id, @RequestBody @Valid NoticeReqDto dto) {
        noticeService.updateNotice(id, dto);
        return ResponseEntity.ok("공지사항이 수정되었습니다.");
    }



}
