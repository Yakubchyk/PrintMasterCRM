package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Material;
import com.springboot.printmastercrm.entity.MaterialUnit;
import com.springboot.printmastercrm.entity.MovementType;
import com.springboot.printmastercrm.entity.StockMovement;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.exception.ExistsException;
import com.springboot.printmastercrm.exception.NotFoundException;
import com.springboot.printmastercrm.repository.MaterialRepository;
import com.springboot.printmastercrm.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final StockMovementRepository movementRepository;

    public List<Material> findAll() {
        return materialRepository.findAllByOrderByNameAsc();
    }

    public Material get(Long id) {
        return materialRepository.findById(id).orElseThrow(() -> new NotFoundException("Материал не найден"));
    }

    public List<StockMovement> movements(Long materialId) {
        return movementRepository.findByMaterialIdOrderByIdDesc(materialId);
    }

    /** Создаёт материал; ненулевой начальный остаток записывается как приход. */
    @Transactional
    public Material create(String name, MaterialUnit unit, BigDecimal initialStock, String note) {
        String trimmed = requireName(name);
        if (materialRepository.existsByName(trimmed)) {
            throw new ExistsException("Материал с таким названием уже есть");
        }
        if (initialStock != null && initialStock.signum() < 0) {
            throw new BusinessRuleException("Остаток не может быть отрицательным");
        }
        Material material = new Material();
        material.setName(trimmed);
        material.setUnit(unit == null ? MaterialUnit.PCS : unit);
        material.setNote(clean(note));
        material = materialRepository.save(material);
        if (initialStock != null && initialStock.signum() > 0) {
            receive(material.getId(), initialStock, "Начальный остаток");
        }
        return material;
    }

    @Transactional
    public Material update(Long id, String name, MaterialUnit unit, String note) {
        Material material = get(id);
        String trimmed = requireName(name);
        if (materialRepository.existsByNameAndIdNot(trimmed, id)) {
            throw new ExistsException("Материал с таким названием уже есть");
        }
        material.setName(trimmed);
        if (unit != null) {
            material.setUnit(unit);
        }
        material.setNote(clean(note));
        return materialRepository.save(material);
    }

    @Transactional
    public void receive(Long id, BigDecimal quantity, String note) {
        move(id, MovementType.IN, quantity, note);
    }

    @Transactional
    public void consume(Long id, BigDecimal quantity, String note) {
        move(id, MovementType.OUT, quantity, note);
    }

    /** Удаляет материал вместе с историей движения. */
    @Transactional
    public void delete(Long id) {
        Material material = get(id);
        movementRepository.deleteByMaterialId(id);
        materialRepository.delete(material);
    }

    private void move(Long id, MovementType type, BigDecimal quantity, String note) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessRuleException("Количество должно быть больше нуля");
        }
        BigDecimal amount = quantity.setScale(3, RoundingMode.HALF_UP);
        Material material = get(id);
        BigDecimal newStock = type == MovementType.IN ? material.getStock().add(amount) : material.getStock().subtract(amount);
        if (newStock.signum() < 0) {
            throw new BusinessRuleException("На складе недостаточно материала «" + material.getName()
                    + "»: осталось " + material.getStock().stripTrailingZeros().toPlainString()
                    + " " + material.getUnit().getLabel());
        }
        material.setStock(newStock);
        materialRepository.save(material);

        StockMovement movement = new StockMovement();
        movement.setMaterial(material);
        movement.setMovementType(type);
        movement.setQuantity(amount);
        movement.setNote(clean(note));
        movementRepository.save(movement);
    }

    private static String requireName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessRuleException("Укажите название материала");
        }
        return trimmed;
    }

    private static String clean(String note) {
        return note == null || note.isBlank() ? null : note.trim();
    }
}
