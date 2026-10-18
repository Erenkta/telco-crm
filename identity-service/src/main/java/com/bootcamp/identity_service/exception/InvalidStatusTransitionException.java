package com.bootcamp.identity_service.exception;

import com.bootcamp.identity_service.exception.enums.ExceptionTypeEnum;
import com.bootcamp.identity_service.exception.enums.ModuleEnum;
import org.springframework.http.HttpStatusCode;

public class InvalidStatusTransitionException extends CRMBaseException{
    public InvalidStatusTransitionException(String message) {
        super(message, ExceptionTypeEnum.INVALID_STATE_TRANSITION, ModuleEnum.IDENTITY_SERVICE, HttpStatusCode.valueOf(409));
    }
}
