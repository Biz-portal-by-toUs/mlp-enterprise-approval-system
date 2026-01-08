package com.multi.mlpenterpriseapprovalsystem.reservation.myreservation.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.reservation.corporatecar.domain.CorporateCarReservation;
import com.multi.mlpenterpriseapprovalsystem.reservation.corporatecar.repository.CorporateCarReservationRepository;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.domain.MeetingRoomAttendee;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.domain.MeetingRoomReservation;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.repository.MeetingRoomAttendeeRepository;
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
import java.util.*;
import java.util.stream.Collectors;

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
    private final MeetingRoomAttendeeRepository meetingRoomAttendeeRepository; // ✅ 추가
    private final CorporateCarReservationRepository corporateCarReservationRepository;
    private final SharedEquipmentReservationRepository sharedEquipmentReservationRepository;

    public List<ResMyReservationDto> getMyReservations(
            CustomUser user,
            LocalDate startDate,
            LocalDate endDate,
            String domain
    ) {
        String comId = user.getComId();
        String empId = user.getUsername();

        // ✅ 날짜 필터: null이면 넓게 잡아 “초기 진입 시도 조회 가능”하게
        LocalDate base = LocalDate.now();
        LocalDate s = (startDate != null) ? startDate : base.minusYears(1);
        LocalDate e = (endDate != null) ? endDate : base.plusYears(1);

        LocalDateTime from = s.atStartOfDay();
        LocalDateTime toExclusive = e.plusDays(1).atStartOfDay();

        // ✅ domain 기본값: 회의실
        String d = (domain == null || domain.isBlank()) ? "MEETING_ROOM" : domain.trim().toUpperCase();

        List<ResMyReservationDto> rows = new ArrayList<>();

        switch (d) {
            case "MEETING_ROOM" -> addMeetingRoom(rows, comId, empId, from, toExclusive);
            case "CORPORATE_CAR" -> addCorporateCar(rows, comId, empId, from, toExclusive);
            case "SHARED_EQUIPMENT" -> addSharedEquipment(rows, comId, empId, from, toExclusive);
            default -> addMeetingRoom(rows, comId, empId, from, toExclusive); // 안전장치
        }

        // ✅ 정렬: 다가오는 예약(오름차순) + 과거 예약(내림차순)
        sortUpcomingFirst(rows);

        return rows;
    }

    private void addMeetingRoom(List<ResMyReservationDto> rows, String comId, String empId,
                                LocalDateTime from, LocalDateTime toExclusive) {

        List<MeetingRoomReservation> hostList =
                meetingRoomReservationRepository
                        .findAllByCompany_ComIdAndResvEmp_EmpIdAndStartedAtBetween(comId, empId, from, toExclusive);

        List<MeetingRoomReservation> attendeeList =
                meetingRoomReservationRepository
                        .findAllAttendingByEmp(comId, empId, from, toExclusive);

        // ✅ 중복 제거
        LinkedHashMap<Long, MeetingRoomReservation> unique = new LinkedHashMap<>();
        for (MeetingRoomReservation r : hostList) unique.put(r.getMeetingResvNo(), r);
        for (MeetingRoomReservation r : attendeeList) unique.putIfAbsent(r.getMeetingResvNo(), r);

        List<Long> resvNos = unique.values().stream()
                .map(MeetingRoomReservation::getMeetingResvNo)
                .toList();

        // ✅ 참석자 이름을 한 번에 조회해서 map으로 묶기 (N+1 방지)
        Map<Long, List<String>> attendeeNamesByResvNo = new HashMap<>();
        if (!resvNos.isEmpty()) {
            List<MeetingRoomAttendee> attendees =
                    meetingRoomAttendeeRepository.findAllByMeetingRoomReservation_MeetingResvNoIn(resvNos);

            attendeeNamesByResvNo = attendees.stream()
                    .collect(Collectors.groupingBy(
                            a -> a.getMeetingRoomReservation().getMeetingResvNo(),
                            Collectors.collectingAndThen(
                                    Collectors.mapping(x -> x.getEmployee().getEmpName(),
                                            Collectors.toCollection(LinkedHashSet::new)),
                                    set -> new ArrayList<>(set)
                            )
                    ));
        }

        for (MeetingRoomReservation r : unique.values()) {
            Long resvNo = r.getMeetingResvNo();

            rows.add(ResMyReservationDto.builder()
                    .domain("MEETING_ROOM")
                    .resvNo(resvNo)
                    .date(r.getStartedAt().toLocalDate())
                    .time(fmtRange(r.getStartedAt(), r.getEndedAt()))
                    .status(statusOf(r.getEndedAt()))
                    .roomName(nvl(r.getMeetingRoom() == null ? null : r.getMeetingRoom().getRoomName(), "-"))
                    .hostName(nvl(r.getResvEmp() == null ? null : r.getResvEmp().getEmpName(), "-"))
                    .attendeeNames(attendeeNamesByResvNo.getOrDefault(resvNo, List.of()))
                    .build());
        }
    }

    private void addCorporateCar(List<ResMyReservationDto> rows, String comId, String empId,
                                 LocalDateTime from, LocalDateTime toExclusive) {

        List<CorporateCarReservation> carList =
                corporateCarReservationRepository
                        .findAllByCompany_ComIdAndResvEmp_EmpIdAndStartedAtBetween(comId, empId, from, toExclusive);

        for (CorporateCarReservation r : carList) {
            String carName = (r.getCorporateCar() == null) ? "법인차량"
                    : nvl(r.getCorporateCar().getCarName(), "법인차량");

            // ✅ 예약 엔티티에 저장된 번호판 사용
            String plateNo =
                    (r.getPlateNo() != null && !r.getPlateNo().isBlank())
                            ? r.getPlateNo()
                            : (r.getCorporateCar() == null
                            ? "-" : nvl(r.getCorporateCar().getPlateNo(), "-"));

            // ✅ 목적도 내려줘야 프론트 목적 칼럼이 채워짐
            String purp = nvl(r.getPurp(), "-");

            rows.add(ResMyReservationDto.builder()
                    .domain("CORPORATE_CAR")
                    .resvNo(r.getCarResvNo())
                    .date(r.getStartedAt().toLocalDate())
                    .time(fmtRange(r.getStartedAt(), r.getEndedAt()))
                    .status(statusOf(r.getEndedAt()))
                    .carName(carName)
                    .plateNo(plateNo)
                    .purp(purp)
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
            String eqName = (r.getSharedEquipment() == null) ? "공유설비"
                    : nvl(r.getSharedEquipment().getEqName(), "공유설비");

            // ⚠️ 보관위치 getter는 실제 필드명으로 수정 필요
            // 예: getStorageLocation(), getLoc(), getStorageLoc() 등
            String eqId =
                    (r.getSharedEquipment() == null)
                            ? "-"
                            : nvl(r.getSharedEquipment().getEqId(), "-");

            rows.add(ResMyReservationDto.builder()
                    .domain("SHARED_EQUIPMENT")
                    .resvNo(r.getEqResvNo())
                    .date(r.getStartedAt().toLocalDate())
                    .time(fmtRange(r.getStartedAt(), r.getEndedAt()))
                    .status(statusOf(r.getEndedAt()))
                    .eqName(eqName)
                    .eqId(eqId)
                    .purp(r.getPurp())
                    .build());
        }
    }

    // ===== 유틸 =====
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

    private static void sortUpcomingFirst(List<ResMyReservationDto> rows) {
        LocalDateTime now = LocalDateTime.now();

        rows.sort((a, b) -> {
            LocalDateTime aStart = LocalDateTime.of(a.getDate(), parseStartTime(a.getTime()));
            LocalDateTime bStart = LocalDateTime.of(b.getDate(), parseStartTime(b.getTime()));

            boolean aUpcoming = !aStart.isBefore(now);
            boolean bUpcoming = !bStart.isBefore(now);

            if (aUpcoming != bUpcoming) return aUpcoming ? -1 : 1; // 다가오는 예약 먼저
            if (aUpcoming) return aStart.compareTo(bStart);        // 미래/오늘: 오름차순
            return bStart.compareTo(aStart);                       // 과거: 내림차순
        });
    }
}