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

    /** Технологические (инженерные) поля с каждой стороны, см. */
    @Column(nullable = false)
    private BigDecimal marginCm = BigDecimal.ZERO;

    /** Материал клише (магний или латунь); по нему резервируется склад. Может быть не указан. */
    @ManyToOne
    @JoinColumn(name = "material_id")
    @ToString.Exclude
    private Material material;

    @Column(nullable = false)
    private BigDecimal totalPrice;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private BigDecimal addMargins(BigDecimal size) {
        BigDecimal margin = marginCm == null ? BigDecimal.ZERO : marginCm;
        return size.add(margin.multiply(BigDecimal.valueOf(2)));
    }

    /** Общая ширина клише с полями, см. */
    public BigDecimal getTotalWidthCm() {
        return addMargins(widthCm);
    }

    /** Общая длина клише с полями, см. */
    public BigDecimal getTotalLengthCm() {
        return addMargins(lengthCm);
    }

    /** Площадь всех клише расчёта с учётом полей и количества, см². */
    public BigDecimal getTotalAreaCm2() {
        return getTotalWidthCm().multiply(getTotalLengthCm()).multiply(BigDecimal.valueOf(quantity));
    }
}
