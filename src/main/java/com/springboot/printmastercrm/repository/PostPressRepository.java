package com.springboot.printmastercrm.repository;

import com.springboot.printmastercrm.entity.PostPress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostPressRepository extends JpaRepository<PostPress, Long> {
    List<PostPress> findByCustomerId(Long customerId);

    void deleteById(Long id);
}