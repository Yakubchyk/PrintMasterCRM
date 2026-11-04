package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Material;
import com.springboot.printmastercrm.entity.MaterialUnit;
import com.springboot.printmastercrm.entity.MovementType;
import com.springboot.printmastercrm.entity.StockMovement;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.exception.ExistsException;
import com.springboot.printmastercrm.repository.MaterialRepository;
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
    @InjectMocks
    private MaterialService service;

    private Material foil;

    @BeforeEach
    void setUp() {
        foil = new Material();
        foil.setId(1L);
        foil.setName("Фольга золото");
        foil.setUnit(MaterialUnit.ROLL);
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
        service.consume(1L, new BigDecimal("4.5"), null);

        assertThat(foil.getStock()).isEqualByComparingTo("5.5");
        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(movementRepository).save(captor.capture());
        assertThat(captor.getValue().getMovementType()).isEqualTo(MovementType.OUT);
    }

    @Test
    void consumeCanTakeEverythingButNotMore() {
        service.consume(1L, new BigDecimal("10"), null);
        assertThat(foil.getStock()).isEqualByComparingTo("0");

        assertThatThrownBy(() -> service.consume(1L, new BigDecimal("0.001"), null))
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
        when(materialRepository.existsByName("Фольга золото")).thenReturn(true);

        assertThatThrownBy(() -> service.create("Фольга золото", MaterialUnit.ROLL, null, null))
                .isInstanceOf(ExistsException.class);
        assertThatThrownBy(() -> service.create("  ", MaterialUnit.ROLL, null, null))
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

        Material material = service.create(" Бумага ", MaterialUnit.PCS, new BigDecimal("500"), null);

        assertThat(material.getName()).isEqualTo("Бумага");
        assertThat(material.getStock()).isEqualByComparingTo("500");
        verify(movementRepository).save(any(StockMovement.class));
    }

    @Test
    void updateRejectsNameOfAnotherMaterial() {
        when(materialRepository.existsByNameAndIdNot("Бумага", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, "Бумага", MaterialUnit.PCS, null))
                .isInstanceOf(ExistsException.class);
    }

    @Test
    void deleteRemovesHistoryFirst() {
        service.delete(1L);

        verify(movementRepository).deleteByMaterialId(1L);
        verify(materialRepository).delete(foil);
    }
}
