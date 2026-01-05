package com.multi.mlpenterpriseapprovalsystem.meeting.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.meeting.domain.MeetingScope;
import com.multi.mlpenterpriseapprovalsystem.meeting.dto.*;
import com.multi.mlpenterpriseapprovalsystem.meeting.service.MeetingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 회의 등록, 수정, 삭제 컨트롤러
 *
 * @author : 김승기
 * @filename : MeetingController
 * @since : 2025. 12. 22. 월요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meetings")
@Slf4j
public class MeetingController {

    private final MeetingService meetingService;

    @Value("${internal.ai.callback-key}")
    private String internalCallbackKey;

    // 회의 목록 조회 (탭 + 제목검색 + 날짜검색(StartedAt 기준) + (전체탭)부서검색)
    @GetMapping
    public ResponseEntity<ResponseDto<ResMeetingListDto>> meeting(
            @RequestParam(name="scope",defaultValue = "ALL") MeetingScope scope, // ALL | MY_DEPT | MY
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "depNo", required = false) Long depNo, // 전체회의 탭에서 부서별 검색용(서비스에서 scope에 따라 적용/무시)
            @RequestParam(name = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 10, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();

        ResMeetingListDto res = meetingService.getMeetingList(
                empId, scope, keyword, depNo, fromDate, toDate, pageable
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "회의 목록 조회 성공", res));
    }

    @GetMapping("/trash")
    public ResponseEntity<ResponseDto<ResMeetingListDto>> deletedMeetingList(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 10, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUser user
    ) {
        // 현재 로그인한 사용자의 ID 추출
        String empId = user.getUsername();

        // 서비스에서 삭제된 회의 목록만 가져오는 메서드 호출
        ResMeetingListDto res = meetingService.getDeletedMeetingList(
                empId, keyword, fromDate, toDate, pageable
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "휴지통 목록 조회 성공", res));
    }

    // 회의 상세 조회
    @GetMapping("/{meetNo}")
    public ResponseEntity<ResponseDto<ResMeetingDetailDto>> meetingDetail(
            @PathVariable(name = "meetNo") Long meetNo,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        ResMeetingDetailDto res = meetingService.getMeetingDetail(empId, meetNo);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "회의 상세 조회 성공", res));
    }

    // 회의 등록
    @PostMapping
    public ResponseEntity<ResponseDto<Map<String,Long>>> createMeeting(
            @RequestBody @Valid ReqMeetingCreateDto request,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        Long meetNo = meetingService.createMeeting(empId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto<>(HttpStatus.CREATED, "회의 등록 성공", Map.of("meetNo", meetNo)));
    }

    // 회의 수정
    @PutMapping("/{meetNo}")
    public ResponseEntity<ResponseDto<Long>> updateMeeting(
            @PathVariable(name = "meetNo") Long meetNo,
            @RequestBody @Valid ReqMeetingUpdateDto request,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        Long updatedMeetNo = meetingService.updateMeeting(empId, meetNo, request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "회의 수정 성공", updatedMeetNo));
    }

    // 회의 삭제(소프트 삭제)
    @DeleteMapping("/{meetNo}")
    public ResponseEntity<ResponseDto<Long>> deleteMeeting(
            @PathVariable(name = "meetNo") Long meetNo,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        Long deletedMeetNo = meetingService.deleteMeeting(empId, meetNo);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "회의 삭제 성공", deletedMeetNo));
    }

    // ✅ (추가) 회의 영구 삭제 (물리 삭제)
    @DeleteMapping("/{meetNo}/hard")
    public ResponseEntity<ResponseDto<Long>> hardDeleteMeeting(
            @PathVariable(name = "meetNo") Long meetNo,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();

        // 서비스에서 실제 DB 삭제 로직 호출
        Long deletedMeetNo = meetingService.hardDeleteMeeting(empId, meetNo);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "회의 영구 삭제 성공", deletedMeetNo));
    }

    // ✅ (추가) 휴지통 회의 복구 (isDeleted = false로 변경)
    @PatchMapping("/{meetNo}/restore")
    public ResponseEntity<ResponseDto<Long>> restoreMeeting(
            @PathVariable(name = "meetNo") Long meetNo,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();

        // 서비스에서 복구 로직 호출
        Long restoredMeetNo = meetingService.restoreMeeting(empId, meetNo);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "회의 복구 성공", restoredMeetNo));
    }


    // =========================================================
    // ✅ (추가1) 프론트 -> AI 처리 요청
    // =========================================================
    @PostMapping("/{meetNo}/ai-request")
    public ResponseEntity<ResponseDto<Long>> requestAi(
            @PathVariable(name = "meetNo") Long meetNo,
            @RequestBody @Valid ReqMeetingAiRequestDto request,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();

        meetingService.requestAiPipeline(empId, meetNo, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "AI 처리 요청 성공", meetNo));
    }

    // =========================================================
    // ✅ (추가2) FastAPI -> AI 결과 콜백(서버간)
    // =========================================================
    @PatchMapping("/{meetNo}/ai")
    public ResponseEntity<ResponseDto<Long>> aiCallback(
            @PathVariable(name = "meetNo") Long meetNo,
            @RequestBody @Valid ReqMeetingAiCallbackDto request,
            @RequestHeader(name = "X-Internal-Callback-Key") String callbackKey
    ) {
        // meetNo 일치 확인
        if (request.getMeetNo() == null || !request.getMeetNo().equals(meetNo)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST); // 없으면 만들어서 써
        }

        // 내부 콜백키 검증
        String expected = internalCallbackKey; // 아래 필드 추가 필요
        if (expected == null || !expected.equals(callbackKey)) {
            throw new CustomException(ErrorCode.FORBIDDEN); // 없으면 만들어서 써
        }

        Long updatedMeetNo = meetingService.applyAiResult(meetNo, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "AI 결과 반영 성공", updatedMeetNo));
    }
}