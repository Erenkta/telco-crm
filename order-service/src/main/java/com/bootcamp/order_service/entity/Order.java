package com.bootcamp.order_service.entity;

import com.bootcamp.common.entity.BaseEntity;
import com.bootcamp.order_service.entity.enums.StatusEnum;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;


@Entity
@Table(name = "orders")
@Setter
@Getter
public class Order extends BaseEntity {

    private UUID customerId;
    private String planCode;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Setter(AccessLevel.NONE)
    private StatusEnum status = StatusEnum.CREATED;

    @Version
    private Long version;

    public StatusEnum changeStatus(StatusEnum target){
        this.status = status.transitionTo(target);
        return this.status;
    }
    public void cancel(){
        changeStatus(StatusEnum.CANCELLED);
    }
}
