package com.multi.mlpenterpriseapprovalsystem.reservation.service;

import com.multi.mlpenterpriseapprovalsystem.reservation.dto.ResMeetingRoomDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.repository.MeetingRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 회의실 관리 도메인의 비즈니스 로직을 담당하는 Service.
 *
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

    public List<ResMeetingRoomDto> getMeetingRooms(String comId) {
        return meetingRoomRepository.findByCompany_ComId(comId).stream()
                .map(room -> ResMeetingRoomDto.builder()
                        .roomNo(room.getRoomNo())
                        .comId(room.getCompany().getComId())
                        .roomName(room.getRoomName())
                        .capacity(room.getCap())
                        .location(room.getLoc())
                        .imageUrl(room.getImgUrl())
                        .equipList(room.getEquipList())
                        .note(room.getNote())
                        .build()
                )
                .toList();
    }

}
