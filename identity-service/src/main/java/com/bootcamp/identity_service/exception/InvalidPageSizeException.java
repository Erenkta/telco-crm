package com.bootcamp.identity_service.exception;

import com.bootcamp.identity_service.exception.enums.ExceptionTypeEnum;
import com.bootcamp.identity_service.exception.enums.ModuleEnum;
import org.springframework.http.HttpStatusCode;

public class InvalidPageSizeException extends CRMBaseException{
    public InvalidPageSizeException(String message) {
        super(message, ExceptionTypeEnum.BAD_PAGE_PARAMETER, ModuleEnum.IDENTITY_SERVICE, HttpStatusCode.valueOf(400));
    }
}
