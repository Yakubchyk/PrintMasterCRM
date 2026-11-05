package com.springboot.printmastercrm.entity;

/** Раздел склада. Единица учёта определяется разделом. */
public enum MaterialCategory {
    PAPER("Бумага и картон", StockUnit.SHEET),
    MAGNESIUM("Клише: магний", StockUnit.CM2),
    BRASS("Клише: латунь", StockUnit.CM2),
    FOIL("Фольга для тиснения", StockUnit.CM2);

    private final String label;
    private final StockUnit unit;

    MaterialCategory(String label, StockUnit unit) {
        this.label = label;
        this.unit = unit;
    }

    public String getLabel() {
        return label;
    }

    public StockUnit getUnit() {
        return unit;
    }
}
