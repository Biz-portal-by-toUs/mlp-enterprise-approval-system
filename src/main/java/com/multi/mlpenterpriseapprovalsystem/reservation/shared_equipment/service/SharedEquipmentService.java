package com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.reservation.common.ReservationCompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.domain.SharedEquipment;
import com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.dto.ReqSharedEquipmentDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.dto.ResSharedEquipmentDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.repository.SharedEquipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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

    public Long registerSharedEquipment(CustomUser user, ReqSharedEquipmentDto sharedEquipmentDto, MultipartFile imageFile) {

        // 1. 권한 체크
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a ->
                        a.getAuthority().equals("ROLE_COM_ADMIN") ||
                                a.getAuthority().equals("ROLE_SEC_ADMIN") ||
                                a.getAuthority().equals("ROLE_THR_ADMIN")
                );

        if (!isAdmin) {
            throw new CustomException(
                    ErrorCode.FORBIDDEN
            );
        }

        String comId = user.getComId();
        Company company = companyRepository.findByComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

        String savedUrl = null;

        // 이미지 파일이 있을 때
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                // 확장자 추출(.jpg, .png 등)
                String ext = Optional.ofNullable(imageFile.getOriginalFilename())
                        .filter(f -> f.contains("."))
                        .map(f -> f.substring(f.lastIndexOf(".")))
                        .orElse("");

                // UUID로 파일명 생성(중복 방지)
                String fileName = UUID.randomUUID() + ext;

                // 저장 경로 생성: IMAGE_DIR + fileName
                Path savePath = Paths.get(IMAGE_DIR).resolve(fileName);  // imageDir = C:/.../uploads

                // uploads 폴더 없으면 생성(있으면 그냥 통과)
                Files.createDirectories(savePath.getParent());

                // 실제 파일 저장(디스크에 write)
                imageFile.transferTo(savePath.toFile());

                // 브라우저에서 접근할 URL 생성(정적 리소스 매핑 필요)
                savedUrl = IMAGE_URL + "/" + fileName; // imageUrl = http://localhost:8090/uploads
            } catch (IOException e) {
                throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
            }
        }

        // 요청 DTO 값으로 SharedEquipment 엔티티 생성 (회사 FK 포함)
        SharedEquipment sharedEquipment = SharedEquipment.builder()
                .company(company)
                .eqName(sharedEquipmentDto.getEqName())
                .eqId(sharedEquipmentDto.getEqId())
                .modelName(sharedEquipmentDto.getModelName())
                .imgUrl(savedUrl)
                .loc(sharedEquipmentDto.getLocation())
                .build();

        return sharedEquipmentRepository.save(sharedEquipment).getEqNo();
    }

    public Long updateSharedEquipment(Long eqNo, CustomUser user, ReqSharedEquipmentDto sharedEquipmentDto, MultipartFile imageFile) {

        // 1. 권한 체크
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a ->
                        a.getAuthority().equals("ROLE_COM_ADMIN") ||
                                a.getAuthority().equals("ROLE_SEC_ADMIN") ||
                                a.getAuthority().equals("ROLE_THR_ADMIN")
                );

        if (!isAdmin) {
            throw new CustomException(
                    ErrorCode.FORBIDDEN
            );
        }

        // 수정할 공유 설비 조회
        SharedEquipment sharedEquipment = sharedEquipmentRepository.findById(eqNo)
                .orElseThrow(() -> new CustomException(ErrorCode.SHARED_EQUIPMENT_NOT_FOUND));

        // 같은 회사 데이터인지 검증(보안)
        String comId = user.getComId();
        if (!sharedEquipment.getCompany().getComId().equals(comId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        boolean isImageChanged = imageFile != null && !imageFile.isEmpty();
        boolean isInfoChanged = !isSame(sharedEquipment, sharedEquipmentDto);

        if (!isImageChanged && !isInfoChanged) {
            // 변경 없음 → 그냥 바로 리턴
            return sharedEquipment.getEqNo();
        }

        // 이미지 파일이 있으면 새로 저장하고 imgUrl만 교체
        if (imageFile != null && !imageFile.isEmpty()) {

            // 확장자 추출
            String ext = Optional.ofNullable(imageFile.getOriginalFilename())
                    .filter(f -> f.contains("."))
                    .map(f -> f.substring(f.lastIndexOf(".")))
                    .orElse("");

            // UUID 파일명 생성
            String fileName = UUID.randomUUID() + ext;


            try {
                // 저장 경로 생성 및 디렉토리 생성
                Path savePath = Paths.get(IMAGE_DIR).resolve(fileName);
                Files.createDirectories(savePath.getParent());

                // 실제 파일 저장
                imageFile.transferTo(savePath.toFile());

                // 접근 URL 생성 후 엔티티에 반영
                String savedUrl = IMAGE_URL + "/" + fileName;
                sharedEquipment.changeImageUrl(savedUrl);
            } catch (IOException e) {
                throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
            }
        }

        // 이미지 외의 필드(이름/인원/위치/장비/비고 등) 업데이트
        sharedEquipment.updateInfo(sharedEquipmentDto);

        return sharedEquipment.getEqNo();
    }

    private boolean isSame(SharedEquipment sharedEquipment, ReqSharedEquipmentDto dto) {
        return sharedEquipment.getEqName().equals(dto.getEqName())
                && sharedEquipment.getEqId().equals(dto.getEqId())
                && sharedEquipment.getModelName().equals(dto.getModelName())
                && sharedEquipment.getLoc().equals(dto.getLocation());

    }

    public void deleteSharedEquipment(Long eqNo) {

        SharedEquipment sharedEquipment = sharedEquipmentRepository.findById(eqNo)
                .orElseThrow(() -> new CustomException(ErrorCode.SHARED_EQUIPMENT_NOT_FOUND));

        // 권한 체크
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        CustomUser user = (CustomUser) authentication.getPrincipal();

        boolean canDelete = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role ->
                        role.equals("ROLE_COM_ADMIN") ||
                                role.equals("ROLE_SEC_ADMIN") ||
                                role.equals("ROLE_THR_ADMIN")
                );

        if (!canDelete) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // 회사 체크
        if (!sharedEquipment.getCompany().getComId().equals(user.getComId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        sharedEquipmentRepository.delete(sharedEquipment);
    }
}
