package com.springboot.printmastercrm.entity;

/** Тип фольги для тиснения. */
public enum FoilType {
    GOLD("Золото"),
    SILVER("Серебро"),
    MATTE_GOLD("Матовое золото"),
    MATTE_SILVER("Матовое серебро"),
    COLOR("Цветная фольга");

    private final String label;

    FoilType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
