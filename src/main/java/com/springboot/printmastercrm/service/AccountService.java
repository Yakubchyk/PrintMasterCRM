package com.springboot.printmastercrm.service;



import com.springboot.printmastercrm.entity.Account;
import com.springboot.printmastercrm.repository.AccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AccountService implements UserDetailsService {


    private final AccountRepository accountRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public AccountService(AccountRepository accountRepository, BCryptPasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }


    public boolean isUserExist(String username) {
        return accountRepository.existsByUsername(username);
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Optional<Account> getAccountById(Long id) {
        return accountRepository.findById(id);
    }

    public boolean accountExists(Long id) {
        return accountRepository.existsById(id);
    }

    public void deleteAccountById(Long id) {
        accountRepository.deleteById(id);
    }

    public Account createAccount(Account user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return accountRepository.save(user);
    }

    public Account updateAccount(Long id, Account account) {
        Account existingAccount = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        existingAccount.setRoles(account.getRoles());
        return accountRepository.save(existingAccount);
    }

    public void deleteAccount(Long id) {
        accountRepository.deleteById(id);
    }

    public String getStatistics() {
        long accountCount = accountRepository.count();
        return "Total accounts: " + accountCount;
    }

    public Optional<Account> login(Account account) {
        Optional<Account> foundAccount = accountRepository.findByUsername(account.getUsername());
        if (foundAccount.isPresent()) {
            if (passwordEncoder.matches(account.getPassword(), foundAccount.get().getPassword())) {
                return foundAccount;
            }
        }
        return Optional.empty();
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