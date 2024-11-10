package com.springboot.printmastercrm.repository;

import com.springboot.printmastercrm.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
}