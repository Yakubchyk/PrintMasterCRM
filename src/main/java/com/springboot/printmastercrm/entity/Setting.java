package com.springboot.printmastercrm.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

/** Единственная строка настроек (id = 1): цены, единицы измерения и коэффициент. */
@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Setting {

    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private LengthUnit lengthUnit = LengthUnit.CM;

    /** Приладка (одноразовая сумма на тиснение). */
    @Column(nullable = false)
    private BigDecimal montageWorkPrice = BigDecimal.ZERO;

    /** Цена одного оттиска. */
    @Column(nullable = false)
    private BigDecimal oneOttiskPrice = BigDecimal.ZERO;

    /** Цена фольги за единицу площади foilAreaUnit. */
    @Column(nullable = false)
    private BigDecimal foilPrice = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private AreaUnit foilAreaUnit = AreaUnit.M2;

    /** Базовая цена одного оттиска при печати (за один лист, одна сторона, бумага 90 г). */
    @Column(nullable = false)
    private BigDecimal pricePrint = BigDecimal.ZERO;

    /** Цена клише за единицу площади clicheAreaUnit. */
    @Column(nullable = false)
    private BigDecimal clichePrice = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private AreaUnit clicheAreaUnit = AreaUnit.CM2;

    /** Минимальная стоимость одного клише. */
    @Column(nullable = false)
    private BigDecimal clicheMinPrice = BigDecimal.ZERO;

    /** Коэффициент, на который умножается итог расчёта тиснения (1.25 = +25%). Печать и клише без него. */
    @Column(nullable = false)
    private BigDecimal markup = new BigDecimal("1.25");
}
