package com.multi.mlpenterpriseapprovalsystem.reservation.corporate_car.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.reservation.common.ReservationCompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.reservation.corporate_car.domain.CorporateCar;
import com.multi.mlpenterpriseapprovalsystem.reservation.corporate_car.dto.ResCorporateCarDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.corporate_car.repository.CorporateCarRepository;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.domain.MeetingRoom;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.dto.ResMeetingRoomDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.repository.MeetingRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 법인 차량 관리 도메인의 비즈니스 로직을 담당하는 Service.
 * <p>
 * 법인 차량 정보의 조회, 등록, 수정, 삭제와 관련된 비즈니스 로직을 처리한다.
 * 데이터 접근은 Repository 계층에 위임하며, 도메인 규칙 및 처리 흐름을 관리한다.
 *
 * @author : 송현님
 * @filename : CorporateCarService
 * @since : 2025-12-21 오후 12:58 일요일
 */

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CorporateCarService {
    private final CorporateCarRepository corporateCarRepository;
    private final ReservationCompanyRepository companyRepository;

    @Value("${image.image-dir}")  // 서버의 실제 저장 위치
    private String IMAGE_DIR;
    @Value("${image.image-url}")  // 브라우저에서 접근하는 주소
    private String IMAGE_URL;

    public ResCorporateCarDto getCorporateCar(Long carNo, CustomUser user) {

        CorporateCar corporateCar = corporateCarRepository.findById(carNo)
                .orElseThrow(() -> new CustomException(ErrorCode.CORPORATE_CAR_NOT_FOUND));

        // 같은 회사인지 체크
        if (!corporateCar.getCompany().getComId().equals(user.getComId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        return ResCorporateCarDto.builder()
                .carNo(corporateCar.getCarNo())
                .carName(corporateCar.getCarName())
                .plateNo(corporateCar.getPlateNo())
                .carType(corporateCar.getCarType())
                .fuel(corporateCar.getFuel())
                .capacity(corporateCar.getCap())
                .imageUrl(corporateCar.getImgUrl())
                .build();
    }



    @Transactional(readOnly = true)
    public Page<ResCorporateCarDto> selectCorporateCarsWithPaging(String comId, Pageable pageable) {
        // 반환 타입이 Page<ResCorporateCarDto>인 이유는 데이터 뿐만 아니라 totalPages, totalElements, first/last 같은 페이지 정보도 같이 주려고
        Page<CorporateCar> corporateCars = corporateCarRepository.findByCompany_ComId(comId, pageable);

        // Entity -> Response DTO로 변환 (Page.map()은 Page 형태 유지하면서 내부 요소만 변환)
        return corporateCars.map(corporateCar -> ResCorporateCarDto.builder()
                .carNo(corporateCar.getCarNo())
                .carName(corporateCar.getCarName())
                .plateNo(corporateCar.getPlateNo())
                .carType(corporateCar.getCarType())
                .fuel(corporateCar.getFuel())
                .capacity(corporateCar.getCap())
                .imageUrl(corporateCar.getImgUrl())
                .build());
    }
}
