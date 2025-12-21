package com.multi.mlpenterpriseapprovalsystem.reservation.corporate_car.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.corporate_car.dto.ReqCorporateCarDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.corporate_car.dto.ResCorporateCarDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.corporate_car.service.CorporateCarService;
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
 * 법인 차량 관리 기능에 대한 REST API 요청을 처리하는 Controller.
 *
 * 법인 차량 관련 데이터 조회 요청을 처리한다.
 * Service 계층을 통해 비즈니스 로직을 수행한다.
 * 처리 결과를 JSON 형태의 법인 차량 DTO 목록으로 반환한다.
 *
 * @author : 송현님
 * @filename : CorporateCarController
 * @since : 2025-12-20 오후 5:30 토요일
 */

@RestController
@Slf4j
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CorporateCarController {

    private final CorporateCarService corporateCarService;

    @GetMapping("/corporate-cars/{carNo}")
    public ResponseEntity<ResponseDto<ResCorporateCarDto>> getCorporateCar(
            @PathVariable Long carNo,
            @AuthenticationPrincipal CustomUser user
    ) {
        ResCorporateCarDto car =
                corporateCarService.getCorporateCar(carNo, user);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "법인 차량 단건 조회 성공", car)
                );
    }

    @GetMapping("/corporate-cars")
    public ResponseEntity<ResponseDto<Map<String, Object>>> getCorporateCarsWithPaging(@AuthenticationPrincipal CustomUser user,
                                                                                       @RequestParam(name = "page", defaultValue = "0") int page,
                                                                                       @RequestParam(name = "size", defaultValue = "6") int size) {  // 한 페이지에서 보여줄 데이터 개수

        Pageable pageable = PageRequest.of(page, size, Sort.by("carName").ascending());

        String comId = user.getComId();
        Page<ResCorporateCarDto> corporateCars = corporateCarService.selectCorporateCarsWithPaging(comId, pageable);

        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a ->
                        a.getAuthority().equals("ROLE_COM_ADMIN") ||
                                a.getAuthority().equals("ROLE_SEC_ADMIN") ||
                                a.getAuthority().equals("ROLE_THR_ADMIN")
                );

        String msg = corporateCars.isEmpty() ? "등록된 법인 차량이 없습니다." : "법인 차량 조회 성공";

        Map<String, Object> result = new HashMap<>();
        result.put("data", corporateCars);
        result.put("isAdmin", isAdmin);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, msg, result));
    }

    @PostMapping(value = "/corporate-cars", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<Long>> registerCorporateCar(@AuthenticationPrincipal CustomUser user,
                                                                  @Valid @ModelAttribute ReqCorporateCarDto corporateCarDto,
                                                                  @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {

        Long carNo = corporateCarService.registerCorporateCar(user, corporateCarDto, imageFile);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto<>(HttpStatus.CREATED, "법인 차량 등록 성공", carNo));
    }

}
