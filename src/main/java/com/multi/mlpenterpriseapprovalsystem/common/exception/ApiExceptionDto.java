package com.multi.mlpenterpriseapprovalsystem.common.exception;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.http.HttpStatus;

/**
 * ApiExceptionDto
 *
 * @author : 권지영
 * @filename : ApiExceptionDto
 * @since : 2025. 12. 17. 수요일
 */
@Data
@NoArgsConstructor
@ToString
public class ApiExceptionDto {
    // int  형으로 내보내기 위해 200 204
    private int state;
    private String message;

    public ApiExceptionDto(HttpStatus state, String message){
        this.state = state.value();
        this.message = message;
    }

}
