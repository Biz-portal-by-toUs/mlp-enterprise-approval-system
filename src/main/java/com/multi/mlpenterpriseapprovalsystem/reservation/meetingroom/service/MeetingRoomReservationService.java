package com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.notification.domain.NotificationType;
import com.multi.mlpenterpriseapprovalsystem.notification.service.NotificationService;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.domain.MeetingRoom;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.domain.MeetingRoomAttendee;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.domain.MeetingRoomReservation;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.dto.ReqReservationCreateDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.dto.ResReservationListDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.repository.MeetingRoomAttendeeRepository;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.repository.MeetingRoomRepository;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.repository.MeetingRoomReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final MeetingRoomRepository meetingRoomRepository;
    private final EmployeeRepository employeeRepository;
    private final MeetingRoomAttendeeRepository meetingRoomAttendeeRepository;
    private final NotificationService notificationService;

    public List<ResReservationListDto> getReservations(String comId, String date) {

        List<MeetingRoomReservation> reservations =
                reservationRepository.findByCompany_ComIdAndIsDeletedFalse(comId);

        // 날짜 필터링
        if (date != null) {
            reservations = reservations.stream()
                    .filter(r -> r.getStartedAt().toLocalDate().toString().equals(date))
                    .toList();
        }

        return reservations.stream()
                .map(resv -> new ResReservationListDto(
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

    public void createReservation(ReqReservationCreateDto dto, CustomUser user) {
        // 시작/종료 시간 만들기
        LocalDateTime startedAt =
                LocalDateTime.of(dto.getResvDate(), dto.getStartTime());
        LocalDateTime endedAt =
                LocalDateTime.of(dto.getResvDate(), dto.getEndTime());

        // 지난 시간 예약 차단
        if (startedAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("지난 시간은 예약할 수 없습니다.");
        }

        // 시간 유효성
        if (!endedAt.isAfter(startedAt)) {
            throw new IllegalArgumentException("종료 시간은 시작 시간 이후여야 합니다.");
        }

        // 회의실 존재 확인
        MeetingRoom room = meetingRoomRepository.findById(dto.getRoomNo())
                .orElseThrow(() -> new IllegalArgumentException("회의실이 존재하지 않습니다."));

        // 예약자 조회 (Employee)
        Employee employee = employeeRepository.findByEmpId(user.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("예약자 정보가 없습니다."));

        // 중복 예약 검사
        if (reservationRepository.existsOverlapping(dto.getRoomNo(), startedAt, endedAt)) {
            throw new IllegalArgumentException("이미 예약된 시간입니다.");
        }

        MeetingRoomReservation reservation =
                MeetingRoomReservation.builder()
                        .company(room.getCompany())
                        .meetingRoom(room)
                        .resvEmp(employee)
                        .startedAt(startedAt)
                        .endedAt(endedAt)
                        .purp(dto.getPurp())
                        .build();

        reservationRepository.save(reservation);

        if (dto.getAttendeeIds() != null && !dto.getAttendeeIds().isEmpty()) {

            List<Employee> attendees =
                    employeeRepository.findByEmpIdIn(dto.getAttendeeIds());

            for (Employee emp : attendees) {

                MeetingRoomAttendee attendee = MeetingRoomAttendee.builder()
                        .company(room.getCompany())
                        .meetingRoomReservation(reservation)
                        .employee(emp)
                        .build();

                meetingRoomAttendeeRepository.save(attendee);
            }
        }

        List<MeetingRoomAttendee> attendees = meetingRoomAttendeeRepository.findAllByMeetingRoomReservation_MeetingResvNo(reservation.getMeetingResvNo());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String startTime = reservation.getStartedAt().format(formatter);
        String endTime = reservation.getEndedAt().format(formatter);

        String title = "회의실 예약 알림";
        String roomName = reservation.getMeetingRoom().getRoomName();
        String content = String.format("[%s]에 예약되었습니다. (시간: %s ~ %s)",
                roomName, startTime, endTime);
        String url = "/my-reservations";

        for (MeetingRoomAttendee attendee : attendees) {
            Employee receiver = attendee.getEmployee();

            notificationService.sendNotification(
                    receiver,
                    NotificationType.MEETING,
                    title,
                    content,
                    url
            );
        }

        Employee host = reservation.getResvEmp();
        notificationService.sendNotification(
                host,
                NotificationType.MEETING,
                title,
                content,
                url
        );



    }

    public void deleteReservation(Long resvNo, CustomUser user) {

        // 예약 조회
        MeetingRoomReservation reservation =
                reservationRepository.findById(resvNo)
                        .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));

        // 권한 체크(예약자 본인만 취소 가능/또는 관리자 역할이면 허용)
        boolean isOwner = reservation.getResvEmp().getEmpId().equals(user.getUsername());
        boolean isAdmin = user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isOwner && !isAdmin) {
            throw new SecurityException("해당 예약을 취소할 권한이 없습니다.");
        }

        // 삭제
        meetingRoomAttendeeRepository.deleteAllByMeetingRoomReservation_MeetingResvNo(resvNo);
        reservationRepository.delete(reservation);

    }
}
