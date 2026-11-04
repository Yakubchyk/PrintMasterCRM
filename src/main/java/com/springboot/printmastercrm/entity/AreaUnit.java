package com.springboot.printmastercrm.entity;

import java.math.BigDecimal;

/** Единица площади, к которой привязана цена (фольги или клише). */
public enum AreaUnit {
    CM2("см²", new BigDecimal("1")),
    M2("м²", new BigDecimal("10000"));

    private final String label;
    private final BigDecimal cm2PerUnit;

    AreaUnit(String label, BigDecimal cm2PerUnit) {
        this.label = label;
        this.cm2PerUnit = cm2PerUnit;
    }

    public String getLabel() {
        return label;
    }

    /** Сколько квадратных сантиметров в одной единице. */
    public BigDecimal getCm2PerUnit() {
        return cm2PerUnit;
    }
}
