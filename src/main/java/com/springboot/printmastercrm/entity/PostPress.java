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

    @Column(unique = true, nullable = false)
    private int quantity;
    @Column(unique = true, nullable = false)
    private BigDecimal oneOttiskPrice;
    @Column(unique = true, nullable = false)
    private BigDecimal montageWorkPrice;
    @Column(unique = true, nullable = false)
    private BigDecimal oneQuadratMetterFoilPrice;
    @Column(unique = true, nullable = false)
    private double widthSM;
    @Column(unique = true, nullable = false)
    private double lengthSM;

    @Transient
    private BigDecimal totalPrice;
}