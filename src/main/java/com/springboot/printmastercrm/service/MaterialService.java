package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Material;
import com.springboot.printmastercrm.entity.MaterialCategory;
import com.springboot.printmastercrm.entity.MovementType;
import com.springboot.printmastercrm.entity.StockMovement;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.exception.ExistsException;
import com.springboot.printmastercrm.exception.NotFoundException;
import com.springboot.printmastercrm.repository.MaterialRepository;
import com.springboot.printmastercrm.repository.OrderMaterialRepository;
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
    private final OrderMaterialRepository orderMaterialRepository;

    public List<Material> findAll() {
        return materialRepository.findAllByOrderByNameAsc();
    }

    public List<Material> findByCategory(MaterialCategory category) {
        return materialRepository.findByCategoryOrderByNameAsc(category);
    }

    public Material get(Long id) {
        return materialRepository.findById(id).orElseThrow(() -> new NotFoundException("Материал не найден"));
    }

    public List<StockMovement> movements(Long materialId) {
        return movementRepository.findByMaterialIdOrderByIdDesc(materialId);
    }

    /** Создаёт материал в разделе; ненулевой начальный остаток записывается как приход. */
    @Transactional
    public Material create(String name, MaterialCategory category, String sheetSize,
                           BigDecimal initialStock, String note) {
        if (category == null) {
            throw new BusinessRuleException("Выберите раздел склада");
        }
        String trimmed = requireName(name);
        if (materialRepository.existsByName(trimmed)) {
            throw new ExistsException("Материал с таким названием уже есть");
        }
        if (initialStock != null && initialStock.signum() < 0) {
            throw new BusinessRuleException("Остаток не может быть отрицательным");
        }
        Material material = new Material();
        material.setName(trimmed);
        material.setCategory(category);
        material.setSheetSize(sheetSizeFor(category, sheetSize));
        material.setNote(clean(note));
        material = materialRepository.save(material);
        if (initialStock != null && initialStock.signum() > 0) {
            receive(material.getId(), initialStock, "Начальный остаток");
        }
        return material;
    }

    /** Раздел материала не меняется: от него зависит единица учёта. */
    @Transactional
    public Material update(Long id, String name, String sheetSize, String note) {
        Material material = get(id);
        String trimmed = requireName(name);
        if (materialRepository.existsByNameAndIdNot(trimmed, id)) {
            throw new ExistsException("Материал с таким названием уже есть");
        }
        material.setName(trimmed);
        material.setSheetSize(sheetSizeFor(material.getCategory(), sheetSize));
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

    /** Удаляет материал вместе с историей движения, если он не использован в заказах. */
    @Transactional
    public void delete(Long id) {
        Material material = get(id);
        if (orderMaterialRepository.existsByMaterialId(id)) {
            throw new BusinessRuleException("Материал «" + material.getName()
                    + "» списывался на заказы, удалить его нельзя");
        }
        movementRepository.deleteByMaterialId(id);
        materialRepository.delete(material);
    }

    private void move(Long id, MovementType type, BigDecimal quantity, String note) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessRuleException("Количество должно быть больше нуля");
        }
        Material material = get(id);
        if (material.getUnit().isWhole() && quantity.stripTrailingZeros().scale() > 0) {
            throw new BusinessRuleException("«" + material.getName() + "»: листы указываются целым числом");
        }
        BigDecimal amount = quantity.setScale(3, RoundingMode.HALF_UP);
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

    private static String sheetSizeFor(MaterialCategory category, String sheetSize) {
        return category == MaterialCategory.PAPER ? clean(sheetSize) : null;
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
