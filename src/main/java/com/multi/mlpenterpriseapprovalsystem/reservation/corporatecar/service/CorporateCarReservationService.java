package com.multi.mlpenterpriseapprovalsystem.reservation.corporatecar.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.reservation.corporatecar.domain.CorporateCar;
import com.multi.mlpenterpriseapprovalsystem.reservation.corporatecar.domain.CorporateCarReservation;
import com.multi.mlpenterpriseapprovalsystem.reservation.corporatecar.dto.ReqReservationCreateDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.corporatecar.dto.ResReservationListDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.corporatecar.repository.CorporateCarRepository;
import com.multi.mlpenterpriseapprovalsystem.reservation.corporatecar.repository.CorporateCarReservationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 법인 차량 예약 비즈니스 로직을 처리하는 Service 클래스
 *
 * 법인 차량 예약과 관련된 조회, 생성, 취소 등의
 * 핵심 비즈니스 로직을 담당한다.
 *
 * @author : 송현님
 * @filename : CorporateCarReservationService
 * @since : 2025-12-27 오후 10:28 토요일
 */

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CorporateCarReservationService {

    private final CorporateCarReservationRepository reservationRepository;
    private final CorporateCarRepository corporateCarRepository;
    private final EmployeeRepository employeeRepository;

    public List<ResReservationListDto> getReservations(String comId, String data) {
        List<CorporateCarReservation> reservations =
                reservationRepository.findByCompany_ComIdAndIsDeletedFalse(comId);

        if (data != null) {
            reservations = reservations.stream()
                    .filter(r -> r.getStartedAt().toLocalDate().toString().equals(data))
                    .toList();
        }

        return reservations.stream()
                .map(resv -> new ResReservationListDto(
                        resv.getCarResvNo(),
                        resv.getCorporateCar().getCarNo(),
                        resv.getCorporateCar().getCarName(),
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
        CorporateCar car = corporateCarRepository.findById(dto.getCarNo())
                .orElseThrow(() -> new IllegalArgumentException("법인 차량이 존재하지 않습니다."));

        // 예약자 조회 (Employee)
        Employee employee = employeeRepository.findByEmpId(user.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("예약자 정보가 없습니다."));

        // 중복 예약 검사
        if (reservationRepository.existsOverlapping(dto.getCarNo(), startedAt, endedAt)) {
            throw new IllegalArgumentException("이미 예약된 시간입니다.");
        }

        CorporateCarReservation reservation =
                CorporateCarReservation.builder()
                        .company(car.getCompany())
                        .corporateCar(car)
                        .resvEmp(employee)
                        .startedAt(startedAt)
                        .endedAt(endedAt)
                        .purp(dto.getPurp())
                        .build();

        reservationRepository.save(reservation);

    }

    public void deleteReservation(Long resvNo, CustomUser user) {

        // 예약 조회
        CorporateCarReservation reservation =
                reservationRepository.findById(resvNo)
                        .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));

        // 권한 체크(예약자 본인만 취소 가능/또는 관리자 역할이면 허용)
        boolean isOwner = reservation.getResvEmp().getEmpId().equals(user.getUsername());
        boolean isAdmin = user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isOwner && !isAdmin) {
            throw new SecurityException("해당 예약을 취소할 권한이 없습니다.");
        }

        // 삭제
        reservationRepository.delete(reservation);
    }
}
