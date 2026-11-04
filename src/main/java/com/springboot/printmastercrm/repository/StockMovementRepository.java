package com.springboot.printmastercrm.repository;

import com.springboot.printmastercrm.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByMaterialIdOrderByIdDesc(Long materialId);

    void deleteByMaterialId(Long materialId);
}
