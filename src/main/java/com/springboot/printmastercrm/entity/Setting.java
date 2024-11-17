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
public class Setting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)

    private double montageWorkPrice; // Стоимость приладки
    private double oneOttiskPrice;   // Стоимость оттиска
    private double oneQuadratMetterFoilPrice; //стоимость одного квадратного метра фольги
}
