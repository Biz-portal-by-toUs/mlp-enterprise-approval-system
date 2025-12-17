package com.multi.mlpenterpriseapprovalsystem.reservation.service;

import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.common.paging.SelectCriteria;
import com.multi.mlpenterpriseapprovalsystem.reservation.dto.ResMeetingRoomDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.register.domain.MeetingRoom;
import com.multi.mlpenterpriseapprovalsystem.reservation.repository.MeetingRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 회의실 관리 도메인의 비즈니스 로직을 담당하는 Service.
 * <p>
 * 회의실 정보의 조회, 등록, 수정, 삭제와 관련된 비즈니스 로직을 처리한다.
 * 데이터 접근은 Repository 계층에 위임하며, 도메인 규칙 및 처리 흐름을 관리한다.
 *
 * @author : 송현님
 * @filename : MeetingRoomService
 * @since : 2025-12-16 오후 2:48 화요일
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MeetingRoomService {

    private final MeetingRoomRepository meetingRoomRepository;

    public Page<ResMeetingRoomDto> selectMeetingRoomsWithPaging(String comId, Pageable pageable) {
    // 반환 타입이 Page<ResMeetingRoomDto>인 이유는 데이터 뿐만 아니라 totalPages, totalElements, first/last 같은 페이지 정보도 같이 주려고
        Page<MeetingRoom> meetingRooms = meetingRoomRepository.findByCompany_ComId(comId, pageable);

        if (meetingRooms.isEmpty()) {
            throw new CustomException(ErrorCode.MEETING_ROOM_NOT_FOUND);
        }

        return meetingRooms.map(meetingRoom -> ResMeetingRoomDto.builder()
                        .roomNo(meetingRoom.getRoomNo())
                        .comId(meetingRoom.getCompany().getComId())
                        .roomName(meetingRoom.getRoomName())
                        .capacity(meetingRoom.getCap())
                        .location(meetingRoom.getLoc())
                        .imageUrl(meetingRoom.getImgUrl())
                        .equipList(meetingRoom.getEquipList())
                        .note(meetingRoom.getNote())
                        .build());
    }
}

