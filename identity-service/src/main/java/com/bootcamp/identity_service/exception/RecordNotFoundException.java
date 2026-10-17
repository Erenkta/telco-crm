package com.bootcamp.identity_service.exception;

import com.bootcamp.identity_service.exception.enums.ExceptionTypeEnum;
import com.bootcamp.identity_service.exception.enums.ModuleEnum;
import org.springframework.http.HttpStatusCode;

public class RecordNotFoundException extends CRMBaseException{
    public RecordNotFoundException(String message) {
        super(message, ExceptionTypeEnum.NOT_FOUND, ModuleEnum.IDENTITY_SERVICE, HttpStatusCode.valueOf(404));
    }
    public RecordNotFoundException(String message,ModuleEnum source) {
        super(message, ExceptionTypeEnum.NOT_FOUND, source, HttpStatusCode.valueOf(404));
    }
}
