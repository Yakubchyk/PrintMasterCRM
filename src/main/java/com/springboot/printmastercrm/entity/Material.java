package com.springboot.printmastercrm.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private MaterialCategory category = MaterialCategory.PAPER;

    /** Размер листа, например «70×100 см» или «SRA3». Только для бумаги и картона. */
    private String sheetSize;

    /** Текущий остаток в единицах раздела: листы или см². */
    @Column(nullable = false)
    private BigDecimal stock = BigDecimal.ZERO;

    /** Размер исходного листа магния/латуни, см. Для бумаги не используется (там sheetSize текстом). */
    private BigDecimal sheetWidthCm;

    private BigDecimal sheetLengthCm;

    /** Зарезервировано под оформленные, но ещё не отгруженные заказы. */
    @Column(nullable = false)
    private BigDecimal reserved = BigDecimal.ZERO;

    private String note;

    /** Площадь одного исходного листа, см²; null, если размер не задан. */
    public BigDecimal getSheetAreaCm2() {
        return sheetWidthCm == null || sheetLengthCm == null ? null : sheetWidthCm.multiply(sheetLengthCm);
    }

    /** Свободный остаток: на складе минус резерв. */
    public BigDecimal getAvailable() {
        return stock.subtract(reserved);
    }

    /** Единица учёта определяется разделом. В базе не хранится. */
    public StockUnit getUnit() {
        return category.getUnit();
    }
}
