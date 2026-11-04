package com.springboot.printmastercrm.entity;

public enum OrderStatus {
    NEW("Новый"),
    IN_PROGRESS("В работе"),
    READY("Готов"),
    SHIPPED("Отгружен");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
