package com.bootcamp.identity_service.exception;

import com.bootcamp.identity_service.exception.enums.ExceptionTypeEnum;
import com.bootcamp.identity_service.exception.enums.ModuleEnum;
import org.springframework.http.HttpStatusCode;

public class RecordNotAvailableException extends CRMBaseException{
    public RecordNotAvailableException(String message) {
        super(message, ExceptionTypeEnum.NOT_AVAILABLE,ModuleEnum.IDENTITY_SERVICE, HttpStatusCode.valueOf(409));
    }
}
