package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.FoilType;
import com.springboot.printmastercrm.entity.Material;
import com.springboot.printmastercrm.entity.MaterialCategory;
import com.springboot.printmastercrm.entity.MovementType;
import com.springboot.printmastercrm.entity.StockMovement;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.exception.ExistsException;
import com.springboot.printmastercrm.exception.NotFoundException;
import com.springboot.printmastercrm.repository.ClicheRepository;
import com.springboot.printmastercrm.repository.MaterialRepository;
import com.springboot.printmastercrm.repository.OrderMaterialRepository;
import com.springboot.printmastercrm.repository.PostPressRepository;
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
    private final ClicheRepository clicheRepository;
    private final PostPressRepository postPressRepository;

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

    /**
     * Магний/латунь: задаётся размер исходного листа (см) и число листов; остаток считается в см²
     * как ширина × длина × листы, чтобы заказы списывали площадь именно с него.
     */
    @Transactional
    public Material createPlate(String name, MaterialCategory category, BigDecimal widthCm, BigDecimal lengthCm,
                                BigDecimal sheets, String note) {
        if (category != MaterialCategory.MAGNESIUM && category != MaterialCategory.BRASS) {
            throw new BusinessRuleException("Размер листа в см² задаётся только для магния и латуни");
        }
        requirePositiveSize(widthCm, lengthCm);
        BigDecimal count = sheets == null ? BigDecimal.ZERO : sheets;
        if (count.signum() < 0 || count.stripTrailingZeros().scale() > 0) {
            throw new BusinessRuleException("Количество листов — целое число, не меньше нуля");
        }
        String trimmed = requireName(name);
        if (materialRepository.existsByName(trimmed)) {
            throw new ExistsException("Материал с таким названием уже есть");
        }
        Material material = new Material();
        material.setName(trimmed);
        material.setCategory(category);
        material.setSheetWidthCm(widthCm);
        material.setSheetLengthCm(lengthCm);
        material.setNote(clean(note));
        material = materialRepository.save(material);
        if (count.signum() > 0) {
            receive(material.getId(), widthCm.multiply(lengthCm).multiply(count),
                    "Начальный остаток: " + count.stripTrailingZeros().toPlainString() + " лист.");
        }
        return material;
    }

    /**
     * Фольга: тип, ширина рулона (см), намотка (м) и число рулонов. Остаток считается в см²:
     * ширина × (намотка в см) × рулоны. Название собирается из типа и размера рулона.
     */
    @Transactional
    public Material createFoil(FoilType foilType, BigDecimal widthCm, BigDecimal windingM, BigDecimal rolls,
                               String note) {
        if (foilType == null) {
            throw new BusinessRuleException("Выберите тип фольги");
        }
        if (widthCm == null || windingM == null || widthCm.signum() <= 0 || windingM.signum() <= 0) {
            throw new BusinessRuleException("Ширина рулона и намотка должны быть больше нуля");
        }
        BigDecimal count = rolls == null ? BigDecimal.ZERO : rolls;
        if (count.signum() < 0 || count.stripTrailingZeros().scale() > 0) {
            throw new BusinessRuleException("Количество рулонов — целое число, не меньше нуля");
        }
        BigDecimal lengthCm = windingM.movePointRight(2);
        String name = foilType.getLabel() + " " + widthCm.stripTrailingZeros().toPlainString() + " см × "
                + windingM.stripTrailingZeros().toPlainString() + " м";
        if (materialRepository.existsByName(name)) {
            throw new ExistsException("Такая фольга («" + name + "») уже есть: примите на неё рулоны на странице материала");
        }
        Material material = new Material();
        material.setName(name);
        material.setCategory(MaterialCategory.FOIL);
        material.setFoilType(foilType);
        material.setSheetWidthCm(widthCm);
        material.setSheetLengthCm(lengthCm);
        material.setNote(clean(note));
        material = materialRepository.save(material);
        if (count.signum() > 0) {
            receive(material.getId(), widthCm.multiply(lengthCm).multiply(count),
                    "Начальный остаток: " + count.stripTrailingZeros().toPlainString() + " рул.");
        }
        return material;
    }

    /** Приход целых листов магния/латуни: на склад добавляется площадь листов. */
    @Transactional
    public void receiveSheets(Long id, BigDecimal sheets, String note) {
        Material material = get(id);
        BigDecimal area = material.getSheetAreaCm2();
        if (area == null) {
            throw new BusinessRuleException("У материала «" + material.getName() + "» не задан размер листа");
        }
        if (sheets == null || sheets.signum() <= 0 || sheets.stripTrailingZeros().scale() > 0) {
            throw new BusinessRuleException("Количество листов — целое число больше нуля");
        }
        String text = sheets.stripTrailingZeros().toPlainString()
                + (material.getCategory() == MaterialCategory.FOIL ? " рул." : " лист.");
        receive(id, area.multiply(sheets), note == null || note.isBlank() ? text : text + ", " + note.trim());
    }

    /** Меняет размер исходного листа магния/латуни (остаток не пересчитывается). */
    @Transactional
    public Material updatePlateSize(Long id, BigDecimal widthCm, BigDecimal lengthCm) {
        Material material = get(id);
        if (material.getCategory() == MaterialCategory.PAPER) {
            throw new BusinessRuleException("Для бумаги размер листа задаётся текстом");
        }
        requirePositiveSize(widthCm, lengthCm);
        material.setSheetWidthCm(widthCm);
        material.setSheetLengthCm(lengthCm);
        return materialRepository.save(material);
    }

    private static void requirePositiveSize(BigDecimal widthCm, BigDecimal lengthCm) {
        if (widthCm == null || lengthCm == null || widthCm.signum() <= 0 || lengthCm.signum() <= 0) {
            throw new BusinessRuleException("Ширина и длина листа должны быть больше нуля");
        }
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
        clicheRepository.detachMaterial(id);
        postPressRepository.detachMaterial(id);
        movementRepository.deleteByMaterialId(id);
        materialRepository.delete(material);
    }

    /** Резервирует материал под заказ: остаток не меняется, но свободного становится меньше. */
    @Transactional
    public void reserve(Long id, BigDecimal quantity) {
        Material material = get(id);
        BigDecimal amount = normalize(material, quantity);
        if (amount.compareTo(material.getAvailable()) > 0) {
            throw notEnough(material);
        }
        material.setReserved(material.getReserved().add(amount));
        materialRepository.save(material);
    }

    /** Снимает резерв (заказ удалён до отгрузки). */
    @Transactional
    public void release(Long id, BigDecimal quantity) {
        Material material = get(id);
        BigDecimal amount = quantity.setScale(3, RoundingMode.HALF_UP);
        material.setReserved(material.getReserved().subtract(amount).max(BigDecimal.ZERO));
        materialRepository.save(material);
    }

    /** Выполнение заказа: резерв превращается в списание со склада. */
    @Transactional
    public void ship(Long id, BigDecimal quantity, String note) {
        Material material = get(id);
        BigDecimal amount = quantity.setScale(3, RoundingMode.HALF_UP);
        if (amount.compareTo(material.getStock()) > 0) {
            throw notEnough(material);
        }
        material.setReserved(material.getReserved().subtract(amount).max(BigDecimal.ZERO));
        material.setStock(material.getStock().subtract(amount));
        materialRepository.save(material);
        writeMovement(material, MovementType.OUT, amount, note);
    }

    /** Отмена выполнения: материал возвращается на склад и снова в резерве заказа. */
    @Transactional
    public void unship(Long id, BigDecimal quantity, String note) {
        Material material = get(id);
        BigDecimal amount = quantity.setScale(3, RoundingMode.HALF_UP);
        material.setStock(material.getStock().add(amount));
        material.setReserved(material.getReserved().add(amount));
        materialRepository.save(material);
        writeMovement(material, MovementType.IN, amount, note);
    }

    private void move(Long id, MovementType type, BigDecimal quantity, String note) {
        Material material = get(id);
        BigDecimal amount = normalize(material, quantity);
        BigDecimal newStock = type == MovementType.IN ? material.getStock().add(amount) : material.getStock().subtract(amount);
        // списать можно только свободный остаток: зарезервированное принадлежит заказам
        if (newStock.signum() < 0 || newStock.compareTo(material.getReserved()) < 0) {
            throw notEnough(material);
        }
        material.setStock(newStock);
        materialRepository.save(material);
        writeMovement(material, type, amount, note);
    }

    private void writeMovement(Material material, MovementType type, BigDecimal amount, String note) {
        StockMovement movement = new StockMovement();
        movement.setMaterial(material);
        movement.setMovementType(type);
        movement.setQuantity(amount);
        movement.setNote(clean(note));
        movementRepository.save(movement);
    }

    private static BigDecimal normalize(Material material, BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessRuleException("Количество должно быть больше нуля");
        }
        if (material.getUnit().isWhole() && quantity.stripTrailingZeros().scale() > 0) {
            throw new BusinessRuleException("«" + material.getName() + "»: листы указываются целым числом");
        }
        return quantity.setScale(3, RoundingMode.HALF_UP);
    }

    private static BusinessRuleException notEnough(Material material) {
        return new BusinessRuleException("На складе недостаточно материала «" + material.getName()
                + "»: свободно " + material.getAvailable().stripTrailingZeros().toPlainString()
                + " " + material.getUnit().getLabel()
                + (material.getReserved().signum() > 0
                ? " (в резерве " + material.getReserved().stripTrailingZeros().toPlainString() + ")" : ""));
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
