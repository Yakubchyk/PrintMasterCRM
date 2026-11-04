package com.springboot.printmastercrm.entity;

public enum MaterialUnit {
    PCS("шт"),
    METER("м"),
    ROLL("рулон"),
    KG("кг");

    private final String label;

    MaterialUnit(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
