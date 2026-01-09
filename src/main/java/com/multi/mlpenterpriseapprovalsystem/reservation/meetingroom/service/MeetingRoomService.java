package com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.common.storage.service.S3UrlService;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.company.repository.CompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.dto.ReqMeetingRoomDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.dto.ResMeetingRoomDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.domain.MeetingRoom;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.repository.MeetingRoomRepository;
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
 * 회의실 관리 도메인의 비즈니스 로직을 담당하는 Service.
 * <p>
 * 회의실 정보의 조회, 등록, 수정, 삭제와 관련된 비즈니스 로직을 처리한다.
 * 데이터 접근은 Repository 계층에 위임하며, 도메인 규칙 및 처리 흐름을 관리한다.
 *
 * @author : 송현님
 * @filename : MeetingRoomService
 * @since : 2025-12-16 오후 2:48 화요일
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MeetingRoomService {

    private final MeetingRoomRepository meetingRoomRepository;
    private final CompanyRepository companyRepository;

    private final S3Client s3Client;
    private final S3UrlService s3UrlService;

    @Value("${app.s3.bucket}")
    private String bucket;

    @Value("${app.s3.env}")
    private String env;

    @Transactional(readOnly = true)
    public ResMeetingRoomDto getMeetingRoom(Long roomNo, CustomUser user) {
        MeetingRoom meetingRoom = meetingRoomRepository.findById(roomNo)
                .orElseThrow(() -> new CustomException(ErrorCode.MEETING_ROOM_NOT_FOUND));

        if (!meetingRoom.getCompany().getComId().equals(user.getComId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        return ResMeetingRoomDto.builder()
                .roomNo(meetingRoom.getRoomNo())
                .roomName(meetingRoom.getRoomName())
                .capacity(meetingRoom.getCap())
                .location(meetingRoom.getLoc())
                // ✅ objectKey -> presigned url
                .imageUrl(toImageUrl(meetingRoom.getImgUrl()))
                .equipList(meetingRoom.getEquipList())
                .note(meetingRoom.getNote())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ResMeetingRoomDto> selectMeetingRoomsWithPaging(String comId, Pageable pageable) {
        Page<MeetingRoom> meetingRooms = meetingRoomRepository.findByCompany_ComId(comId, pageable);

        return meetingRooms.map(meetingRoom -> ResMeetingRoomDto.builder()
                .roomNo(meetingRoom.getRoomNo())
                .comId(meetingRoom.getCompany().getComId())
                .roomName(meetingRoom.getRoomName())
                .capacity(meetingRoom.getCap())
                .location(meetingRoom.getLoc())
                // ✅ objectKey -> presigned url
                .imageUrl(toImageUrl(meetingRoom.getImgUrl()))
                .equipList(meetingRoom.getEquipList())
                .note(meetingRoom.getNote())
                .build());
    }

    public Long registerMeetingRoom(CustomUser user, ReqMeetingRoomDto meetingRoomDto, MultipartFile imageFile) {
        boolean isAdmin = isAdmin(user);
        if (!isAdmin) throw new CustomException(ErrorCode.FORBIDDEN);

        String comId = user.getComId();
        Company company = companyRepository.findByComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

        if (meetingRoomRepository.existsByCompany_ComIdAndRoomName(comId, meetingRoomDto.getRoomName())) {
            throw new CustomException(ErrorCode.DUPLICATE_MEETING_ROOM_NAME);
        }

        // 1) 먼저 저장해서 roomNo 확보
        MeetingRoom meetingRoom = MeetingRoom.builder()
                .company(company)
                .roomName(meetingRoomDto.getRoomName())
                .cap(meetingRoomDto.getCapacity())
                .loc(meetingRoomDto.getLocation())
                .imgUrl(null) // ✅ DB에는 objectKey 저장
                .equipList(meetingRoomDto.getEquipList())
                .note(meetingRoomDto.getNote())
                .build();

        MeetingRoom saved = meetingRoomRepository.save(meetingRoom);

        // 2) 이미지 있으면 S3 업로드 후 objectKey 저장
        if (imageFile != null && !imageFile.isEmpty()) {
            String key = uploadMeetingRoomImageToS3(comId, saved.getRoomNo(), imageFile);
            saved.changeImageUrl(key);
        }

        return saved.getRoomNo();
    }

    public Long updateMeetingRoom(Long roomNo, CustomUser user, ReqMeetingRoomDto meetingRoomDto, MultipartFile imageFile) {
        if (!isAdmin(user)) throw new CustomException(ErrorCode.FORBIDDEN);

        MeetingRoom meetingRoom = meetingRoomRepository.findById(roomNo)
                .orElseThrow(() -> new CustomException(ErrorCode.MEETING_ROOM_NOT_FOUND));

        String comId = user.getComId();
        if (!meetingRoom.getCompany().getComId().equals(comId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        String newName = meetingRoomDto.getRoomName();
        if (!meetingRoom.getRoomName().equals(newName)) {
            if (meetingRoomRepository.existsByCompany_ComIdAndRoomNameAndRoomNoNot(comId, newName, roomNo)) {
                throw new CustomException(ErrorCode.DUPLICATE_MEETING_ROOM_NAME);
            }
        }

        boolean isImageChanged = imageFile != null && !imageFile.isEmpty();
        boolean isInfoChanged = !isSame(meetingRoom, meetingRoomDto);

        if (!isImageChanged && !isInfoChanged) return meetingRoom.getRoomNo();

        if (isImageChanged) {
            String oldKey = meetingRoom.getImgUrl();
            String newKey = uploadMeetingRoomImageToS3(comId, meetingRoom.getRoomNo(), imageFile);
            meetingRoom.changeImageUrl(newKey);

            // ✅ 커밋 성공 후에만 oldKey 삭제
            deleteAfterCommit(oldKey);
        }

        meetingRoom.updateInfo(meetingRoomDto);
        return meetingRoom.getRoomNo();
    }

    public void deleteMeetingRoom(Long roomNo) {
        MeetingRoom meetingRoom = meetingRoomRepository.findById(roomNo)
                .orElseThrow(() -> new CustomException(ErrorCode.MEETING_ROOM_NOT_FOUND));

        // 권한 체크(기존 로직 유지)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        CustomUser user = (CustomUser) authentication.getPrincipal();
        if (!isAdmin(user)) throw new CustomException(ErrorCode.FORBIDDEN);

        if (!meetingRoom.getCompany().getComId().equals(user.getComId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        String key = meetingRoom.getImgUrl(); // objectKey
        meetingRoomRepository.delete(meetingRoom);
        deleteAfterCommit(key);
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

    private boolean isSame(MeetingRoom meetingRoom, ReqMeetingRoomDto dto) {
        return meetingRoom.getRoomName().equals(dto.getRoomName())
                && meetingRoom.getCap().equals(dto.getCapacity())
                && meetingRoom.getLoc().equals(dto.getLocation())
                && Objects.equals(meetingRoom.getEquipList(), dto.getEquipList())
                && Objects.equals(meetingRoom.getNote(), dto.getNote());
    }

    private String uploadMeetingRoomImageToS3(String comId, Long roomNo, MultipartFile imageFile) {
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

        String key = String.format("%s/%s/meeting-room/%d/%s%s",
                env, comId, roomNo, UUID.randomUUID(), ext
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

    private void deleteS3ObjectQuietly(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return;

        // ✅ 혹시 과거 데이터가 "http..." URL로 저장돼있다면 삭제 시도 안 함(안전장치)
        if (objectKey.startsWith("http://") || objectKey.startsWith("https://")) return;

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build());
        } catch (Exception e) {
            log.warn("[meeting-room] S3 delete failed. key={}", objectKey, e);
        }
    }

    private String toImageUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return null;

        // ✅ 과거 로컬/정적 URL이 남아있으면 그대로 내려주기(마이그레이션 전에도 깨지지 않게)
        if (objectKey.startsWith("http://") || objectKey.startsWith("https://")) {
            return objectKey;
        }

        return s3UrlService.presignGetUrl(objectKey);
    }

    private void deleteAfterCommit(String oldKey) {
        if (oldKey == null || oldKey.isBlank()) return;

        // 트랜잭션 동기화가 활성일 때만 afterCommit 등록
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // 트랜잭션이 없으면 즉시 삭제(정책에 따라 그냥 return 해도 됨)
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
