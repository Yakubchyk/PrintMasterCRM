package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.FoilType;
import com.springboot.printmastercrm.entity.Material;
import com.springboot.printmastercrm.entity.MaterialCategory;
import com.springboot.printmastercrm.entity.MovementType;
import com.springboot.printmastercrm.entity.StockMovement;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.exception.ExistsException;
import com.springboot.printmastercrm.repository.ClicheRepository;
import com.springboot.printmastercrm.repository.MaterialRepository;
import com.springboot.printmastercrm.repository.OrderMaterialRepository;
import com.springboot.printmastercrm.repository.PostPressRepository;
import com.springboot.printmastercrm.repository.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialServiceTest {

    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private StockMovementRepository movementRepository;
    @Mock
    private OrderMaterialRepository orderMaterialRepository;
    @Mock
    private ClicheRepository clicheRepository;
    @Mock
    private PostPressRepository postPressRepository;
    @InjectMocks
    private MaterialService service;

    private Material foil;

    @BeforeEach
    void setUp() {
        foil = new Material();
        foil.setId(1L);
        foil.setName("Картон 300");
        foil.setCategory(MaterialCategory.PAPER);
        foil.setStock(new BigDecimal("10.000"));
        lenient().when(materialRepository.findById(1L)).thenReturn(Optional.of(foil));
        lenient().when(materialRepository.save(any(Material.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void receiveIncreasesStockAndWritesMovement() {
        service.receive(1L, new BigDecimal("5"), "Поставка");

        assertThat(foil.getStock()).isEqualByComparingTo("15");
        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(movementRepository).save(captor.capture());
        assertThat(captor.getValue().getMovementType()).isEqualTo(MovementType.IN);
        assertThat(captor.getValue().getQuantity()).isEqualByComparingTo("5");
        assertThat(captor.getValue().getNote()).isEqualTo("Поставка");
    }

    @Test
    void consumeDecreasesStock() {
        service.consume(1L, new BigDecimal("4"), null);

        assertThat(foil.getStock()).isEqualByComparingTo("6");
        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(movementRepository).save(captor.capture());
        assertThat(captor.getValue().getMovementType()).isEqualTo(MovementType.OUT);
    }

    @Test
    void consumeCanTakeEverythingButNotMore() {
        service.consume(1L, new BigDecimal("10"), null);
        assertThat(foil.getStock()).isEqualByComparingTo("0");

        assertThatThrownBy(() -> service.consume(1L, new BigDecimal("1"), null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("недостаточно");
    }

    @Test
    void consumeMoreThanInStockIsRejectedAndNothingIsSaved() {
        assertThatThrownBy(() -> service.consume(1L, new BigDecimal("11"), null))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(foil.getStock()).isEqualByComparingTo("10");
        verify(movementRepository, never()).save(any());
    }

    @Test
    void quantityMustBePositive() {
        assertThatThrownBy(() -> service.receive(1L, BigDecimal.ZERO, null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.consume(1L, new BigDecimal("-1"), null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.receive(1L, null, null)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void createRejectsDuplicateNameAndBlankName() {
        when(materialRepository.existsByName("Картон 300")).thenReturn(true);

        assertThatThrownBy(() -> service.create("Картон 300", MaterialCategory.PAPER, null, null, null))
                .isInstanceOf(ExistsException.class);
        assertThatThrownBy(() -> service.create("  ", MaterialCategory.PAPER, null, null, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void createWithInitialStockWritesReceipt() {
        Material[] created = new Material[1];
        when(materialRepository.save(any(Material.class))).thenAnswer(i -> {
            Material m = i.getArgument(0);
            if (m.getId() == null) {
                m.setId(2L);
            }
            created[0] = m;
            return m;
        });
        when(materialRepository.findById(2L)).thenAnswer(i -> Optional.of(created[0]));

        Material material = service.create(" Бумага ", MaterialCategory.PAPER, "70x100", new BigDecimal("500"), null);

        assertThat(material.getName()).isEqualTo("Бумага");
        assertThat(material.getStock()).isEqualByComparingTo("500");
        verify(movementRepository).save(any(StockMovement.class));
    }

    @Test
    void updateRejectsNameOfAnotherMaterial() {
        when(materialRepository.existsByNameAndIdNot("Бумага", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, "Бумага", null, null))
                .isInstanceOf(ExistsException.class);
    }

    @Test
    void sheetsMustBeWholeNumbers() {
        assertThatThrownBy(() -> service.receive(1L, new BigDecimal("2.5"), null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("целым");
        service.receive(1L, new BigDecimal("2.0"), null);
        assertThat(foil.getStock()).isEqualByComparingTo("12");
    }

    @Test
    void clicheMaterialsAcceptFractionsOfSquareCm() {
        Material magnesium = new Material();
        magnesium.setId(5L);
        magnesium.setName("Магний 7 мм");
        magnesium.setCategory(MaterialCategory.MAGNESIUM);
        magnesium.setStock(new BigDecimal("1000"));
        when(materialRepository.findById(5L)).thenReturn(Optional.of(magnesium));

        service.consume(5L, new BigDecimal("12.5"), null);

        assertThat(magnesium.getStock()).isEqualByComparingTo("987.5");
    }

    @Test
    void sheetSizeIsKeptOnlyForPaper() {
        when(materialRepository.save(any(Material.class))).thenAnswer(i -> i.getArgument(0));

        Material paper = service.create("Мелованная", MaterialCategory.PAPER, " 70x100 ", null, null);
        Material brass = service.create("Латунь 2 мм", MaterialCategory.BRASS, "70x100", null, null);

        assertThat(paper.getSheetSize()).isEqualTo("70x100");
        assertThat(brass.getSheetSize()).isNull();
        assertThat(brass.getUnit().getLabel()).isEqualTo("см²");
    }

    @Test
    void usedMaterialCannotBeDeleted() {
        when(orderMaterialRepository.existsByMaterialId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(BusinessRuleException.class);
        verify(materialRepository, never()).delete(any(Material.class));
    }

    @Test
    void deleteRemovesHistoryFirst() {
        service.delete(1L);

        verify(clicheRepository).detachMaterial(1L);
        verify(postPressRepository).detachMaterial(1L);
        verify(movementRepository).deleteByMaterialId(1L);
        verify(materialRepository).delete(foil);
    }

    @Test
    void reserveKeepsStockButReducesAvailable() {
        service.reserve(1L, new BigDecimal("4"));

        assertThat(foil.getStock()).isEqualByComparingTo("10");
        assertThat(foil.getReserved()).isEqualByComparingTo("4");
        assertThat(foil.getAvailable()).isEqualByComparingTo("6");
        verify(movementRepository, never()).save(any());
    }

    @Test
    void reserveMoreThanAvailableIsRejected() {
        service.reserve(1L, new BigDecimal("8"));

        assertThatThrownBy(() -> service.reserve(1L, new BigDecimal("3")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("свободно 2");
        assertThat(foil.getReserved()).isEqualByComparingTo("8");
    }

    @Test
    void reservedMaterialCannotBeWrittenOffManually() {
        service.reserve(1L, new BigDecimal("8"));

        assertThatThrownBy(() -> service.consume(1L, new BigDecimal("3"), null))
                .isInstanceOf(BusinessRuleException.class);
        service.consume(1L, new BigDecimal("2"), null);
        assertThat(foil.getStock()).isEqualByComparingTo("8");
    }

    @Test
    void releaseReturnsReserveAndNeverGoesBelowZero() {
        service.reserve(1L, new BigDecimal("3"));

        service.release(1L, new BigDecimal("5"));

        assertThat(foil.getReserved()).isEqualByComparingTo("0");
    }

    @Test
    void shipTurnsReserveIntoWriteOffWithMovement() {
        service.reserve(1L, new BigDecimal("4"));

        service.ship(1L, new BigDecimal("4"), "Отгрузка");

        assertThat(foil.getStock()).isEqualByComparingTo("6");
        assertThat(foil.getReserved()).isEqualByComparingTo("0");
        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(movementRepository).save(captor.capture());
        assertThat(captor.getValue().getMovementType()).isEqualTo(MovementType.OUT);
    }

    @Test
    void unshipReturnsMaterialToStockAndReserve() {
        service.reserve(1L, new BigDecimal("4"));
        service.ship(1L, new BigDecimal("4"), null);

        service.unship(1L, new BigDecimal("4"), null);

        assertThat(foil.getStock()).isEqualByComparingTo("10");
        assertThat(foil.getReserved()).isEqualByComparingTo("4");
    }

    @Test
    void reservedSheetsMustBeWhole() {
        assertThatThrownBy(() -> service.reserve(1L, new BigDecimal("1.5")))
                .isInstanceOf(BusinessRuleException.class);
    }

    private Material savedPlate() {
        Material[] created = new Material[1];
        when(materialRepository.save(any(Material.class))).thenAnswer(i -> {
            Material m = i.getArgument(0);
            if (m.getId() == null) {
                m.setId(3L);
            }
            created[0] = m;
            return m;
        });
        when(materialRepository.findById(3L)).thenAnswer(i -> Optional.of(created[0]));
        return null;
    }

    @Test
    void plateStockIsSheetAreaTimesSheets() {
        savedPlate();

        Material magnesium = service.createPlate("Магний 7 мм", MaterialCategory.MAGNESIUM,
                new BigDecimal("30"), new BigDecimal("40"), new BigDecimal("5"), null);

        assertThat(magnesium.getSheetAreaCm2()).isEqualByComparingTo("1200");
        assertThat(magnesium.getStock()).isEqualByComparingTo("6000");   // 30 × 40 × 5 листов
        assertThat(magnesium.getUnit().getLabel()).isEqualTo("см²");
        verify(movementRepository).save(any(StockMovement.class));
    }

    @Test
    void plateRequiresSizeWholeSheetsAndNotPaper() {
        assertThatThrownBy(() -> service.createPlate("M", MaterialCategory.BRASS, BigDecimal.ZERO,
                new BigDecimal("40"), BigDecimal.ONE, null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.createPlate("M", MaterialCategory.BRASS, new BigDecimal("30"),
                new BigDecimal("40"), new BigDecimal("1.5"), null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.createPlate("M", MaterialCategory.PAPER, new BigDecimal("30"),
                new BigDecimal("40"), BigDecimal.ONE, null)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void receiveSheetsAddsWholeSheetAreas() {
        savedPlate();
        Material brass = service.createPlate("Латунь", MaterialCategory.BRASS,
                new BigDecimal("20"), new BigDecimal("10"), BigDecimal.ZERO, null);

        service.receiveSheets(3L, new BigDecimal("3"), "Поставка");

        assertThat(brass.getStock()).isEqualByComparingTo("600");
        assertThatThrownBy(() -> service.receiveSheets(3L, new BigDecimal("0.5"), null))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.receiveSheets(1L, BigDecimal.ONE, null))   // у бумаги нет площади листа
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void foilRollIsConvertedToSquareCentimetres() {
        savedPlate();

        // рулон 62 см × 122 м = 62 × 12 200 = 756 400 см²; 2 рулона
        Material foil = service.createFoil(FoilType.GOLD, new BigDecimal("62"), new BigDecimal("122"),
                new BigDecimal("2"), null);

        assertThat(foil.getCategory()).isEqualTo(MaterialCategory.FOIL);
        assertThat(foil.getSheetAreaCm2()).isEqualByComparingTo("756400");
        assertThat(foil.getStock()).isEqualByComparingTo("1512800");
        assertThat(foil.getRollLengthM()).isEqualByComparingTo("122");
        assertThat(foil.getName()).isEqualTo("Золото 62 см × 122 м");
        assertThat(foil.getUnit().getLabel()).isEqualTo("см²");
    }

    @Test
    void foilRequiresTypeSizeAndWholeRolls() {
        assertThatThrownBy(() -> service.createFoil(null, new BigDecimal("62"), new BigDecimal("122"), BigDecimal.ONE, null))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.createFoil(FoilType.SILVER, BigDecimal.ZERO, new BigDecimal("122"), BigDecimal.ONE, null))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.createFoil(FoilType.SILVER, new BigDecimal("62"), new BigDecimal("122"), new BigDecimal("1.5"), null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void sameFoilIsNotDuplicated() {
        when(materialRepository.existsByName("Серебро 62 см × 305 м")).thenReturn(true);

        assertThatThrownBy(() -> service.createFoil(FoilType.SILVER, new BigDecimal("62"), new BigDecimal("305"), BigDecimal.ONE, null))
                .isInstanceOf(ExistsException.class);
    }

    @Test
    void receiveRollsAddsRollAreas() {
        savedPlate();
        Material foil = service.createFoil(FoilType.MATTE_GOLD, new BigDecimal("30"), new BigDecimal("100"),
                BigDecimal.ZERO, null);

        service.receiveSheets(3L, new BigDecimal("2"), null);

        assertThat(foil.getStock()).isEqualByComparingTo("600000");   // 30 × 10 000 × 2
    }
}
