package com.bootcamp.order_service.exception;

import com.bootcamp.common.exception.CRMBaseException;
import com.bootcamp.common.exception.enums.ExceptionTypeEnum;
import org.springframework.http.HttpStatusCode;

public class CustomerClientNotAvailable extends CRMBaseException {
    public CustomerClientNotAvailable(String message) {
        super(message, ExceptionTypeEnum.CLIENT_NOT_AVAILABLE, HttpStatusCode.valueOf(500));
    }
}
