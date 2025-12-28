package com.multi.mlpenterpriseapprovalsystem.reservation.corporate_car.service;

import com.multi.mlpenterpriseapprovalsystem.reservation.corporate_car.domain.CorporateCarReservation;
import com.multi.mlpenterpriseapprovalsystem.reservation.corporate_car.dto.ResReservationListDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.corporate_car.repository.CorporateCarRepository;
import com.multi.mlpenterpriseapprovalsystem.reservation.corporate_car.repository.CorporateCarReservationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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






}
