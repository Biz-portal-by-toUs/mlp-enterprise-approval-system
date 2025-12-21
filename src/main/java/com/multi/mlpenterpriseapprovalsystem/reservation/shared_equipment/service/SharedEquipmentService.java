package com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.reservation.common.ReservationCompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.domain.SharedEquipment;
import com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.dto.ResSharedEquipmentDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.repository.SharedEquipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공유 설비 관리 도메인의 비즈니스 로직을 담당하는 Service.
 *
 * 공유 설비 정보의 조회, 등록, 수정, 삭제와 관련된 비즈니스 로직을 처리한다.
 * 데이터 접근은 Repository 계층에 위임하며, 도메인 규칙 및 처리 흐름을 관리한다.
 *
 * @author : 송현님
 * @filename : SharedEquipmentService
 * @since : 2025-12-21 오후 11:32 일요일
 */

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SharedEquipmentService {
    private final SharedEquipmentRepository sharedEquipmentRepository;
    private final ReservationCompanyRepository companyRepository;

    @Value("${image.image-dir}")  // 서버의 실제 저장 위치
    private String IMAGE_DIR;
    @Value("${image.image-url}")  // 브라우저에서 접근하는 주소
    private String IMAGE_URL;

    public ResSharedEquipmentDto getSharedEquipment(Long eqNo, CustomUser user) {

        SharedEquipment sharedEquipment = sharedEquipmentRepository.findById(eqNo)
                .orElseThrow(() -> new CustomException(ErrorCode.SHARED_EQUIPMENT_NOT_FOUND));

        // 같은 회사인지 체크
        if (!sharedEquipment.getCompany().getComId().equals(user.getComId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        return ResSharedEquipmentDto.builder()
                .eqNo(sharedEquipment.getEqNo())
                .eqName(sharedEquipment.getEqName())
                .eqId(sharedEquipment.getEqId())
                .modelName(sharedEquipment.getModelName())
                .imageUrl(sharedEquipment.getImgUrl())
                .location(sharedEquipment.getLoc())
                .build();
    }



    @Transactional(readOnly = true)
    public Page<ResSharedEquipmentDto> selectSharedEquipmentWithPaging(String comId, Pageable pageable) {
        // 반환 타입이 Page<ResSharedEquipmentDto>인 이유는 데이터 뿐만 아니라 totalPages, totalElements, first/last 같은 페이지 정보도 같이 주려고
        Page<SharedEquipment> sharedEquipment = sharedEquipmentRepository.findByCompany_ComId(comId, pageable);

        // Entity -> Response DTO로 변환 (Page.map()은 Page 형태 유지하면서 내부 요소만 변환)
        return sharedEquipment.map(equipment -> ResSharedEquipmentDto.builder()
                .eqNo(equipment.getEqNo())
                .comId(equipment.getCompany().getComId())
                .eqName(equipment.getEqName())
                .eqId(equipment.getEqId())
                .modelName(equipment.getModelName())
                .imageUrl(equipment.getImgUrl())
                .location(equipment.getLoc())
                .build());
    }
}
