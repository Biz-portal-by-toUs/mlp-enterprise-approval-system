package com.multi.mlpenterpriseapprovalsystem.documentform.attachment.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.*;
import com.multi.mlpenterpriseapprovalsystem.documentform.attachment.dto.req.*;
import com.multi.mlpenterpriseapprovalsystem.documentform.attachment.dto.res.*;
import org.springframework.data.domain.*;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : AttachBoxService
 * @since : 2026-01-10 토요일
 */

public interface AttachBoxService {

    Long create(ReqAttachCreateDto req, CustomUser user);

    Page<ResAttachListDto> list(Pageable pageable, CustomUser user);

    ResAttachDetailDto detail(Long attachNo, CustomUser user);

    ResAttachDelDto delete(Long attachNo, CustomUser user);
}