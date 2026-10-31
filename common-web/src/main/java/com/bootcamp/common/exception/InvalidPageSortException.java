package com.bootcamp.common.exception;

import com.bootcamp.common.exception.enums.ExceptionTypeEnum;
import org.springframework.http.HttpStatusCode;

public class InvalidPageSortException extends CRMBaseException{
    public InvalidPageSortException(String message) {
        super(message, ExceptionTypeEnum.BAD_PAGE_PARAMETER, HttpStatusCode.valueOf(400));
    }
}
