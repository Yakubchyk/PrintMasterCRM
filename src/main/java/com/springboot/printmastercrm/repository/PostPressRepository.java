package com.springboot.printmastercrm.repository;

import com.springboot.printmastercrm.entity.PostPress;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostPressRepository extends JpaRepository<PostPress, Long> {
}