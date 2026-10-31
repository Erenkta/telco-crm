package com.bootcamp.common.exception;

import com.bootcamp.common.exception.enums.ExceptionTypeEnum;
import org.springframework.http.HttpStatusCode;

public class RecordNotAvailableException extends CRMBaseException{
    public RecordNotAvailableException(String message) {
        super(message, ExceptionTypeEnum.NOT_AVAILABLE, HttpStatusCode.valueOf(409));
    }
}
