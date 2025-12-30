package com.multi.mlpenterpriseapprovalsystem.chat.service;

import com.multi.mlpenterpriseapprovalsystem.chat.domain.*;
import com.multi.mlpenterpriseapprovalsystem.chat.dto.*;
import com.multi.mlpenterpriseapprovalsystem.chat.redis.ChatRedisPublisher;
import com.multi.mlpenterpriseapprovalsystem.chat.repository.ChatMessageRepository;
import com.multi.mlpenterpriseapprovalsystem.chat.repository.ChatRoomMemberRepository;
import com.multi.mlpenterpriseapprovalsystem.chat.repository.ChatRoomRepository;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 채팅방 서비스
 *
 * @author : 김승기
 * @filename : ChatRoomService
 * @since : 2025. 12. 17. 수요일
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final EmployeeRepository employeeRepository;
    private final ChatRedisPublisher redisPublisher;

    /**
     * 채팅방 생성 (1:1 / 그룹)
     */
    public ResChatRoomDto createRoom(ReqChatRoomCreateDto request, String empId) {
        Employee creator = employeeRepository.findByEmpId(empId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        String creatorComId = creator.getCompany().getComId();

        if (request.getMemberIds() == null) {
            throw new CustomException(ErrorCode.INVALID_MEMBER_COUNT);
        }

        Set<String> targets = new LinkedHashSet<>();
        for (String id : request.getMemberIds()) {
            if (id == null || id.isBlank()) continue;
            if (id.equals(empId)) continue;
            targets.add(id);
        }

        if (targets.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_MEMBER_COUNT);
        }

        String targetNameForOneToOne = null;

        for (String targetEmpId : targets) {
            Employee target = employeeRepository.findByEmpId(targetEmpId)
                    .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

            if (targets.size() == 1) {
                targetNameForOneToOne = target.getEmpName();
            }

            String targetComId = target.getCompany().getComId();
            if (!Objects.equals(creatorComId, targetComId)) {
                throw new CustomException(ErrorCode.COMPANY_MISMATCH);
            }
        }

        int memberCount = targets.size();
        RoomType roomType = (memberCount == 1) ? RoomType.ONE : RoomType.GROUP;

        if (roomType == RoomType.ONE) {
            String targetEmpId = targets.iterator().next();

            Optional<ChatRoom> existingRoom = chatRoomRepository.findOneToOneRoom(RoomType.ONE, empId, targetEmpId);

            if (existingRoom.isPresent()) {
                ChatRoom room = existingRoom.get();
                chatRoomMemberRepository.reactivateMe(room.getRoomNo(), empId, LocalDateTime.now());

                ResChatRoomDto res = ResChatRoomDto.from(room);
                res.setRoomName(targetNameForOneToOne);
                return res;
            }
        }

        String roomName = null;
        if (roomType == RoomType.GROUP) {
            if (request.getRoomName() == null || request.getRoomName().isBlank()) {
                List<String> empNames = new ArrayList<>();
                empNames.add(creator.getEmpName());
                for (String id : targets) {
                    if (empNames.size() >= 5) break;
                    employeeRepository.findByEmpId(id).ifPresent(e -> empNames.add(e.getEmpName()));
                }
                roomName = String.join(", ", empNames);
                if (roomName.length() > 45) roomName = roomName.substring(0, 45) + "...";
            } else {
                roomName = request.getRoomName();
                if (roomName.length() > 50) throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }

        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.create(roomName, roomType));

        addMember(chatRoom, empId);
        for (String id : targets) {
            addMember(chatRoom, id);
        }

        ResChatRoomDto result = ResChatRoomDto.from(chatRoom);
        if (roomType == RoomType.ONE) {
            result.setRoomName(targetNameForOneToOne);
        }

        return result;
    }

    /**
     * 내 채팅방 목록 조회 (무한 스크롤)
     *
     */
    @Transactional(readOnly = true)
    public List<ResChatRoomListDto> getMyRooms(String keyword, LocalDateTime cursor, Pageable pageable, String empId) {
        return chatRoomRepository.findMyRooms(empId, keyword, cursor, pageable)
                .stream()
                .map(room -> ResChatRoomListDto.from(room, empId))
                .collect(Collectors.toList());
    }

    /**
     * 채팅방 상세 조회 (active=true 멤버만)
     */
    @Transactional(readOnly = true)
    public ResChatRoomDto getRoom(Long roomNo, String empId) {

        ChatRoom chatRoom = chatRoomRepository.findByIdWithActiveMembers(roomNo)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        boolean isMember = chatRoomMemberRepository
                .existsByChatRoom_RoomNoAndEmployee_EmpIdAndIsActiveTrue(roomNo, empId);

        if (!isMember) {
            throw new CustomException(ErrorCode.CHAT_ACCESS_DENIED);
        }

        return ResChatRoomDto.from(chatRoom);
    }

    /**
     * 채팅방 멤버 추가
     */
    private void addMember(ChatRoom chatRoom, String empId) {
        Employee employee = employeeRepository.findByEmpId(empId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        ChatRoomMember member = ChatRoomMember.create(chatRoom, employee);
        chatRoom.addMember(member);
        chatRoomMemberRepository.save(member);
    }


    @Transactional
    public void markAsRead(Long roomNo, String empId) {
        ChatRoomMember member = chatRoomMemberRepository
                .findByChatRoom_RoomNoAndEmployee_EmpIdAndIsActiveTrue(roomNo, empId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ACCESS_DENIED));

        member.markReadNow();

        ChatRoom room = member.getChatRoom();
        String roomName;

        if (room.getRoomType() == RoomType.ONE) {
            roomName = room.getMembers().stream()
                    .filter(m -> !m.getEmployee().getEmpId().equals(empId)) // 내가 아닌 멤버 찾기
                    .map(m -> m.getEmployee().getEmpName())
                    .findFirst()
                    .orElse("알 수 없는 사용자");
        } else {
            roomName = room.getRoomName() != null ? room.getRoomName() : "그룹 채팅";
        }

        // 2. DB에서 최신 메시지 조회 (목록 갱신용)
        ChatMessage latestMsg = chatMessageRepository.findTopByRoomNoOrderByCreatedAtDesc(roomNo)
                .orElse(null);

        String content = (latestMsg != null) ? latestMsg.getContent() : null;
        String createdAt = (latestMsg != null) ? latestMsg.getCreatedAt().toString() : null;

        int activeMemberCount = (int) room.getMembers().stream()
                .filter(ChatRoomMember::isActive)
                .count();

        // 3. 최신 데이터(상대방 이름 포함)로 목록 업데이트 알림 전송
        ResChatRoomUpdateDto updateDto = new ResChatRoomUpdateDto(
                roomNo,
                content,
                createdAt,
                0,
                roomName
                ,false
                ,room.getRoomType()
                ,activeMemberCount
        );

        redisPublisher.publishRoomUpdate(empId, updateDto);
    }


    /**
     * 시스템 메시지(입장/퇴장 등)를 DB에 저장하고, Redis(STOMP)로도 발행
     */
    private void saveAndPublishSystemMessage(Long roomNo, String content) {
        // ✅ DB 저장
        ChatMessage systemMessage = ChatMessage.builder()
                .roomNo(roomNo)
                .senderEmpId("SYSTEM")
                .senderName("SYSTEM")
                .content(content)
                .type(MessageType.SYSTEM)
                .createdAt(LocalDateTime.now())
                .build();
        chatMessageRepository.save(systemMessage);

        redisPublisher.publishSystem(roomNo, content);
    }

    /**
     * 채팅방 나가기
     * - 1:1 / 단톡 공통으로 active=false 처리
     * - 1:1은 상대 메시지 오면(ChatMessageService에서) 자동 복귀 가능
     * - 단톡은 초대(Invite)에서만 복귀시키는 걸 권장
     */
    @Transactional
    public void leaveRoom(Long roomNo, String empId) {

        ChatRoom room = chatRoomRepository.findById(roomNo)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        ChatRoomMember member = chatRoomMemberRepository
                .findByChatRoom_RoomNoAndEmployee_EmpIdAndIsActiveTrue(roomNo, empId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ACCESS_DENIED));

        Employee leaver = employeeRepository.findByEmpId(empId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        member.markReadNow();
        member.deactivateNow();

        if (RoomType.GROUP.equals(member.getChatRoom().getRoomType())) {
            saveAndPublishSystemMessage(roomNo, leaver.getEmpName() + "님이 나갔습니다.");
        }
        ResChatRoomUpdateDto dto = new ResChatRoomUpdateDto(roomNo, null, null, 0, null,true,RoomType.ONE,0);
        redisPublisher.publishRoomUpdate(empId, dto);

        log.info("[LEAVE] roomNo={}, empId={}, type={}", roomNo, empId, room.getRoomType());
    }

    @Transactional
    public void inviteMembers(Long roomNo, ReqChatRoomInviteDto request, String inviterEmpId) {

        ChatRoom room = chatRoomRepository.findById(roomNo)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (room.getRoomType() != RoomType.GROUP) {
            throw new CustomException(ErrorCode.INVALID_ROOM_TYPE);
        }

        boolean inviterIsMember = chatRoomMemberRepository
                .existsByChatRoom_RoomNoAndEmployee_EmpIdAndIsActiveTrue(roomNo, inviterEmpId);

        if (!inviterIsMember) {
            throw new CustomException(ErrorCode.CHAT_ACCESS_DENIED);
        }

        if (request.getMemberIds() == null || request.getMemberIds().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_MEMBER_COUNT);
        }

        Employee inviter = employeeRepository.findByEmpId(inviterEmpId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        String inviterComId = inviter.getCompany().getComId();

        LocalDateTime now = LocalDateTime.now();

        List<String> invitedNames = new ArrayList<>();
        List<String> alreadyInRoomNames = new ArrayList<>();

        Set<String> uniqueTargetIds = new LinkedHashSet<>(request.getMemberIds());

        for (String targetEmpId : uniqueTargetIds) {

            if (targetEmpId == null || targetEmpId.isBlank()) continue;
            if (targetEmpId.equals(inviterEmpId)) continue;

            Employee target = employeeRepository.findByEmpId(targetEmpId)
                    .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

            String targetComId = target.getCompany().getComId();
            if (!Objects.equals(inviterComId, targetComId)) {
                throw new CustomException(ErrorCode.COMPANY_MISMATCH);
            }

            ChatRoomMember existing = chatRoomMemberRepository
                    .findByChatRoom_RoomNoAndEmployee_EmpId(roomNo, targetEmpId)
                    .orElse(null);

            if (existing == null) {
                ChatRoomMember newMember = ChatRoomMember.create(room, target);
                room.addMember(newMember);
                chatRoomMemberRepository.save(newMember);
                invitedNames.add(target.getEmpName());
            } else {
                if (existing.isActive()) {
                    alreadyInRoomNames.add(target.getEmpName());
                    continue;
                }

                existing.reactivateNow(now);
                invitedNames.add(target.getEmpName());
            }
        }

        if (invitedNames.isEmpty()) {
            return;
        }
        chatRoomMemberRepository.flush();

        String content = String.join(", ", invitedNames) + "님이 들어왔습니다.";
        saveAndPublishSystemMessage(roomNo, content);


    }

}