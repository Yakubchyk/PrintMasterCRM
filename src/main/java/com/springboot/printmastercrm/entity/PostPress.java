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

    @ManyToOne
    @JoinColumn(name = "manager_id") // Связь с таблицей Account
    private Account manager; // Поле для назначения менеджера

    private int quantity;
    private double widthSM;
    private double lengthSM;
    private BigDecimal totalPrice;

}