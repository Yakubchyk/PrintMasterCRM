package com.springboot.printmastercrm.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Единица, в которой пользователь вводит и видит размеры. В базе размеры всегда хранятся в сантиметрах. */
public enum LengthUnit {
    CM("см", new BigDecimal("1")),
    MM("мм", new BigDecimal("0.1"));

    private final String label;
    private final BigDecimal cmPerUnit;

    LengthUnit(String label, BigDecimal cmPerUnit) {
        this.label = label;
        this.cmPerUnit = cmPerUnit;
    }

    public String getLabel() {
        return label;
    }

    public BigDecimal toCm(BigDecimal value) {
        return value.multiply(cmPerUnit).setScale(3, RoundingMode.HALF_UP);
    }

    /** Размер из базы (см) в выбранных единицах, без лишних нулей: 12.5, 100. */
    public String format(BigDecimal cm) {
        if (cm == null) {
            return "";
        }
        BigDecimal value = cm.divide(cmPerUnit, 3, RoundingMode.HALF_UP).stripTrailingZeros();
        return value.scale() < 0 ? value.setScale(0).toPlainString() : value.toPlainString();
    }
}
