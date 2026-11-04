package com.springboot.printmastercrm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String username;

    @ToString.Exclude // хэш пароля не должен попадать в логи через toString() Account, Order и Customer
    @Column(nullable = false)
    private String password;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<Role> roles;

    public enum Role implements GrantedAuthority {
        ROLE_MANAGER, ROLE_ADMIN;

        @Override
        public String getAuthority() {
            return name();
        }
    }

}
