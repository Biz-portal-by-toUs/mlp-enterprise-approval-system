package com.multi.mlpenterpriseapprovalsystem.meeting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ai 콜백 받을 dto
 *
 * @author : 김승기
 * @filename : ReqMeetingAiCallbackDto
 * @since : 2025. 12. 26. 금요일
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReqMeetingAiCallbackDto {

    @NotNull
    @JsonProperty("meetNo")
    private Long meetNo;

    private String objectKey;

    // FastAPI 결과
    private String sttText;
    private String aiText;

    private String status;
    private String errorMessage;
}