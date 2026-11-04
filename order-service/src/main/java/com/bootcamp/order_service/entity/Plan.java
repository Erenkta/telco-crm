package com.bootcamp.order_service.entity;

import com.bootcamp.common.entity.BaseEntity;
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

    private String name;

    @Column(name = "monthly_fee",nullable = false)
    private BigDecimal monthlyFee;
}
