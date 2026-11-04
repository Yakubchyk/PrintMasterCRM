package com.springboot.printmastercrm.repository;

import com.springboot.printmastercrm.entity.OrderMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OrderMaterialRepository extends JpaRepository<OrderMaterial, Long> {

    List<OrderMaterial> findByOrderId(Long orderId);

    List<OrderMaterial> findByOrderIdIn(Collection<Long> orderIds);

    boolean existsByMaterialId(Long materialId);
}
