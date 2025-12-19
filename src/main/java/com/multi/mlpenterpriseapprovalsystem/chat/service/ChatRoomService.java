package com.multi.mlpenterpriseapprovalsystem.chat.service;

import com.multi.mlpenterpriseapprovalsystem.chat.domain.ChatRoom;
import com.multi.mlpenterpriseapprovalsystem.chat.domain.ChatRoomMember;
import com.multi.mlpenterpriseapprovalsystem.chat.domain.RoomType;
import com.multi.mlpenterpriseapprovalsystem.chat.dto.ReqChatRoomCreateDto;
import com.multi.mlpenterpriseapprovalsystem.chat.dto.ResChatRoomDto;
import com.multi.mlpenterpriseapprovalsystem.chat.dto.ResChatRoomListDto;
import com.multi.mlpenterpriseapprovalsystem.chat.repository.ChatRoomMemberRepository;
import com.multi.mlpenterpriseapprovalsystem.chat.repository.ChatRoomRepository;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;

import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    /**
     * 채팅방 생성 (1:1 / 그룹)
     */
    public ResChatRoomDto createRoom(ReqChatRoomCreateDto request, String empId) {


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

    /* =======================
       1️⃣ ONE 채팅
     ======================= */
        if (roomType == RoomType.ONE) {

            String targetEmpId = request.getMemberIds().get(0);

            // 기존 1:1 채팅방 있으면 반환
            Optional<ChatRoom> existingRoom =
                    chatRoomRepository.findOneToOneRoom(
                            RoomType.ONE,
                            empId,
                            targetEmpId
                    );

            if (existingRoom.isPresent()) {
                return ResChatRoomDto.from(existingRoom.get());
            }

            Employee target = employeeRepository.findByEmpId(targetEmpId)
                    .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

            roomName = target.getEmpName();
        }

    /* =======================
       2️⃣ GROUP 채팅
     ======================= */
        else {

            // roomName을 안 주면 → 멤버 이름 조합
            if (request.getRoomName() == null || request.getRoomName().isBlank()) {

                List<String> empNames = new ArrayList<>();

                // 나 자신
                Employee me = employeeRepository.findByEmpId(empId)
                        .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));
                empNames.add(me.getEmpName());

                // 초대 멤버들
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

        // 3️⃣ 채팅방 생성
        ChatRoom chatRoom = chatRoomRepository.save(
                ChatRoom.create(roomName, roomType)
        );

        // 4️⃣ 멤버 등록
        addMember(chatRoom, empId);

        for (String id : request.getMemberIds()) {
            addMember(chatRoom, id);
        }

        return ResChatRoomDto.from(chatRoom);
    }

    /**
     * 내 채팅방 목록 조회 (무한 스크롤)
     */
    @Transactional(readOnly = true)
    public List<ResChatRoomListDto> getMyRooms(LocalDateTime cursor, int size, String empId) {


        return chatRoomRepository.findMyRooms(empId, cursor, size)
                .stream()
                .map(room -> ResChatRoomListDto.from(room, empId))
                .collect(Collectors.toList());
    }

    /**
     * 채팅방 상세 조회
     */
    @Transactional(readOnly = true)
    public ResChatRoomDto getRoom(Long roomNo,String empId) {

        ChatRoom chatRoom = chatRoomRepository.findById(roomNo)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND)
                );

        // 🔥 멤버 검증
        boolean isMember = chatRoomMemberRepository
                .existsByChatRoom_RoomNoAndEmployee_EmpId(roomNo, empId);

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
                .orElseThrow(() ->
                        new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND)
                );

        ChatRoomMember member = ChatRoomMember.create(chatRoom, employee);
        chatRoom.addMember(member);
        chatRoomMemberRepository.save(member);
    }

    // 채팅방 읽음 처리
    @Transactional
    public void markAsRead(Long roomNo, String empId) {
        ChatRoomMember member = chatRoomMemberRepository
                .findByChatRoom_RoomNoAndEmployee_EmpId(roomNo, empId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ACCESS_DENIED));

        member.markReadNow(); // lastReadAt=now, unreadCount=0
    }


}