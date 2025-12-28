package com.multi.mlpenterpriseapprovalsystem.meeting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * fastAPI에게 넘길 objectKey 포함 dto
 *
 * @author : 김승기
 * @filename : ReqMeetingAiRequestDto
 * @since : 2025. 12. 26. 금요일
 */
@Getter
@NoArgsConstructor
public class ReqMeetingAiRequestDto {

    // ✅ S3 objectKey(또는 로컬 저장 경로를 쓴다면 그 경로)
    @NotBlank
    private String objectKey;

    // 프론트가 알고 있으면 같이 보내면 좋아(로깅/검증용)
    private String contentType;
    private String originalName;

    @NotNull
    private Long size;
}