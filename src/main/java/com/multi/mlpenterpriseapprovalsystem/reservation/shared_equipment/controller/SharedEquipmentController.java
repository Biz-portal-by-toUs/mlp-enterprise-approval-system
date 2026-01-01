package com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.dto.ReqMeetingRoomDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.dto.ReqSharedEquipmentDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.dto.ResSharedEquipmentDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.service.SharedEquipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 공유 설비 관리 기능에 대한 REST API 요청을 처리하는 Controller.
 *
 * 공유 설비 관련 데이터 조회 요청을 처리한다.
 * Service 계층을 통해 비즈니스 로직을 수행한다.
 * 처리 결과를 JSON 형태의 공유 설비 DTO 목록으로 반환한다.
 *
 * @author : 송현님
 * @filename : SharedEquipmentController
 * @since : 2025-12-21 오후 11:32 일요일
 */

@RestController
@Slf4j
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SharedEquipmentController {
    private final SharedEquipmentService sharedEquipmentService;

    @GetMapping("/shared-equipment/{eqNo}")
    public ResponseEntity<ResponseDto<ResSharedEquipmentDto>> getSharedEquipment(
            @PathVariable(name = "eqNo") Long eqNo,
            @AuthenticationPrincipal CustomUser user
    ) {
        ResSharedEquipmentDto eq =
                sharedEquipmentService.getSharedEquipment(eqNo, user);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "공유 설비 단건 조회 성공", eq)
                );
    }

    @GetMapping("/shared-equipment")
    public ResponseEntity<ResponseDto<Map<String, Object>>> getSharedEquipmentWithPaging(@AuthenticationPrincipal CustomUser user,
                                                                                      @RequestParam(name = "page", defaultValue = "0") int page,
                                                                                      @RequestParam(name = "size", defaultValue = "6") int size) {  // 한 페이지에서 보여줄 데이터 개수

        Pageable pageable = PageRequest.of(page, size, Sort.by("eqName").ascending());

        String comId = user.getComId();
        Page<ResSharedEquipmentDto> sharedEquipment = sharedEquipmentService.selectSharedEquipmentWithPaging(comId, pageable);

        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a ->
                        a.getAuthority().equals("ROLE_COM_ADMIN") ||
                                a.getAuthority().equals("ROLE_SEC_ADMIN") ||
                                a.getAuthority().equals("ROLE_THR_ADMIN")
                );

        String msg = sharedEquipment.isEmpty() ? "등록된 공유 설비가 없습니다." : "공유 설비 조회 성공";

        Map<String, Object> result = new HashMap<>();
        result.put("data", sharedEquipment);
        result.put("isAdmin", isAdmin);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, msg, result));
    }

    @PostMapping(value ="/shared-equipment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<Long>> registerSharedEquipment(@AuthenticationPrincipal CustomUser user,
                                                                 @Valid @ModelAttribute ReqSharedEquipmentDto sharedEquipmentDto,
                                                                 @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {

        Long eqNo = sharedEquipmentService.registerSharedEquipment(user, sharedEquipmentDto, imageFile);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto<>(HttpStatus.CREATED, "공유 설비 등록 성공", eqNo));
    }

    @PutMapping(value="/shared-equipment/{eqNo}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<Long>> updateSharedEquipment(@PathVariable(name = "eqNo") Long eqNo,
                                                               @AuthenticationPrincipal CustomUser user,
                                                               @Valid @ModelAttribute ReqSharedEquipmentDto sharedEquipmentDto,
                                                               @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {


        Long updatedEqNo = sharedEquipmentService.updateSharedEquipment(eqNo, user, sharedEquipmentDto, imageFile);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "공유 설비 수정 성공", updatedEqNo));
    }

    @DeleteMapping("/shared-equipment/{eqNo}")
    public ResponseEntity<ResponseDto> deleteSharedEquipment(@PathVariable(name = "eqNo") Long eqNo) {
        sharedEquipmentService.deleteSharedEquipment(eqNo);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "공유 설비 삭제 성공", null));
    }
}
