package com.bootcamp.order_service.entity.enums;

import com.bootcamp.common.exception.InvalidStatusTransitionException;

import java.util.EnumSet;
import java.util.Set;

public enum StatusEnum {
    CREATED,
    CANCELLED;


    public Set<StatusEnum> getAllowed() {
       return switch (this){
            case StatusEnum.CREATED -> EnumSet.of(StatusEnum.CANCELLED);
            default -> EnumSet.noneOf(StatusEnum.class);
       };
    }

    public StatusEnum transitionTo(StatusEnum target){
        if(!getAllowed().contains(target)){
            String message = String.format("Status cannot translate from %s to %s",this,target);
            throw new InvalidStatusTransitionException(message);
        }
        return target;
    }

}
