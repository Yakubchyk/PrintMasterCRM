package com.springboot.printmastercrm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString

public class PostPress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    private int quantity;
    private BigDecimal oneOttiskPrice;
    private BigDecimal montageWorkPrice;
    private BigDecimal oneQuadratMetterFoilPrice;
    private double widthSM;
    private double lengthSM;

    @Transient
    private BigDecimal totalPrice;
}