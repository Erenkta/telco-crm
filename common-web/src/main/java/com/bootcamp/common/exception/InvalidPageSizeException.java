package com.bootcamp.common.exception;

import com.bootcamp.common.exception.enums.ExceptionTypeEnum;
import org.springframework.http.HttpStatusCode;

public class InvalidPageSizeException extends CRMBaseException{
    public InvalidPageSizeException(String message) {
        super(message, ExceptionTypeEnum.BAD_PAGE_PARAMETER ,HttpStatusCode.valueOf(400));
    }
}
