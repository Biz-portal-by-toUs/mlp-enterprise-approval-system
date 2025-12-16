package com.multi.mlpenterpriseapprovalsystem.reservation.service;

import com.multi.mlpenterpriseapprovalsystem.reservation.dto.MeetingRoomResponseDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.register.domain.MeetingRoom;
import com.multi.mlpenterpriseapprovalsystem.reservation.repository.MeetingRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Please explain the class!!!
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
@Transactional
public class MeetingRoomService {

    private final MeetingRoomRepository meetingRoomRepository;

    public List<MeetingRoom> getMeetingRooms(String comId) {
//        String comId = SecurityUtil.getComId();  // JWT에서 추출
        return meetingRoomRepository.findByCompany_ComId(comId);
    }
}
