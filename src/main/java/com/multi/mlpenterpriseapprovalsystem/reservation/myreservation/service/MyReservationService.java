package com.multi.mlpenterpriseapprovalsystem.reservation.myreservation.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.reservation.corporatecar.domain.CorporateCarReservation;
import com.multi.mlpenterpriseapprovalsystem.reservation.corporatecar.repository.CorporateCarReservationRepository;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.domain.MeetingRoomReservation;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.repository.MeetingRoomReservationRepository;
import com.multi.mlpenterpriseapprovalsystem.reservation.myreservation.dto.ResMyReservationDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.sharedequipment.domain.SharedEquipmentReservation;
import com.multi.mlpenterpriseapprovalsystem.reservation.sharedequipment.repository.SharedEquipmentReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 내 예약 조회 서비스
 *
 * 로그인한 사용자의 예약 데이터를 기간(from~to) 조건으로 조회하여,
 * 화면에서 바로 표시할 수 있는 공통 형태(ResMyReservationDto)로 변환해 반환한다.
 *
 * @author : 송현님
 * @filename : MyReservationService
 * @since : 2025-12-28 오후 10:30 일요일
 */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyReservationService {

    private final MeetingRoomReservationRepository meetingRoomReservationRepository;
    private final CorporateCarReservationRepository corporateCarReservationRepository;
    private final SharedEquipmentReservationRepository sharedEquipmentReservationRepository;

    public List<ResMyReservationDto> getMyReservations(CustomUser user, LocalDate startDate, LocalDate endDate, String domain) {

        String comId = user.getComId();
        String empId = user.getUsername();

        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime toExclusive = endDate.plusDays(1).atStartOfDay();

        // ✅ 여기! domain 정규화 + 분기용
        String d = (domain == null) ? "ALL" : domain.trim().toUpperCase();

        List<ResMyReservationDto> rows = new ArrayList<>();

        switch (d) {
            case "MEETING_ROOM":
                addMeetingRoom(rows, comId, empId, from, toExclusive);
                break;

            case "CORPORATE_CAR":
                addCorporateCar(rows, comId, empId, from, toExclusive);
                break;

            case "SHARED_EQUIPMENT":
                addSharedEquipment(rows, comId, empId, from, toExclusive);
                break;

            case "ALL":
            default:
                addMeetingRoom(rows, comId, empId, from, toExclusive);
                addCorporateCar(rows, comId, empId, from, toExclusive);
                addSharedEquipment(rows, comId, empId, from, toExclusive);
                break;
        }

        // 정렬은 그대로
        rows.sort(Comparator
                .comparing(ResMyReservationDto::getDate)
                .thenComparing(r -> parseStartTime(r.getTime()))
        );

        return rows;
    }

    // ✅ 아래 3개는 기존 for문 덩어리 그대로 “메서드로만” 빼준 거야
    private void addMeetingRoom(List<ResMyReservationDto> rows, String comId, String empId,
                                LocalDateTime from, LocalDateTime toExclusive) {
        List<MeetingRoomReservation> mrList =
                meetingRoomReservationRepository.findAllByCompany_ComIdAndResvEmp_EmpIdAndStartedAtBetween(
                        comId, empId, from, toExclusive
                );

        for (MeetingRoomReservation r : mrList) {
            rows.add(ResMyReservationDto.builder()
                    .target("회의실")
                    .date(r.getStartedAt().toLocalDate())
                    .time(fmtRange(r.getStartedAt(), r.getEndedAt()))
                    .purpose(nvl(r.getPurp()))
                    .location(nvl(r.getMeetingRoom() == null ? null : r.getMeetingRoom().getLoc()))
                    .status(statusOf(r.getEndedAt()))
                    .domain("MEETING_ROOM")
                    .resvNo(r.getMeetingResvNo())
                    .build());
        }
    }

    private void addCorporateCar(List<ResMyReservationDto> rows, String comId, String empId,
                                 LocalDateTime from, LocalDateTime toExclusive) {
        List<CorporateCarReservation> carList =
                corporateCarReservationRepository.findAllByCompany_ComIdAndResvEmp_EmpIdAndStartedAtBetween(
                        comId, empId, from, toExclusive
                );

        for (CorporateCarReservation r : carList) {
            rows.add(ResMyReservationDto.builder()
                    .target(nvl(r.getCorporateCar() == null ? null : r.getCorporateCar().getCarName(), "법인차량"))
                    .date(r.getStartedAt().toLocalDate())
                    .time(fmtRange(r.getStartedAt(), r.getEndedAt()))
                    .purpose(nvl(r.getPurp()))
                    .location("-")
                    .status(statusOf(r.getEndedAt()))
                    .domain("CORPORATE_CAR")
                    .resvNo(r.getCarResvNo())
                    .build());
        }
    }

    private void addSharedEquipment(List<ResMyReservationDto> rows, String comId, String empId,
                                    LocalDateTime from, LocalDateTime toExclusive) {
        List<SharedEquipmentReservation> eqList =
                sharedEquipmentReservationRepository.findAllByCompany_ComIdAndResvEmp_EmpIdAndStartedAtBetween(
                        comId, empId, from, toExclusive
                );

        for (SharedEquipmentReservation r : eqList) {
            rows.add(ResMyReservationDto.builder()
                    .target(nvl(r.getSharedEquipment() == null ? null : r.getSharedEquipment().getEqName(), "공유설비"))
                    .date(r.getStartedAt().toLocalDate())
                    .time(fmtRange(r.getStartedAt(), r.getEndedAt()))
                    .purpose(nvl(r.getPurp()))
                    .location("-")
                    .status(statusOf(r.getEndedAt()))
                    .domain("SHARED_EQUIPMENT")
                    .resvNo(r.getEqResvNo())
                    .build());
        }
    }

    // 아래 유틸들은 너 원래 있던 거 그대로 쓰면 됨
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private static String fmtRange(LocalDateTime s, LocalDateTime e) {
        return s.toLocalTime().format(TIME_FMT) + "~" + e.toLocalTime().format(TIME_FMT);
    }

    private static LocalTime parseStartTime(String range) {
        try {
            String start = range.split("~")[0];
            return LocalTime.parse(start, TIME_FMT);
        } catch (Exception e) {
            return LocalTime.MIN;
        }
    }

    private static String statusOf(LocalDateTime endedAt) {
        return endedAt.isBefore(LocalDateTime.now()) ? "사용 완료" : "사용 예정";
    }

    private static String nvl(String v) {
        return (v == null || v.isBlank()) ? "-" : v;
    }

    private static String nvl(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
