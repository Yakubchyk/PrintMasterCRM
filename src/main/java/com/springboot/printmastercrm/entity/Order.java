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
@Table(name = "customer_orders")
public class Order {
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "manager_id", nullable = false)
    private Account account;

    @Column(nullable = false)
    private String product;

    @Column(nullable = false)
    private BigDecimal postpressPrice;

    @Column(nullable = false)
    private BigDecimal totalCost;
}