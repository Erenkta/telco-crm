package com.bootcamp.identity_service.exception;

import com.bootcamp.identity_service.exception.enums.ExceptionTypeEnum;
import com.bootcamp.identity_service.exception.enums.ModuleEnum;
import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class CRMBaseException extends RuntimeException {
    private final ExceptionTypeEnum type;
    private final ModuleEnum source;
    private final HttpStatusCode statusCode;

    protected CRMBaseException(String message, ExceptionTypeEnum type, ModuleEnum source,HttpStatusCode statusCode) {
        super(message);
        this.type = type;
        this.source = source;
        this.statusCode = statusCode;
    }

    public String generateExceptionTitle(){
        return type.name() +" error occurred in "+source.name();
    }
    public String generateExceptionSummary(){
        return generateExceptionTitle()+". Exception Details: "+getMessage();
    }
}
