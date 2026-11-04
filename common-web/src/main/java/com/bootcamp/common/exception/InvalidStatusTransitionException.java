package com.bootcamp.common.exception;

import com.bootcamp.common.exception.enums.ExceptionTypeEnum;
import org.springframework.http.HttpStatusCode;

public class InvalidStatusTransitionException extends CRMBaseException{
    public InvalidStatusTransitionException(String message) {
        super(message, ExceptionTypeEnum.INVALID_STATE_TRANSITION, HttpStatusCode.valueOf(409));
    }
}
