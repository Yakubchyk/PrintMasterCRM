package com.springboot.printmastercrm.dto;

import lombok.*;
import jakarta.validation.constraints.NotBlank;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class LoginAccountDto {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
}