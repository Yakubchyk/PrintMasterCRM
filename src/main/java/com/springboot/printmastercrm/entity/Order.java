package com.springboot.printmastercrm.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString
@Table(name = "customer_orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    @ToString.Exclude
    private Customer customer;

    @Column(nullable = false)
    private String product;

    @Column(nullable = false)
    private BigDecimal postpressPrice = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal printingPrice = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal clichePrice = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.NEW;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
