package com.bootcamp.order_service.entity;

import com.bootcamp.common.entity.BaseEntity;
import com.bootcamp.order_service.exception.InvalidMonthlyFeeException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "plans",uniqueConstraints = {
        @UniqueConstraint(name = "uk_plans_code",columnNames = {"code"})
})
@Getter
@Setter
@NoArgsConstructor
public class Plan extends BaseEntity {
    @Column(name = "code",nullable = false)
    private String code;

    @Column(name = "monthly_fee",nullable = false)
    @Setter(AccessLevel.NONE)
    private BigDecimal monthlyFee;

    public void setMonthlyFee(BigDecimal monthlyFee) {
        if(monthlyFee == null){
            throw new InvalidMonthlyFeeException("Monthly fee cannot be null");
        }
        // to achieve ck_plans_monthly_fee constraint.
        if(monthlyFee.signum() < 0){
            throw new InvalidMonthlyFeeException("Monthly fee cannot be negative");
        }
        if(!fits(monthlyFee)){
            String message = String.format("Given monthly fee %s is not acceptable.",monthlyFee);
            throw new InvalidMonthlyFeeException(message);
        }
        this.monthlyFee = monthlyFee;
    }

    //To check if the given monthly fee fits our constraint
    private static boolean fits(BigDecimal value) {
        BigDecimal v = value.stripTrailingZeros();
        int fractionDigits = Math.max(v.scale(), 0);
        int integerDigits = Math.max(v.precision() - v.scale(), 0);
        return fractionDigits <= 2 && integerDigits <= 10 - 2;
    }

}
