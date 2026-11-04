package com.bootcamp.order_service.exception;

import com.bootcamp.common.exception.CRMBaseException;
import com.bootcamp.common.exception.enums.ExceptionTypeEnum;
import org.springframework.http.HttpStatusCode;

public class InvalidOrderAmountException extends CRMBaseException {
    public InvalidOrderAmountException(String message) {
        super(message, ExceptionTypeEnum.INVALID_AMOUNT, HttpStatusCode.valueOf(400));
    }
}
