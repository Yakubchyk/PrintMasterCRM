package com.springboot.printmastercrm.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Расчёт стоимости печати. */
@Entity
@Table(name = "printing")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Printing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    @ToString.Exclude
    private Customer customer;

    /** Плотность бумаги: 90g/m2, 150g/m2, 200g/m2, 300g/m2. */
    @Column(nullable = false)
    private String papier;

    /** Печать с одной стороны (4+0) или с двух (4+4). */
    @Column(nullable = false)
    private String colour;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal totalCost;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
