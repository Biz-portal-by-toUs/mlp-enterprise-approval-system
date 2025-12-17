package com.multi.mlpenterpriseapprovalsystem.reservation.controller;

import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.dto.ReqMeetingRoomDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.dto.ResMeetingRoomDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.service.MeetingRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 회의실 관리 기능에 대한 REST API 요청을 처리하는 Controller.
 *
 * 회의실 관련 데이터 조회 요청을 처리한다.
 * Service 계층을 통해 비즈니스 로직을 수행한다.
 * 처리 결과를 JSON 형태의 회의실 DTO 목록으로 반환한다.
 *
 * ※ 현재 comId는 임시로 RequestParam에서 전달받으며,
 * JWT 인증 연동 후 Access Token에서 추출하도록 변경 예정이다.
 *
 * @author : 송현님
 * @filename : MeetingRoomController
 * @since : 2025-12-16 오후 2:47 화요일
 */

@RestController
@Slf4j
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MeetingRoomController {

    private final MeetingRoomService meetingRoomService;

    @GetMapping("/meeting-rooms")
    public ResponseEntity<ResponseDto<Page<ResMeetingRoomDto>>> getMeetingRoomsWithPaging(@RequestParam String comId,
                                                                 @RequestParam(name = "page", defaultValue = "0") int page,
                                                                 @RequestParam(name = "size", defaultValue = "6") int size) {  // 한 페이지에서 보여줄 데이터 개수

        Pageable pageable = PageRequest.of(page, size, Sort.by("roomName").ascending());

        Page<ResMeetingRoomDto> meetingRooms = meetingRoomService.selectMeetingRoomsWithPaging(comId, pageable);
        String msg = meetingRooms.isEmpty() ? "등록된 회의실이 없습니다." : "회의실 조회 성공";
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, msg, meetingRooms));
    }

    @PostMapping(value ="/meeting-rooms", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<Long>> registerMeetingRoom(@RequestParam String comId,  //임시로 @RequestAttribute("comId")
                                                                 @ModelAttribute ReqMeetingRoomDto meetingRoomDto,
                                                                 @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) throws IOException {

        Long roomNo = meetingRoomService.registerMeetingRoom(comId, meetingRoomDto, imageFile);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto<>(HttpStatus.CREATED, "회의실 등록 성공", roomNo));
    }
}
