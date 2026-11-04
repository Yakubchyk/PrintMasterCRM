package com.springboot.printmastercrm.exception;

/**
 * Нарушено бизнес-правило (например, нельзя удалить менеджера, у которого есть клиенты).
 * Сообщение предназначено для показа пользователю.
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
