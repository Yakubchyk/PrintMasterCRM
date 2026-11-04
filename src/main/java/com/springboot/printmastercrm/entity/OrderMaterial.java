package com.springboot.printmastercrm.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/** Сколько материала списано со склада на заказ. */
@Entity
@Table(name = "order_material")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class OrderMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    @ToString.Exclude
    private Order order;

    @ManyToOne(optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    @ToString.Exclude
    private Material material;

    @Column(nullable = false)
    private BigDecimal quantity;

    /** false — материал только зарезервирован под заказ; true — списан со склада при отгрузке. */
    @Column(nullable = false)
    private boolean writtenOff = false;
}
