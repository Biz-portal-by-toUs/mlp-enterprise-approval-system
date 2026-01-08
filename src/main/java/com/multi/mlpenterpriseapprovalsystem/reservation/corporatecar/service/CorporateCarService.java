package com.multi.mlpenterpriseapprovalsystem.reservation.corporatecar.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.common.storage.service.S3UrlService;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.company.repository.CompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.reservation.corporatecar.domain.CorporateCar;
import com.multi.mlpenterpriseapprovalsystem.reservation.corporatecar.dto.ReqCorporateCarDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.corporatecar.dto.ResCorporateCarDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.corporatecar.repository.CorporateCarRepository;
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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 법인 차량 관리 도메인의 비즈니스 로직을 담당하는 Service.
 *
 * - 이미지 저장: 로컬 -> S3 업로드
 * - DB(imgUrl): S3 objectKey 저장
 * - 응답(imageUrl): objectKey -> presigned GET URL로 변환해서 내려줌
 *
 * @author : 송현님
 * @filename : CorporateCarService
 * @since : 2025-12-21
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CorporateCarService {

    private final CorporateCarRepository corporateCarRepository;
    private final CompanyRepository companyRepository;

    private final S3Client s3Client;
    private final S3UrlService s3UrlService;

    @Value("${app.s3.bucket}")
    private String bucket;

    @Value("${app.s3.env}")
    private String env;

    @Transactional(readOnly = true)
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
                // ✅ objectKey -> presigned url
                .imageUrl(toImageUrl(corporateCar.getImgUrl()))
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ResCorporateCarDto> selectCorporateCarsWithPaging(String comId, Pageable pageable) {

        Page<CorporateCar> corporateCars = corporateCarRepository.findByCompany_ComId(comId, pageable);

        return corporateCars.map(corporateCar -> ResCorporateCarDto.builder()
                .carNo(corporateCar.getCarNo())
                .carName(corporateCar.getCarName())
                .plateNo(corporateCar.getPlateNo())
                .carType(corporateCar.getCarType())
                .fuel(corporateCar.getFuel())
                .capacity(corporateCar.getCap())
                // ✅ objectKey -> presigned url
                .imageUrl(toImageUrl(corporateCar.getImgUrl()))
                .build());
    }

    public Long registerCorporateCar(CustomUser user, ReqCorporateCarDto corporateCarDto, MultipartFile imageFile) {

        if (!isAdmin(user)) throw new CustomException(ErrorCode.FORBIDDEN);

        String comId = user.getComId();
        Company company = companyRepository.findByComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

        // 차량번호 중복 체크
        if (corporateCarRepository.existsByCompany_ComIdAndPlateNo(comId, corporateCarDto.getPlateNo())) {
            throw new CustomException(ErrorCode.DUPLICATE_CAR_PLATE_NO);
        }

        // 1) 먼저 저장해서 carNo 확보 (이미지는 일단 null)
        CorporateCar corporateCar = CorporateCar.builder()
                .company(company)
                .carName(corporateCarDto.getCarName())
                .plateNo(corporateCarDto.getPlateNo())
                .cap(corporateCarDto.getCapacity())
                .carType(corporateCarDto.getCarType())
                .fuel(corporateCarDto.getFuel())
                .imgUrl(null) // ✅ DB에는 objectKey 저장
                .build();

        CorporateCar saved = corporateCarRepository.save(corporateCar);

        // 2) 이미지 있으면 S3 업로드 후 objectKey 저장
        if (imageFile != null && !imageFile.isEmpty()) {
            String key = uploadCorporateCarImageToS3(comId, saved.getCarNo(), imageFile);
            saved.changeImageUrl(key);
        }

        return saved.getCarNo();
    }

    public Long updateCorporateCar(Long carNo, CustomUser user, ReqCorporateCarDto corporateCarDto, MultipartFile imageFile) {

        if (!isAdmin(user)) throw new CustomException(ErrorCode.FORBIDDEN);

        CorporateCar corporateCar = corporateCarRepository.findById(carNo)
                .orElseThrow(() -> new CustomException(ErrorCode.CORPORATE_CAR_NOT_FOUND));

        String comId = user.getComId();
        if (!corporateCar.getCompany().getComId().equals(comId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // 차량번호 변경 시 중복 체크
        String newPlateNo = corporateCarDto.getPlateNo();
        if (!corporateCar.getPlateNo().equals(newPlateNo)) {
            if (corporateCarRepository.existsByCompany_ComIdAndPlateNoAndCarNoNot(comId, newPlateNo, carNo)) {
                throw new CustomException(ErrorCode.DUPLICATE_CAR_PLATE_NO);
            }
        }

        boolean isImageChanged = imageFile != null && !imageFile.isEmpty();
        boolean isInfoChanged = !isSame(corporateCar, corporateCarDto);

        if (!isImageChanged && !isInfoChanged) return corporateCar.getCarNo();

        // ✅ 이미지 변경: 새로 업로드 -> DB 반영 -> 커밋 후 oldKey 삭제
        if (isImageChanged) {
            String oldKey = corporateCar.getImgUrl();
            String newKey = uploadCorporateCarImageToS3(comId, corporateCar.getCarNo(), imageFile);
            corporateCar.changeImageUrl(newKey);

            // ✅ 커밋 성공 후에만 삭제(롤백이면 실행 안 됨)
            deleteAfterCommit(oldKey);
        }

        corporateCar.updateInfo(corporateCarDto);
        return corporateCar.getCarNo();
    }

    public void deleteCorporateCar(Long carNo) {

        CorporateCar corporateCar = corporateCarRepository.findById(carNo)
                .orElseThrow(() -> new CustomException(ErrorCode.CORPORATE_CAR_NOT_FOUND));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        CustomUser user = (CustomUser) authentication.getPrincipal();

        if (!isAdmin(user)) throw new CustomException(ErrorCode.FORBIDDEN);

        if (!corporateCar.getCompany().getComId().equals(user.getComId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        String oldKey = corporateCar.getImgUrl();

        corporateCarRepository.delete(corporateCar);

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

    private boolean isSame(CorporateCar corporateCar, ReqCorporateCarDto dto) {
        return corporateCar.getCarName().equals(dto.getCarName())
                && corporateCar.getPlateNo().equals(dto.getPlateNo())
                && corporateCar.getCap().equals(dto.getCapacity())
                && Objects.equals(corporateCar.getCarType(), dto.getCarType())
                && Objects.equals(corporateCar.getFuel(), dto.getFuel());
    }

    private String uploadCorporateCarImageToS3(String comId, Long carNo, MultipartFile imageFile) {
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

        String key = String.format("%s/%s/corporate-car/%d/%s%s",
                env, comId, carNo, UUID.randomUUID(), ext
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
            log.warn("[corporate-car] S3 delete failed. key={}", objectKey, e);
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
