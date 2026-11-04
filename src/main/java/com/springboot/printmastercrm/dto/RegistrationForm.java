package com.springboot.printmastercrm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Данные формы регистрации. Намеренно НЕ содержит id и роли:
 * роль назначается только на сервере, id генерирует база данных.
 */
@Getter
@Setter
public class RegistrationForm {

    @NotBlank(message = "Укажите имя")
    @Size(max = 255, message = "Не более 255 символов")
    private String name;

    @NotBlank(message = "Укажите логин")
    @Size(max = 255, message = "Не более 255 символов")
    private String username;

    @NotBlank(message = "Укажите пароль")
    @Size(min = 8, max = 72, message = "Пароль должен содержать от 8 до 72 символов")
    private String password;
}
