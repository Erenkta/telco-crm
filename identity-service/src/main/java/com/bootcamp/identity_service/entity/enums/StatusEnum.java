package com.bootcamp.identity_service.entity.enums;

import com.bootcamp.identity_service.exception.InvalidStatusTransitionException;

import java.util.*;

public enum StatusEnum {
    ACTIVE,
    INACTIVE,
    DELETED,
    BLOCKED;

    public final Set<StatusEnum> allowedTransitions(){
     return switch(this){
         case StatusEnum.ACTIVE -> EnumSet.of(StatusEnum.INACTIVE,StatusEnum.DELETED,StatusEnum.BLOCKED);
         case StatusEnum.INACTIVE -> EnumSet.of(StatusEnum.ACTIVE,StatusEnum.DELETED,StatusEnum.BLOCKED);
         case StatusEnum.DELETED ->  EnumSet.noneOf(StatusEnum.class);
         case StatusEnum.BLOCKED -> EnumSet.of(StatusEnum.ACTIVE);
     };
    }

    public boolean canTransitionTo(StatusEnum target) {
        return this == target || allowedTransitions().contains(target);
    }

    public StatusEnum transitionTo(StatusEnum target) {
        if (!canTransitionTo(target)) {
            String message = String.format("Status cannot transition from %s to %s",this,target);
            throw new InvalidStatusTransitionException(message);
        }
        return target;
    }
}
