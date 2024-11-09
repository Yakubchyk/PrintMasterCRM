package com.springboot.printmastercrm.entity;

import jakarta.persistence.*;
import lombok.NonNull;

import java.math.BigDecimal;

@Entity
@Table(name = "stamping")
public class Stamping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    private int quantity; //Количество

    @NonNull
    private BigDecimal oneOttiskPrice; //Стоимость одного оттиска

    @NonNull
    private BigDecimal montageWorkPrice; //Приладка

    @NonNull
    private BigDecimal oneQuadratMetterFoilPrice; //Квадратный метр стоимости фольги

    @NonNull
    private double widthSM; //Ширина

    @NonNull
    private double lengthSM; //Длинна

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

}
