package com.bootcamp.common.exception;

import com.bootcamp.common.exception.enums.ExceptionTypeEnum;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;

@Getter
public class CRMBaseException extends RuntimeException {
    private final ExceptionTypeEnum type;
    private final HttpStatusCode statusCode;
    protected CRMBaseException(String message,
                               ExceptionTypeEnum type,
                               HttpStatusCode statusCode) {
        super(message);
        this.type = type;
        this.statusCode = statusCode;
    }


}
