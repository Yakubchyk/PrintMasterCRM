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

    private BigDecimal montageWorkPrice = BigDecimal.ZERO; // Стоимость приладки
    private BigDecimal oneOttiskPrice = BigDecimal.ZERO;  // Стоимость оттиска
    private BigDecimal oneQuadratMetterFoilPrice = BigDecimal.ZERO; //стоимость одного квадратного метра фольги

    private BigDecimal pricePrint = BigDecimal.ZERO;
}
