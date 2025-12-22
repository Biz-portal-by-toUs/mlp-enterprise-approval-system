package com.multi.mlpenterpriseapprovalsystem.common.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 파일 저장 dto
 *
 * @author : 권지영
 * @filename : StoredFile
 * @since : 2025. 12. 19. 금요일
 */
@Getter
@AllArgsConstructor
public class StoredFile {

    private final String url;
    private final String path;
}
