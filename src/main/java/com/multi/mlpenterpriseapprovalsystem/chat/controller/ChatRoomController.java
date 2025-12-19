package com.multi.mlpenterpriseapprovalsystem.chat.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.chat.dto.ReqChatRoomCreateDto;
import com.multi.mlpenterpriseapprovalsystem.chat.dto.ResChatRoomDto;
import com.multi.mlpenterpriseapprovalsystem.chat.dto.ResChatRoomListDto;
import com.multi.mlpenterpriseapprovalsystem.chat.service.ChatRoomService;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;


/**
 * 채팅방 컨트롤러 (생성, 채팅방 목록 조회, 상세 조회)
 *
 * @author : 김승기
 * @filename : ChatController
 * @since : 2025. 12. 17. 수요일
 */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatRoomController {
    private final ChatRoomService chatRoomService;

    @PostMapping
    public ResponseEntity<ResponseDto<ResChatRoomDto>> createRoom(
            @RequestBody ReqChatRoomCreateDto request, @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        ResChatRoomDto room = chatRoomService.createRoom(request, empId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto<>(HttpStatus.CREATED, "채팅방 생성 성공", room));

    }

    @GetMapping("/my")
    public ResponseEntity<ResponseDto<List<ResChatRoomListDto>>> getMyRooms(
            @RequestParam(required = false) LocalDateTime cursor,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        List<ResChatRoomListDto> resChatRoomListDtos = chatRoomService.getMyRooms(cursor, size, empId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "채팅방 목록 조회 성공", resChatRoomListDtos));
    }

    @GetMapping("/{roomNo}")
    public ResponseEntity<ResponseDto<ResChatRoomDto>> getRoom(
            @PathVariable Long roomNo,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        ResChatRoomDto resChatRoomDto = chatRoomService.getRoom(roomNo, empId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "채팅방 상세 조회 성공", resChatRoomDto));
    }

    @PostMapping("/{roomNo}/read")
    public ResponseEntity<ResponseDto<Void>> readRoom(
            @PathVariable Long roomNo,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        chatRoomService.markAsRead(roomNo, empId);

        return ResponseEntity.ok(new ResponseDto<>(
                HttpStatus.OK, "읽음 처리 성공", null
        ));
    }
}
