package com.multi.mlpenterpriseapprovalsystem.reservation.sharedequipment.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.common.storage.service.S3UrlService;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.company.repository.CompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.reservation.sharedequipment.domain.SharedEquipment;
import com.multi.mlpenterpriseapprovalsystem.reservation.sharedequipment.dto.ReqSharedEquipmentDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.sharedequipment.dto.ResSharedEquipmentDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.sharedequipment.repository.SharedEquipmentRepository;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * 공유 설비 관리 도메인의 비즈니스 로직을 담당하는 Service.
 *
 * - 이미지 저장: 로컬 -> S3 업로드
 * - DB(imgUrl): S3 objectKey 저장
 * - 응답(imageUrl): objectKey -> presigned GET URL로 변환해서 내려줌
 *
 * @author : 송현님
 * @filename : SharedEquipmentService
 * @since : 2025-12-21
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SharedEquipmentService {

    private final SharedEquipmentRepository sharedEquipmentRepository;
    private final CompanyRepository companyRepository;

    private final S3Client s3Client;
    private final S3UrlService s3UrlService;

    @Value("${app.s3.bucket}")
    private String bucket;

    @Value("${app.s3.env}")
    private String env;

    @Transactional(readOnly = true)
    public ResSharedEquipmentDto getSharedEquipment(Long eqNo, CustomUser user) {

        SharedEquipment sharedEquipment = sharedEquipmentRepository.findById(eqNo)
                .orElseThrow(() -> new CustomException(ErrorCode.SHARED_EQUIPMENT_NOT_FOUND));

        if (!sharedEquipment.getCompany().getComId().equals(user.getComId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        return ResSharedEquipmentDto.builder()
                .eqNo(sharedEquipment.getEqNo())
                .eqName(sharedEquipment.getEqName())
                .eqId(sharedEquipment.getEqId())
                .modelName(sharedEquipment.getModelName())
                // ✅ objectKey -> presigned url
                .imageUrl(toImageUrl(sharedEquipment.getImgUrl()))
                .location(sharedEquipment.getLoc())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ResSharedEquipmentDto> selectSharedEquipmentWithPaging(String comId, Pageable pageable) {

        Page<SharedEquipment> sharedEquipment = sharedEquipmentRepository.findByCompany_ComId(comId, pageable);

        return sharedEquipment.map(equipment -> ResSharedEquipmentDto.builder()
                .eqNo(equipment.getEqNo())
                .comId(equipment.getCompany().getComId())
                .eqName(equipment.getEqName())
                .eqId(equipment.getEqId())
                .modelName(equipment.getModelName())
                // ✅ objectKey -> presigned url
                .imageUrl(toImageUrl(equipment.getImgUrl()))
                .location(equipment.getLoc())
                .build());
    }

    public Long registerSharedEquipment(CustomUser user, ReqSharedEquipmentDto sharedEquipmentDto, MultipartFile imageFile) {

        if (!isAdmin(user)) throw new CustomException(ErrorCode.FORBIDDEN);

        String comId = user.getComId();
        Company company = companyRepository.findByComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

        // 1) 먼저 저장해서 eqNo 확보 (이미지는 일단 null)
        SharedEquipment sharedEquipment = SharedEquipment.builder()
                .company(company)
                .eqName(sharedEquipmentDto.getEqName())
                .eqId(sharedEquipmentDto.getEqId())
                .modelName(sharedEquipmentDto.getModelName())
                .imgUrl(null) // ✅ DB에는 objectKey 저장
                .loc(sharedEquipmentDto.getLocation())
                .build();

        SharedEquipment saved = sharedEquipmentRepository.save(sharedEquipment);

        // 2) 이미지 있으면 S3 업로드 후 objectKey 저장
        if (imageFile != null && !imageFile.isEmpty()) {
            String key = uploadSharedEquipmentImageToS3(comId, saved.getEqNo(), imageFile);
            saved.changeImageUrl(key);
        }

        return saved.getEqNo();
    }

    public Long updateSharedEquipment(Long eqNo, CustomUser user, ReqSharedEquipmentDto sharedEquipmentDto, MultipartFile imageFile) {

        if (!isAdmin(user)) throw new CustomException(ErrorCode.FORBIDDEN);

        SharedEquipment sharedEquipment = sharedEquipmentRepository.findById(eqNo)
                .orElseThrow(() -> new CustomException(ErrorCode.SHARED_EQUIPMENT_NOT_FOUND));

        String comId = user.getComId();
        if (!sharedEquipment.getCompany().getComId().equals(comId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        boolean isImageChanged = imageFile != null && !imageFile.isEmpty();
        boolean isInfoChanged = !isSame(sharedEquipment, sharedEquipmentDto);

        if (!isImageChanged && !isInfoChanged) return sharedEquipment.getEqNo();

        // ✅ 이미지 변경: 새로 업로드 -> DB 반영 -> 커밋 후 oldKey 삭제
        if (isImageChanged) {
            String oldKey = sharedEquipment.getImgUrl();
            String newKey = uploadSharedEquipmentImageToS3(comId, sharedEquipment.getEqNo(), imageFile);
            sharedEquipment.changeImageUrl(newKey);

            // ✅ 커밋 성공 후에만 삭제(롤백이면 실행 안 됨)
            deleteAfterCommit(oldKey);
        }

        sharedEquipment.updateInfo(sharedEquipmentDto);
        return sharedEquipment.getEqNo();
    }

    public void deleteSharedEquipment(Long eqNo) {

        SharedEquipment sharedEquipment = sharedEquipmentRepository.findById(eqNo)
                .orElseThrow(() -> new CustomException(ErrorCode.SHARED_EQUIPMENT_NOT_FOUND));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        CustomUser user = (CustomUser) authentication.getPrincipal();

        if (!isAdmin(user)) throw new CustomException(ErrorCode.FORBIDDEN);

        if (!sharedEquipment.getCompany().getComId().equals(user.getComId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        String oldKey = sharedEquipment.getImgUrl();

        sharedEquipmentRepository.delete(sharedEquipment);

        // ✅ DB 삭제 커밋된 다음에만 S3 삭제
        deleteAfterCommit(oldKey);
    }

    /* ===================== helpers ===================== */

    private boolean isAdmin(CustomUser user) {
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role ->
                        role.equals("ROLE_COM_ADMIN") ||
                                role.equals("ROLE_SEC_ADMIN") ||
                                role.equals("ROLE_THR_ADMIN")
                );
    }

    private boolean isSame(SharedEquipment sharedEquipment, ReqSharedEquipmentDto dto) {
        return sharedEquipment.getEqName().equals(dto.getEqName())
                && sharedEquipment.getEqId().equals(dto.getEqId())
                && sharedEquipment.getModelName().equals(dto.getModelName())
                && sharedEquipment.getLoc().equals(dto.getLocation());
    }

    private String uploadSharedEquipmentImageToS3(String comId, Long eqNo, MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) return null;

        String contentType = imageFile.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        String original = Optional.ofNullable(imageFile.getOriginalFilename()).orElse("image");
        String safeName = original.replaceAll("\\s+", "_")
                .replaceAll("[\\\\/:*?\"<>|]", "_");

        String ext = "";
        int dot = safeName.lastIndexOf('.');
        if (dot > 0 && dot < safeName.length() - 1) ext = safeName.substring(dot);

        String key = String.format("%s/%s/shared-equipment/%d/%s%s",
                env, comId, eqNo, UUID.randomUUID(), ext
        );

        try {
            PutObjectRequest putReq = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(
                    putReq,
                    RequestBody.fromInputStream(imageFile.getInputStream(), imageFile.getSize())
            );

            return key;
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private String toImageUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return null;

        // 과거 데이터가 URL이면 그대로 내려줌(마이그레이션 전 호환)
        if (objectKey.startsWith("http://") || objectKey.startsWith("https://")) {
            return objectKey;
        }

        return s3UrlService.presignGetUrl(objectKey);
    }

    private void deleteS3ObjectQuietly(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return;

        // 과거 데이터가 URL이면 삭제 시도 안 함
        if (objectKey.startsWith("http://") || objectKey.startsWith("https://")) return;

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build());
        } catch (Exception e) {
            log.warn("[shared-equipment] S3 delete failed. key={}", objectKey, e);
        }
    }

    private void deleteAfterCommit(String oldKey) {
        if (oldKey == null || oldKey.isBlank()) return;

        // 트랜잭션이 없으면 즉시 삭제(정책에 따라 return로 바꿔도 됨)
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteS3ObjectQuietly(oldKey);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteS3ObjectQuietly(oldKey);
            }
        });
    }
}
