package com.bootcamp.common.exception;

import com.bootcamp.common.exception.enums.ExceptionTypeEnum;
import org.springframework.http.HttpStatusCode;

// 428: the endpoint only accepts conditional requests, but no If-Match header was sent.
public class PreconditionRequiredException extends CRMBaseException{
    public PreconditionRequiredException(String message) {
        super(message, ExceptionTypeEnum.PRECONDITION_REQUIRED, HttpStatusCode.valueOf(428));
    }
}
