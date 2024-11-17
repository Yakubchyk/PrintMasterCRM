package com.springboot.printmastercrm.service;


import com.springboot.printmastercrm.entity.Account;
import com.springboot.printmastercrm.repository.AccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class AccountService implements UserDetailsService {

    @Autowired
    private AccountRepository accountRepository;

    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public void save(Account account) {
        account.setPassword(new BCryptPasswordEncoder(11).encode(account.getPassword()));
        account.setRoles(Set.of(Account.Role.ROLE_MANAGER));
        accountRepository.save(account);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void saveAdmin(Account account) {
        account.setPassword(new BCryptPasswordEncoder(11).encode(account.getPassword()));
        account.setRoles(Set.of(Account.Role.ROLE_ADMIN));
        accountRepository.save(account);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<Account> byUsername = accountRepository.findByUsername(username);

        if (byUsername.isPresent()) {
            return byUsername.get();
        }

        throw new UsernameNotFoundException(username);
    }
}
