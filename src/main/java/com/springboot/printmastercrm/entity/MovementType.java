package com.springboot.printmastercrm.entity;

public enum MovementType {
    IN("Приход"),
    OUT("Расход");

    private final String label;

    MovementType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
