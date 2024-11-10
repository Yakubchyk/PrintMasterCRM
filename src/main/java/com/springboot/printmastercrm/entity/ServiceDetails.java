package com.springboot.printmastercrm.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Embeddable
@Data
public class ServiceDetails {

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private BigDecimal cost;
}