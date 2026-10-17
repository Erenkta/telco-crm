package com.bootcamp.identity_service.exception;

import com.bootcamp.identity_service.exception.enums.ExceptionTypeEnum;
import com.bootcamp.identity_service.exception.enums.ModuleEnum;
import org.springframework.http.HttpStatusCode;

public class RecordAlreadyExistException extends CRMBaseException{
    public RecordAlreadyExistException(String message, ModuleEnum source) {
        super(message, ExceptionTypeEnum.CONFLICT, source, HttpStatusCode.valueOf(409));
    }
    public RecordAlreadyExistException(String message) {
        super(message, ExceptionTypeEnum.CONFLICT, ModuleEnum.IDENTITY_SERVICE, HttpStatusCode.valueOf(409));
    }
}
