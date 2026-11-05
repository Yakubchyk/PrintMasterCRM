package com.springboot.printmastercrm.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Расчёт тиснения. Размеры хранятся в сантиметрах. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString
public class PostPress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    @ToString.Exclude
    private Customer customer;

    private int quantity;

    @Column(nullable = false)
    private BigDecimal widthCm;

    @Column(nullable = false)
    private BigDecimal lengthCm;

    /** Фольга, которая расходуется на этот расчёт; по ней резервируется склад. Может быть не указана. */
    @ManyToOne
    @JoinColumn(name = "material_id")
    @ToString.Exclude
    private Material material;

    @Column(nullable = false)
    private BigDecimal totalPrice;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Площадь фольги на весь тираж: размер оттиска × тираж, см². */
    public BigDecimal getFoilAreaCm2() {
        return widthCm.multiply(lengthCm).multiply(BigDecimal.valueOf(quantity));
    }
}
