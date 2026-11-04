package com.springboot.printmastercrm.service;


import com.springboot.printmastercrm.dto.RegistrationForm;
import com.springboot.printmastercrm.entity.Account;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.exception.ExistsException;
import com.springboot.printmastercrm.exception.NotFoundException;
import com.springboot.printmastercrm.repository.AccountRepository;
import com.springboot.printmastercrm.repository.CustomerRepository;
import com.springboot.printmastercrm.repository.OrderRepository;
import com.springboot.printmastercrm.repository.PostPressRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class AccountService implements UserDetailsService {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PostPressRepository postPressRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Публичная регистрация. Всегда создаёт менеджера: роль не берётся из запроса.
     */
    public Account registerManager(RegistrationForm form) {
        if (accountRepository.existsByUsername(form.getUsername())) {
            throw new ExistsException("Пользователь с таким логином уже существует");
        }
        if (accountRepository.existsByName(form.getName())) {
            throw new ExistsException("Пользователь с таким именем уже существует");
        }

        Account account = new Account();
        account.setName(form.getName());
        account.setUsername(form.getUsername());
        account.setPassword(passwordEncoder.encode(form.getPassword()));
        account.setRoles(new HashSet<>(Set.of(Account.Role.ROLE_MANAGER)));

        try {
            return accountRepository.save(account);
        } catch (DataIntegrityViolationException e) {
            // гонка: логин/имя заняли между проверкой и сохранением
            throw new ExistsException("Пользователь с таким логином или именем уже существует", e);
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        // В сессии хранится лёгкий User, а не JPA-сущность: после входа Spring Security
        // сам стирает из него пароль, и хэш не остаётся в памяти и в сессии.
        return User.withUsername(account.getUsername())
                .password(account.getPassword())
                .authorities(account.getRoles())
                .build();
    }

    /**
     * Удаляет менеджера. Раньше метод удалял ещё и клиента с тем же числовым id, что у менеджера.
     * Теперь удаляется только сам аккаунт, и только если за ним не осталось данных:
     * сначала клиентов нужно передать другому менеджеру.
     */
    public void deleteManagerById(Long managerId, String currentUsername) {
        Account account = accountRepository.findById(managerId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        if (account.getUsername().equals(currentUsername)) {
            throw new BusinessRuleException("Нельзя удалить собственную учётную запись");
        }
        if (customerRepository.existsByAccountId(managerId)) {
            throw new BusinessRuleException(
                    "У пользователя «" + account.getUsername() + "» есть клиенты. Сначала передайте их другому менеджеру.");
        }
        if (orderRepository.existsByAccountId(managerId)) {
            throw new BusinessRuleException(
                    "За пользователем «" + account.getUsername() + "» закреплены заказы. Сначала передайте клиентов другому менеджеру.");
        }
        if (postPressRepository.existsByManagerId(managerId)) {
            throw new BusinessRuleException(
                    "За пользователем «" + account.getUsername() + "» закреплены расчёты тиснения.");
        }

        accountRepository.delete(account);
    }

    public Optional<Account> findById(Long id) {
        return accountRepository.findById(id); // Стандартный метод JpaRepository
    }

    public List<Account> findAll(Long id) {
        return accountRepository.findAll();
    }


}
