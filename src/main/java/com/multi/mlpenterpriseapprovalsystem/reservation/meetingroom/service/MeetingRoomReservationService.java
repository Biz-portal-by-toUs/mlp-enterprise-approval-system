package com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.service;

import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.domain.MeetingRoomReservation;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.dto.MeetingRoomReservationDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.repository.MeetingRoomReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 회의실 예약 비즈니스 로직을 처리하는 Service 클래스
 *
 * 회의실 예약과 관련된 조회, 생성, 취소 등의
 * 핵심 비즈니스 로직을 담당한다.
 *
 * @author : 송현님
 * @filename : MeetingRoomReservationService
 * @since : 2025-12-22 오후 2:28 월요일
 */

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MeetingRoomReservationService {

    private final MeetingRoomReservationRepository reservationRepository;

    public List<MeetingRoomReservationDto> getReservations(String comId) {

        List<MeetingRoomReservation> reservations =
                reservationRepository.findByCompany_ComIdAndIsDeletedFalse(comId);

        return reservations.stream()
                .map(resv -> new MeetingRoomReservationDto(
                        resv.getMeetingResvNo(),
                        resv.getMeetingRoom().getRoomNo(),
                        resv.getMeetingRoom().getRoomName(),
                        resv.getStartedAt().toLocalDate(),
                        resv.getStartedAt().toLocalTime(),
                        resv.getEndedAt().toLocalTime(),
                        resv.getResvEmp().getEmpId(),
                        resv.getResvEmp().getEmpName()
                ))
                .toList();
    }
}
