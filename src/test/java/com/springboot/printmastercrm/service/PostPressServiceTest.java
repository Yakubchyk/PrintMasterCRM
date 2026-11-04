package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.PostPress;
import com.springboot.printmastercrm.entity.Setting;
import com.springboot.printmastercrm.repository.PostPressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
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

    private static Setting settings() {
        Setting s = new Setting();
        s.setMontageWorkPrice(new BigDecimal("100"));
        s.setOneOttiskPrice(new BigDecimal("2"));
        s.setOneQuadratMetterFoilPrice(new BigDecimal("50"));
        s.setPricePrint(new BigDecimal("0.10"));
        return s;
    }

    private static PostPress pp(int quantity, double width, double length) {
        PostPress p = new PostPress();
        p.setQuantity(quantity);
        p.setWidthSM(width);
        p.setLengthSM(length);
        return p;
    }

    @Test
    void usualAreaIncludesWorkFoilAndCoefficients() {
        // 10x10 см = 0,01 м²; фольга 10*0,01*1,05*50 = 5,25; работа (100+2*10)*1,05 = 126
        // (126 + 5,25) * 1,25 = 164,0625 -> 164,06
        assertThat(service.calculateTotal(pp(10, 10, 10), settings())).isEqualByComparingTo("164.06");
    }

    @Test
    void smallAreaIsRaisedToMinimum50SquareCm() {
        // 5x5 = 25 см² -> считается как 50 см² = 0,005 м²; фольга 2,625; работа 126
        // (126 + 2,625) * 1,25 = 160,78125 -> 160,78
        assertThat(service.calculateTotal(pp(10, 5, 5), settings())).isEqualByComparingTo("160.78");
    }

    @Test
    void resultHasTwoDecimalPlaces() {
        assertThat(service.calculateTotal(pp(3, 7.3, 4.1), settings()).scale()).isEqualTo(2);
    }

    @Test
    void saveStoresCalculatedTotal() {
        when(settingService.getSettings()).thenReturn(settings());
        PostPress p = pp(10, 10, 10);

        service.save(p);

        assertThat(p.getTotalPrice()).isEqualByComparingTo("164.06");
        verify(postPressRepository).save(p);
    }
}
