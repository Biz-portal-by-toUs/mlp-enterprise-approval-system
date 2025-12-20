package com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.repository.ReservationCompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.dto.ReqMeetingRoomDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.dto.ResMeetingRoomDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.domain.MeetingRoom;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.repository.MeetingRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final ReservationCompanyRepository companyRepository;

    @Value("${image.image-dir}")  // 서버의 실제 저장 위치
    private String IMAGE_DIR;
    @Value("${image.image-url}")  // 브라우저에서 접근하는 주소
    private String IMAGE_URL;

    public ResMeetingRoomDto getMeetingRoom(Long roomNo, CustomUser user) {

        MeetingRoom meetingRoom = meetingRoomRepository.findById(roomNo)
                .orElseThrow(() -> new CustomException(ErrorCode.MEETING_ROOM_NOT_FOUND));

        // 같은 회사인지 체크
        if (!meetingRoom.getCompany().getComId().equals(user.getComId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        return ResMeetingRoomDto.builder()
                .roomNo(meetingRoom.getRoomNo())
                .roomName(meetingRoom.getRoomName())
                .capacity(meetingRoom.getCap())
                .location(meetingRoom.getLoc())
                .imageUrl(meetingRoom.getImgUrl())
                .equipList(meetingRoom.getEquipList())
                .note(meetingRoom.getNote())
                .build();
    }



    @Transactional(readOnly = true)
    public Page<ResMeetingRoomDto> selectMeetingRoomsWithPaging(String comId, Pageable pageable) {
        // 반환 타입이 Page<ResMeetingRoomDto>인 이유는 데이터 뿐만 아니라 totalPages, totalElements, first/last 같은 페이지 정보도 같이 주려고
        Page<MeetingRoom> meetingRooms = meetingRoomRepository.findByCompany_ComId(comId, pageable);

        // Entity -> Response DTO로 변환 (Page.map()은 Page 형태 유지하면서 내부 요소만 변환)
        return meetingRooms.map(meetingRoom -> ResMeetingRoomDto.builder()
                .roomNo(meetingRoom.getRoomNo())
                .comId(meetingRoom.getCompany().getComId())
                .roomName(meetingRoom.getRoomName())
                .capacity(meetingRoom.getCap())
                .location(meetingRoom.getLoc())
                .imageUrl(meetingRoom.getImgUrl())
                .equipList(meetingRoom.getEquipList())
                .note(meetingRoom.getNote())
                .build());
    }

    public Long registerMeetingRoom(CustomUser user, ReqMeetingRoomDto meetingRoomDto, MultipartFile imageFile) {

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

        if (meetingRoomRepository.existsByCompany_ComIdAndRoomName(
                comId,
                meetingRoomDto.getRoomName()
        )) {
            throw new CustomException(ErrorCode.DUPLICATE_MEETING_ROOM_NAME);
        }

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

        // 요청 DTO 값으로 MeetingRoom 엔티티 생성 (회사 FK 포함)
        MeetingRoom meetingRoom = MeetingRoom.builder()
                .company(company)
                .roomName(meetingRoomDto.getRoomName())
                .cap(meetingRoomDto.getCapacity())
                .loc(meetingRoomDto.getLocation())
                .imgUrl(savedUrl)
                .equipList(meetingRoomDto.getEquipList())
                .note(meetingRoomDto.getNote())
                .build();

        return meetingRoomRepository.save(meetingRoom).getRoomNo();
    }

    public Long updateMeetingRoom(Long roomNo, CustomUser user, ReqMeetingRoomDto meetingRoomDto, MultipartFile imageFile) {

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

        // 수정할 회의실 조회
        MeetingRoom meetingRoom = meetingRoomRepository.findById(roomNo)
                .orElseThrow(() -> new CustomException(ErrorCode.MEETING_ROOM_NOT_FOUND));

        // 같은 회사 데이터인지 검증(보안)
        String comId = user.getComId();
        if (!meetingRoom.getCompany().getComId().equals(comId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        String newName = meetingRoomDto.getRoomName();

        if (!meetingRoom.getRoomName().equals(newName)) {
            // 이름 변경 시에만 중복 검사
            if (meetingRoomRepository.existsByCompany_ComIdAndRoomNameAndRoomNoNot(
                    comId,
                    newName,
                    roomNo
            )) {
                throw new CustomException(ErrorCode.DUPLICATE_MEETING_ROOM_NAME);
            }
        }

        boolean isImageChanged = imageFile != null && !imageFile.isEmpty();
        boolean isInfoChanged = !isSame(meetingRoom, meetingRoomDto);

        if (!isImageChanged && !isInfoChanged) {
            // 변경 없음 → 그냥 바로 리턴
            return meetingRoom.getRoomNo();
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
                meetingRoom.changeImageUrl(savedUrl);
            } catch (IOException e) {
                throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
            }
        }

        // 이미지 외의 필드(이름/인원/위치/장비/비고 등) 업데이트
        meetingRoom.updateInfo(meetingRoomDto);

        return meetingRoom.getRoomNo();
    }

    private boolean isSame(MeetingRoom meetingRoom, ReqMeetingRoomDto dto) {
        return meetingRoom.getRoomName().equals(dto.getRoomName())
                && meetingRoom.getCap().equals(dto.getCapacity())
                && meetingRoom.getLoc().equals(dto.getLocation())
                && Objects.equals(meetingRoom.getEquipList(), dto.getEquipList())
                && Objects.equals(meetingRoom.getNote(), dto.getNote());
    }



}


