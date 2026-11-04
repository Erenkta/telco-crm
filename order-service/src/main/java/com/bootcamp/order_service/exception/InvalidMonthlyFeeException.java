package com.bootcamp.order_service.exception;

import com.bootcamp.common.exception.CRMBaseException;
import com.bootcamp.common.exception.enums.ExceptionTypeEnum;
import org.springframework.http.HttpStatusCode;

public class InvalidMonthlyFeeException extends CRMBaseException {
    public InvalidMonthlyFeeException(String message) {
        super(message, ExceptionTypeEnum.INVALID_FEE, HttpStatusCode.valueOf(400));
    }
}
