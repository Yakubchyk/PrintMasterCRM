package com.springboot.printmastercrm.repository;

import com.springboot.printmastercrm.entity.Material;
import com.springboot.printmastercrm.entity.MaterialCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    List<Material> findAllByOrderByNameAsc();

    List<Material> findByCategoryOrderByNameAsc(MaterialCategory category);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
