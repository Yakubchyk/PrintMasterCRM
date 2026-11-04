package com.springboot.printmastercrm.repository;

import com.springboot.printmastercrm.entity.PostPress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostPressRepository extends JpaRepository<PostPress, Long> {

    List<PostPress> findByCustomerIdOrderByIdDesc(Long customerId);

    void deleteByCustomerId(Long customerId);
}
