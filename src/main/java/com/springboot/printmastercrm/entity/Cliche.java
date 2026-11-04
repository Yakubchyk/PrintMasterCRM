package com.springboot.printmastercrm.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Расчёт стоимости клише. Размеры хранятся в сантиметрах. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Cliche {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    @ToString.Exclude
    private Customer customer;

    /** Количество клише. */
    private int quantity;

    @Column(nullable = false)
    private BigDecimal widthCm;

    @Column(nullable = false)
    private BigDecimal lengthCm;

    @Column(nullable = false)
    private BigDecimal totalPrice;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
