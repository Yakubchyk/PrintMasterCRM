package com.springboot.printmastercrm.repository;

import com.springboot.printmastercrm.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByName(String name);

    @Query("select count(a) > 0 from Account a join a.roles r where r = :role")
    boolean hasAnyWithRole(@Param("role") Account.Role role);
}
