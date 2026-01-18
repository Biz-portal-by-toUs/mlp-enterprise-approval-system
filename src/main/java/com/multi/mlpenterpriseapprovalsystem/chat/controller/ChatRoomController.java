package com.multi.mlpenterpriseapprovalsystem.chat.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.chat.dto.ReqChatRoomCreateDto;
import com.multi.mlpenterpriseapprovalsystem.chat.dto.ReqChatRoomInviteDto;
import com.multi.mlpenterpriseapprovalsystem.chat.dto.ResChatRoomDto;
import com.multi.mlpenterpriseapprovalsystem.chat.dto.ResChatRoomListDto;
import com.multi.mlpenterpriseapprovalsystem.chat.service.ChatRoomService;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    /**
     * 채팅방 생성 (1:1 / 그룹)
     * - 1:1 기존 방이 있으면 그 방 반환
     * - 만약 내가 1:1을 나가서(active=false) 숨김 상태였다면, 여기서 다시 active=true로 복귀시켜줌(joinedAt=now)
     */
    @PostMapping
    public ResponseEntity<ResponseDto<ResChatRoomDto>> createRoom(
            @RequestBody ReqChatRoomCreateDto request,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        ResChatRoomDto room = chatRoomService.createRoom(request, empId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto<>(HttpStatus.CREATED, "채팅방 생성 성공", room));
    }

    /**
     * 내 채팅방 목록 (채팅 탭)
     * - active=true인 방만 내려주는 걸 전제로 함(Repo 쿼리에서 active 조건 처리 권장)
     */
    @GetMapping("/my")
    public ResponseEntity<ResponseDto<List<ResChatRoomListDto>>> getMyRooms(
            @RequestParam(name = "keyword", required = false) String keyword, // ✅ 검색어 추가
            @RequestParam(name = "lastMessageAt", required = false) LocalDateTime cursor,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();

        // ✅ 서비스 호출 시 keyword도 같이 넘겨줍니다.
        List<ResChatRoomListDto> rooms = chatRoomService.getMyRooms(keyword, cursor, pageable, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "채팅방 목록 조회 성공", rooms));
    }

    /**
     * 채팅방 상세 조회
     * - active=true 멤버만 접근 가능
     */
    @GetMapping("/{roomNo}")
    public ResponseEntity<ResponseDto<ResChatRoomDto>> getRoom(
            @PathVariable(name = "roomNo") Long roomNo,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        ResChatRoomDto room = chatRoomService.getRoom(roomNo, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "채팅방 상세 조회 성공", room));
    }

    /**
     * 읽음 처리
     * - active=true 멤버만 가능
     */
    @PostMapping("/{roomNo}/read")
    public ResponseEntity<ResponseDto<Void>> readRoom(
            @PathVariable(name = "roomNo") Long roomNo,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        chatRoomService.markAsRead(roomNo, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "읽음 처리 성공", null)
        );
    }

    /**
     * 채팅방 나가기
     * - 1:1: active=false(숨김 효과)
     * - 단톡: active=false(진짜 퇴장)
     * - 차이는 "다시 active=true로 만드는 트리거"가 다름
     *   * 1:1: 상대 메시지 오면(ChatMessageService에서) 자동 복귀 가능
     *   * 단톡: 초대(Invite)에서만 복귀
     */
    @PostMapping("/{roomNo}/leave")
    public ResponseEntity<ResponseDto<Void>> leaveRoom(
            @PathVariable(name = "roomNo") Long roomNo,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        chatRoomService.leaveRoom(roomNo, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "채팅방 나가기 성공", null)
        );
    }

    @PostMapping("/{roomNo}/invite")
    public ResponseEntity<ResponseDto<Void>> inviteMembers(
            @PathVariable(name = "roomNo") Long roomNo,
            @RequestBody ReqChatRoomInviteDto request,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        chatRoomService.inviteMembers(roomNo, request, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "초대 성공", null)
        );
    }

    /**
     * 위젯용: 현재 로그인한 사용자의 전체 채팅 안 읽은 메시지 수 조회
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ResponseDto<Long>> getUnreadCount(@AuthenticationPrincipal CustomUser user) {
        String empId = user.getUsername();
        long count = chatRoomService.getTotalUnreadCount(empId);

        return ResponseEntity.ok(
                new ResponseDto<>(HttpStatus.OK, "전체 안 읽은 채팅 개수 조회 성공", count)
        );
    }
}
