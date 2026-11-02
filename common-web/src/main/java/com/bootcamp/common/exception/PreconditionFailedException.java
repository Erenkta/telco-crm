package com.bootcamp.common.exception;

import com.bootcamp.common.exception.enums.ExceptionTypeEnum;
import org.springframework.http.HttpStatusCode;

// 412: the client's If-Match version no longer matches the record (someone changed it since the client read it).
public class PreconditionFailedException extends CRMBaseException{
    public PreconditionFailedException(String message) {
        super(message, ExceptionTypeEnum.PRECONDITION_FAILED, HttpStatusCode.valueOf(412));
    }
}
