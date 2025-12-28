package com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.domain.SharedEquipment;
import com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.domain.SharedEquipmentReservation;
import com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.dto.ReqReservationCreateDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.dto.ResReservationListDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.repository.SharedEquipmentRepository;
import com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.repository.SharedEquipmentReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 공유 설비 예약 비즈니스 로직을 처리하는 Service 클래스
 * <p>
 * 공유 설비 예약과 관련된 조회, 생성, 취소 등의
 * 핵심 비즈니스 로직을 담당한다.
 *
 * @author : 송현님
 * @filename : SharedEquipmentReservationService
 * @since : 2025-12-28 오후 4:50 일요일
 */

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SharedEquipmentReservationService {

    private final SharedEquipmentReservationRepository reservationRepository;
    private final SharedEquipmentRepository sharedEquipmentRepository;
    private final EmployeeRepository employeeRepository;

    public List<ResReservationListDto> getReservations(String comId, String data) {
        List<SharedEquipmentReservation> reservations =
                reservationRepository.findByCompany_ComIdAndIsDeletedFalse(comId);

        if (data != null) {
            reservations = reservations.stream()
                    .filter(r -> r.getStartedAt().toLocalDate().toString().equals(data))
                    .toList();
        }

        return reservations.stream()
                .map(resv -> new ResReservationListDto(
                        resv.getEqResvNo(),
                        resv.getSharedEquipment().getEqNo(),
                        resv.getSharedEquipment().getEqName(),
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
        SharedEquipment eq = sharedEquipmentRepository.findById(dto.getEqNo())
                .orElseThrow(() -> new IllegalArgumentException("법인 차량이 존재하지 않습니다."));

        // 예약자 조회 (Employee)
        Employee employee = employeeRepository.findByEmpId(user.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("예약자 정보가 없습니다."));

        // 중복 예약 검사
        if (reservationRepository.existsOverlapping(dto.getEqNo(), startedAt, endedAt)) {
            throw new IllegalArgumentException("이미 예약된 시간입니다.");
        }

        SharedEquipmentReservation reservation =
                SharedEquipmentReservation.builder()
                        .company(eq.getCompany())
                        .sharedEquipment(eq)
                        .resvEmp(employee)
                        .startedAt(startedAt)
                        .endedAt(endedAt)
                        .purp(dto.getPurp())
                        .build();

        reservationRepository.save(reservation);

    }
}



