package com.bootcamp.order_service.exception;

import com.bootcamp.common.exception.CRMBaseException;
import com.bootcamp.common.exception.enums.ExceptionTypeEnum;
import org.springframework.http.HttpStatusCode;

public class RemoteResourceNotFoundException extends CRMBaseException {
    public RemoteResourceNotFoundException(String message) {
        super(message, ExceptionTypeEnum.CLIENT_REQUEST_NOT_VALID, HttpStatusCode.valueOf(422));
    }
}
