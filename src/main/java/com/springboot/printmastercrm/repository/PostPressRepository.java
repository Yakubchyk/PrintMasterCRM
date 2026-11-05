package com.springboot.printmastercrm.repository;

import com.springboot.printmastercrm.entity.PostPress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostPressRepository extends JpaRepository<PostPress, Long> {

    List<PostPress> findByCustomerIdOrderByIdDesc(Long customerId);

    void deleteByCustomerId(Long customerId);

    /** Убирает ссылку на фольгу из расчётов, когда материал удаляют. */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update PostPress p set p.material = null where p.material.id = :materialId")
    void detachMaterial(@org.springframework.data.repository.query.Param("materialId") Long materialId);
}
