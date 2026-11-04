package com.bootcamp.order_service.entity;

import com.bootcamp.common.entity.BaseEntity;
import com.bootcamp.order_service.entity.enums.StatusEnum;
import com.bootcamp.order_service.exception.InvalidMonthlyFeeException;
import com.bootcamp.order_service.exception.InvalidOrderAmountException;
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

    @Setter(AccessLevel.NONE)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private StatusEnum status;

    private void setAmount(BigDecimal amount) {
        if(amount == null){
            throw new InvalidOrderAmountException("Amount cannot be null");
        }
        // to achieve ck_orders_amount constraint.
        if(amount.signum() < 0){
            throw new InvalidOrderAmountException("Amount cannot be negative");
        }
        if(!fits(amount)){
            String message = String.format("Given amount %s is not acceptable.",amount);
            throw new InvalidOrderAmountException(message);
        }
        this.amount = amount;
    }


    //To check if the given amount fits our constraint
    private static boolean fits(BigDecimal value) {
        BigDecimal v = value.stripTrailingZeros();
        int fractionDigits = Math.max(v.scale(), 0);
        int integerDigits = Math.max(v.precision() - v.scale(), 0);
        return fractionDigits <= 2 && integerDigits <= 10 - 2;
    }


}
