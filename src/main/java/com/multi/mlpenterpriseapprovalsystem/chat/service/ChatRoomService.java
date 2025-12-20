package com.multi.mlpenterpriseapprovalsystem.chat.service;

import com.multi.mlpenterpriseapprovalsystem.chat.domain.ChatRoom;
import com.multi.mlpenterpriseapprovalsystem.chat.domain.ChatRoomMember;
import com.multi.mlpenterpriseapprovalsystem.chat.domain.RoomType;
import com.multi.mlpenterpriseapprovalsystem.chat.dto.ReqChatRoomCreateDto;
import com.multi.mlpenterpriseapprovalsystem.chat.dto.ReqChatRoomInviteDto;
import com.multi.mlpenterpriseapprovalsystem.chat.dto.ResChatRoomDto;
import com.multi.mlpenterpriseapprovalsystem.chat.dto.ResChatRoomListDto;
import com.multi.mlpenterpriseapprovalsystem.chat.redis.ChatRedisPublisher;
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

        // 대상 empId 정리 (중복/공백 제거 + 본인 제거)
        Set<String> targets = new LinkedHashSet<>();
        for (String id : request.getMemberIds()) {
            if (id == null || id.isBlank()) continue;
            if (id.equals(empId)) continue;
            targets.add(id);
        }

        if (targets.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_MEMBER_COUNT);
        }

        // ✅ 회사 comId 검증
        for (String targetEmpId : targets) {
            Employee target = employeeRepository.findByEmpId(targetEmpId)
                    .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

            String targetComId = target.getCompany().getComId();
            if (!Objects.equals(creatorComId, targetComId)) {
                throw new CustomException(ErrorCode.COMPANY_MISMATCH);
            }
        }


        int memberCount = request.getMemberIds().size();
        RoomType roomType;

        if (memberCount == 1) {
            roomType = RoomType.ONE;
        } else if (memberCount >= 2) {
            roomType = RoomType.GROUP;
        } else {
            throw new CustomException(ErrorCode.INVALID_MEMBER_COUNT);
        }

        String roomName;


        if (roomType == RoomType.ONE) {

            String targetEmpId = request.getMemberIds().get(0);

            Optional<ChatRoom> existingRoom =
                    chatRoomRepository.findOneToOneRoom(
                            RoomType.ONE,
                            empId,
                            targetEmpId
                    );

            if (existingRoom.isPresent()) {
                ChatRoom room = existingRoom.get();

                chatRoomMemberRepository.reactivateMe(room.getRoomNo(), empId, LocalDateTime.now());

                return ResChatRoomDto.from(room);
            }

            Employee target = employeeRepository.findByEmpId(targetEmpId)
                    .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

            roomName = target.getEmpName();
        }

        else {
            if (request.getRoomName() == null || request.getRoomName().isBlank()) {

                List<String> empNames = new ArrayList<>();

                Employee me = employeeRepository.findByEmpId(empId)
                        .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));
                empNames.add(me.getEmpName());

                for (String id : request.getMemberIds()) {
                    Employee emp = employeeRepository.findByEmpId(id)
                            .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));
                    empNames.add(emp.getEmpName());
                }

                roomName = String.join(", ", empNames);
            } else {
                roomName = request.getRoomName();
            }
        }

        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.create(roomName, roomType));

        addMember(chatRoom, empId);
        for (String id : request.getMemberIds()) {
            addMember(chatRoom, id);
        }

        return ResChatRoomDto.from(chatRoom);
    }

    /**
     * 내 채팅방 목록 조회 (무한 스크롤)
     *
     */
    @Transactional(readOnly = true)
    public List<ResChatRoomListDto> getMyRooms(LocalDateTime cursor, Pageable pageable, String empId) {
        return chatRoomRepository.findMyRooms(empId, cursor, pageable)
                .stream()
                .map(room -> ResChatRoomListDto.from(room, empId))
                .collect(Collectors.toList());
    }

    /**
     * 채팅방 상세 조회 (active=true 멤버만)
     */
    @Transactional(readOnly = true)
    public ResChatRoomDto getRoom(Long roomNo, String empId) {

        ChatRoom chatRoom = chatRoomRepository.findById(roomNo)
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

    /**
     * 읽음 처리 (active=true 멤버만)
     */
    @Transactional
    public void markAsRead(Long roomNo, String empId) {
        ChatRoomMember member = chatRoomMemberRepository
                .findByChatRoom_RoomNoAndEmployee_EmpIdAndIsActiveTrue(roomNo, empId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ACCESS_DENIED));

        member.markReadNow();
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
            redisPublisher.publishSystem(roomNo, leaver.getEmpName() + "님이 나갔습니다.");
        }

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

        // ✅ 중복 초대 방지 (요청에 같은 empId 여러번 들어오는 케이스)
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

        String content = String.join(", ", invitedNames) + "님이 들어왔습니다.";
        redisPublisher.publishSystem(roomNo, content);

    }

}