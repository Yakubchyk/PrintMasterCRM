package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.AreaUnit;
import com.springboot.printmastercrm.entity.Customer;
import com.springboot.printmastercrm.entity.Material;
import com.springboot.printmastercrm.entity.MaterialCategory;
import com.springboot.printmastercrm.entity.PostPress;
import com.springboot.printmastercrm.entity.Setting;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.exception.NotFoundException;
import com.springboot.printmastercrm.repository.PostPressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostPressServiceTest {

    @Mock
    private PostPressRepository postPressRepository;
    @Mock
    private SettingService settingService;
    @InjectMocks
    private PostPressService service;

    private static BigDecimal d(String value) {
        return new BigDecimal(value);
    }

    @Test
    void usualAreaIncludesWorkFoilAndMarkup() {
        // 10x10 см = 100 см²; фольга 10*100*1,05*50/10000 = 5,25; работа (100+2*10)*1,05 = 126
        // (126 + 5,25) * 1,25 = 164,0625 -> 164,06
        assertThat(service.calculateTotal(d("10"), d("10"), 10, TestSettings.standard())).isEqualByComparingTo("164.06");
    }

    @Test
    void smallAreaIsRaisedToMinimum50SquareCm() {
        // 5x5 = 25 см² -> считается как 50 см²; фольга 2,625; работа 126; (128,625) * 1,25 = 160,78
        assertThat(service.calculateTotal(d("5"), d("5"), 10, TestSettings.standard())).isEqualByComparingTo("160.78");
    }

    @Test
    void foilPricePerSquareCentimetreGivesSameResultAsPerSquareMetre() {
        Setting perCm2 = TestSettings.standard();
        perCm2.setFoilPrice(d("0.005"));
        perCm2.setFoilAreaUnit(AreaUnit.CM2);

        assertThat(service.calculateTotal(d("10"), d("10"), 10, perCm2)).isEqualByComparingTo("164.06");
    }

    @Test
    void markupComesFromSettings() {
        Setting doubled = TestSettings.standard();
        doubled.setMarkup(d("2"));

        // (126 + 5,25) * 2
        assertThat(service.calculateTotal(d("10"), d("10"), 10, doubled)).isEqualByComparingTo("262.50");
    }

    @Test
    void resultHasTwoDecimalPlaces() {
        assertThat(service.calculateTotal(d("7.3"), d("4.1"), 3, TestSettings.standard()).scale()).isEqualTo(2);
    }

    @Test
    void createStoresCalculatedTotalInCentimetres() {
        when(settingService.getSettings()).thenReturn(TestSettings.standard());
        when(postPressRepository.save(any(PostPress.class))).thenAnswer(i -> i.getArgument(0));
        Customer customer = Customer.builder().id(1L).build();

        PostPress saved = service.create(customer, d("10"), d("10"), 10);

        assertThat(saved.getTotalPrice()).isEqualByComparingTo("164.06");
        assertThat(saved.getWidthCm()).isEqualByComparingTo("10");
        assertThat(saved.getCustomer()).isSameAs(customer);
    }

    @Test
    void createRejectsNonPositiveValues() {
        Customer customer = Customer.builder().id(1L).build();

        assertThatThrownBy(() -> service.create(customer, d("0"), d("10"), 10)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.create(customer, d("10"), d("-1"), 10)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.create(customer, d("10"), d("10"), 0)).isInstanceOf(BusinessRuleException.class);
        verify(postPressRepository, never()).save(any());
    }

    @Test
    void deleteOnlyFromTheOwnCustomer() {
        PostPress pp = new PostPress();
        pp.setCustomer(Customer.builder().id(5L).build());
        when(postPressRepository.findById(9L)).thenReturn(Optional.of(pp));

        assertThatThrownBy(() -> service.delete(1L, 9L)).isInstanceOf(NotFoundException.class);
        verify(postPressRepository, never()).delete(any());

        service.delete(5L, 9L);
        verify(postPressRepository).delete(pp);
    }

    @Test
    void createStoresFoilAndRejectsNonFoilMaterial() {
        when(settingService.getSettings()).thenReturn(TestSettings.standard());
        when(postPressRepository.save(any(PostPress.class))).thenAnswer(i -> i.getArgument(0));
        Customer customer = Customer.builder().id(1L).build();
        Material foil = new Material();
        foil.setCategory(MaterialCategory.FOIL);
        Material paper = new Material();

        PostPress saved = service.create(customer, d("10"), d("10"), 10, foil);

        assertThat(saved.getMaterial()).isSameAs(foil);
        assertThat(saved.getFoilAreaCm2()).isEqualByComparingTo("1000");
        assertThatThrownBy(() -> service.create(customer, d("10"), d("10"), 10, paper))
                .isInstanceOf(BusinessRuleException.class);
    }
}
