package com.springboot.printmastercrm.entity;

/** Единицы учёта на складе: листы и квадратные сантиметры. */
public enum StockUnit {
    SHEET("лист", true),
    CM2("см²", false);

    private final String label;
    private final boolean whole;

    StockUnit(String label, boolean whole) {
        this.label = label;
        this.whole = whole;
    }

    public String getLabel() {
        return label;
    }

    /** true, если количество может быть только целым (листы). */
    public boolean isWhole() {
        return whole;
    }
}
