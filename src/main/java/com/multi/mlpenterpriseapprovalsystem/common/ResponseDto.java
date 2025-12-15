package com.multi.mlpenterpriseapprovalsystem.common;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@NoArgsConstructor
public class ResponseDto {

    private int status;
    private String message;
    private Object data;

    public ResponseDto(HttpStatus status, String message, Object data){
        this.status = status.value();
        this.message = message;
        this.data = data;
    }

}
