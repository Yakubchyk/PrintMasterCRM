package com.springboot.printmastercrm.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Entity
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String managerUsername;

    public enum Role {
        ROLE_CUSTOMER
    }

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
}