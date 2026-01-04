package com.multi.mlpenterpriseapprovalsystem.notice.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.notice.dto.NoticeListItemResDto;
import com.multi.mlpenterpriseapprovalsystem.notice.dto.NoticeReqDto;
import com.multi.mlpenterpriseapprovalsystem.notice.dto.NoticeResAllDto;
import com.multi.mlpenterpriseapprovalsystem.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
    @PreAuthorize("hasRole('COM_ADMIN')")
    @PostMapping(value = "/notice", consumes = {"multipart/form-data"})
    public ResponseEntity<ResponseDto<NoticeResAllDto>> registNotice(@ModelAttribute NoticeReqDto dto, @AuthenticationPrincipal CustomUser customUser){
        dto.setComId(customUser.getComId());
        dto.setEmpId(customUser.getUsername());
        dto.setRating(0);
        dto.setIsDeleted(false);
        System.out.println("dto : " + dto);
       // noticeService.registNotice(dto);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<NoticeResAllDto>(HttpStatus.OK, "공지사항 등록 성공", noticeService.registNotice(dto)));
    }

    //회사별 전체 조회 (페이징 처리 없음
    @GetMapping("/notices-popup")
    public ResponseEntity<ResponseDto<List<NoticeResAllDto>>> getAll(@AuthenticationPrincipal CustomUser customUser) {
        String comId = customUser.getComId();
        return ResponseEntity.ok(new ResponseDto<List<NoticeResAllDto>>(HttpStatus.OK, "공지 사항 팝업 조회 성공", noticeService.getAllNotices(comId)));
    }

    // 회사별 페이징 처리 전체 조회
//    @GetMapping("/notices-all")
//    public ResponseEntity<ResponseDto<Page<NoticeResAllDto>>> getNoticesForAll(@RequestParam(name = "page", defaultValue = "0") int page,
//                                                        @RequestParam(name = "size", defaultValue = "10") int size) {
//        System.out.println("page : " + page + ", size : " + size);
//        Pageable pageable = PageRequest.of(page, size, Sort.by("noticeNo").descending());
//        Page<NoticeResAllDto> notices = noticeService.selectNoticeListWithPagingForAll(pageable);
//
//        return ResponseEntity.ok().body(new ResponseDto<Page<NoticeResAllDto>>(HttpStatus.OK, "조회 성공", notices));
//    }

    //회사별 페이징처리--원본
//    @GetMapping("/notices-all")
//    public ResponseEntity<ResponseDto<Page<NoticeResAllDto>>> getNoticesForAllByCompany(@RequestParam(name = "page", defaultValue = "0") int page,
//                                                        @RequestParam(name = "size", defaultValue = "10") int size
//                                                          ) {
//        String comId = "A005";
//        Pageable pageable = PageRequest.of(page, size, Sort.by("noticeNo").descending());
//        Page<NoticeResAllDto> notices = noticeService.selectNoticeListWithPagingForAllByCompany(pageable,comId);
//
//        return ResponseEntity.ok().body(new ResponseDto<Page<NoticeResAllDto>>(HttpStatus.OK, "조회 성공", notices));
//    }



    @GetMapping("/notices-all")
    public ResponseEntity<ResponseDto<Page<NoticeResAllDto>>> getNoticesForAllByCompany(@AuthenticationPrincipal CustomUser customUser,
                                                                                        @RequestParam(name = "page", defaultValue = "0") int page,
                                                                                        @RequestParam(name = "size", defaultValue = "10") int size
                                                                                       ) {
        String comId = customUser.getComId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("noticeNo").descending());
        Page<NoticeResAllDto> notices = noticeService.selectNoticeListWithPagingForAllByCompany(pageable,comId);

        return ResponseEntity.ok().body(new ResponseDto<Page<NoticeResAllDto>>(HttpStatus.OK, "조회 성공", notices));
    }

    //공지사항 삭제
    @PreAuthorize("hasRole('COM_ADMIN')")
    @DeleteMapping("/notice/{noticeNo}")
    public ResponseEntity<ResponseDto> delete(@PathVariable(name="noticeNo") Long noticeNo) {
        noticeService.deleteNotice(noticeNo);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto(HttpStatus.OK, "공지사항 삭제 성공", null));
    }

    //공지사항 상세조회  --- 일련번호(key로 조회)
    @GetMapping("/notice/{noticeNo}")
    public ResponseEntity<ResponseDto<NoticeResAllDto>> detail(@PathVariable(name="noticeNo") Long noticeNo) {
        return ResponseEntity.ok().body(new ResponseDto<NoticeResAllDto>(HttpStatus.OK, "조회 성공", noticeService.detailNotice(noticeNo)));
    }

    //공지사항 수정  --- 일련번호(key로 수정)  consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    @PreAuthorize("hasRole('COM_ADMIN')")
    @PutMapping(value = "/notice/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<ResponseDto> update(@PathVariable(name="id") Long id,
                                              @ModelAttribute NoticeReqDto dto,
                                              @AuthenticationPrincipal CustomUser customUser  ) {
        dto.setComId(customUser.getComId());
        dto.setEmpId(customUser.getUsername());
        dto.setIsDeleted(false);
        noticeService.updateNotice(id, dto);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto(HttpStatus.OK, "공지사항 수정 성공", null));
    }

    // 예) /api/v1/notices?type=title&keyword=공지&page=0&size=10
    // 예) /api/v1/notices?type=writer&keyword=EMP0001&page=0&size=10
    // 예) /api/v1/notices?type=date&from=2025-12-01&to=2025-12-21&page=0&size=10
    // 예) /api/v1/notices?page=0&size=10  (전체)
    @GetMapping("/notices")
    public ResponseEntity<ResponseDto<Page<NoticeListItemResDto>>> search(@AuthenticationPrincipal CustomUser customUser,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String comId = customUser.getComId();
        System.out.println("====== Controller =====");
        System.out.println("type : " + type + ", keyword : " + keyword + ", from : " + from + ", to : " + to);
        System.out.println("page : " + page + ", size : " + size);
        System.out.println("comId : " + comId);

        if (type != null) type = type.trim();
        if (keyword != null) keyword = keyword.trim();
        Pageable pageable = PageRequest.of(page, size, Sort.by("noticeNo").descending());

        Page<NoticeListItemResDto> result = noticeService.searchNotices(comId, type, keyword, from, to, pageable);
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "공지사항 조회 성공", result));
    }


}
