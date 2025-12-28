package com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.service;

import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.domain.SharedEquipmentReservation;
import com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.dto.ResReservationListDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.repository.SharedEquipmentRepository;
import com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.repository.SharedEquipmentReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

}


