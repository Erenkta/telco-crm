package com.bootcamp.common.exception;

import com.bootcamp.common.exception.enums.ExceptionTypeEnum;
import org.springframework.http.HttpStatusCode;

public class RecordNotFoundException extends CRMBaseException{
    public RecordNotFoundException(String message) {
        super(message, ExceptionTypeEnum.NOT_FOUND, HttpStatusCode.valueOf(404));
    }
}
