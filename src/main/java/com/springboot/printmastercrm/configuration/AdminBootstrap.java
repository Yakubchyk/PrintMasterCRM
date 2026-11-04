package com.springboot.printmastercrm.configuration;

import com.springboot.printmastercrm.entity.Account;
import com.springboot.printmastercrm.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Создаёт первого администратора при запуске, если в базе ещё нет ни одного.
 * Регистрация через форму создаёт только менеджеров, поэтому без этого
 * в систему нельзя было бы попасть в админ-панель.
 * <p>
 * Задаётся через переменные окружения (или application.yml):
 * APP_BOOTSTRAP_ADMIN_USERNAME, APP_BOOTSTRAP_ADMIN_PASSWORD, APP_BOOTSTRAP_ADMIN_NAME.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.username:}")
    private String username;

    @Value("${app.bootstrap-admin.password:}")
    private String password;

    @Value("${app.bootstrap-admin.name:Administrator}")
    private String name;

    @Override
    public void run(ApplicationArguments args) {
        if (accountRepository.hasAnyWithRole(Account.Role.ROLE_ADMIN)) {
            return;
        }

        if (username.isBlank() || password.isBlank()) {
            log.warn("В базе нет ни одного администратора. Задайте APP_BOOTSTRAP_ADMIN_USERNAME и "
                    + "APP_BOOTSTRAP_ADMIN_PASSWORD, чтобы создать его при запуске.");
            return;
        }

        if (accountRepository.existsByUsername(username) || accountRepository.existsByName(name)) {
            log.warn("Не удалось создать администратора: логин '{}' или имя '{}' уже заняты.", username, name);
            return;
        }

        Account admin = new Account();
        admin.setName(name);
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRoles(new HashSet<>(Set.of(Account.Role.ROLE_ADMIN)));
        accountRepository.save(admin);
        log.info("Создан администратор '{}'.", username);
    }
}
