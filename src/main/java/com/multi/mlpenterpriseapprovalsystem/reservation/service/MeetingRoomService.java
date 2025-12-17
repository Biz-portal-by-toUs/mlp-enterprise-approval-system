package com.multi.mlpenterpriseapprovalsystem.reservation.service;

import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.reservation.repository.CompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.reservation.dto.ReqMeetingRoomDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.dto.ResMeetingRoomDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.register.domain.MeetingRoom;
import com.multi.mlpenterpriseapprovalsystem.reservation.repository.MeetingRoomRepository;
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
import java.util.Optional;
import java.util.UUID;

/**
 * 회의실 관리 도메인의 비즈니스 로직을 담당하는 Service.
 *
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

    @Value("${image.image-dir}")  // 서버의 실제 저장 위치
    private String IMAGE_DIR;
    @Value("${image.image-url}")  // 브라우저에서 접근하는 주소
    private String IMAGE_URL;

    public Page<ResMeetingRoomDto> selectMeetingRoomsWithPaging(String comId, Pageable pageable) {
    // 반환 타입이 Page<ResMeetingRoomDto>인 이유는 데이터 뿐만 아니라 totalPages, totalElements, first/last 같은 페이지 정보도 같이 주려고
        Page<MeetingRoom> meetingRooms = meetingRoomRepository.findByCompany_ComId(comId, pageable);

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

    @Transactional
    public Long registerMeetingRoom(String comId, ReqMeetingRoomDto meetingRoomDto, MultipartFile imageFile) throws IOException {

        Company company = (Company) companyRepository.findByComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

        String savedUrl = null;

        if (imageFile != null && !imageFile.isEmpty()) {
            // 1) 파일명 생성
            String ext = Optional.ofNullable(imageFile.getOriginalFilename())
                    .filter(f -> f.contains("."))
                    .map(f -> f.substring(f.lastIndexOf(".")))
                    .orElse("");

            String fileName = UUID.randomUUID() + ext;

            // 2) 저장 경로 (yml의 image.image-dir 사용)
            Path savePath = Paths.get(IMAGE_DIR).resolve(fileName);  // imageDir = C:/.../uploads
            Files.createDirectories(savePath.getParent());
            imageFile.transferTo(savePath.toFile());

            // 3) 접근 URL (yml의 image.image-url 사용)
            savedUrl = IMAGE_URL + "/" + fileName; // imageUrl = http://localhost:8090/uploads
        }


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
}

