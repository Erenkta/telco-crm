package com.bootcamp.common.exception;

import com.bootcamp.common.exception.enums.ExceptionTypeEnum;
import org.springframework.http.HttpStatusCode;

public class RecordAlreadyExistException extends CRMBaseException{
    public RecordAlreadyExistException(String message) {
        super(message, ExceptionTypeEnum.CONFLICT,HttpStatusCode.valueOf(409));
    }
}
