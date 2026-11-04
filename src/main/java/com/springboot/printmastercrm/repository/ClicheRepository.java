package com.springboot.printmastercrm.repository;

import com.springboot.printmastercrm.entity.Cliche;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClicheRepository extends JpaRepository<Cliche, Long> {

    List<Cliche> findByCustomerIdOrderByIdDesc(Long customerId);

    void deleteByCustomerId(Long customerId);

    /** Убирает ссылку на материал из расчётов, когда материал удаляют. */
    @Modifying
    @Query("update Cliche c set c.material = null where c.material.id = :materialId")
    void detachMaterial(@Param("materialId") Long materialId);
}
