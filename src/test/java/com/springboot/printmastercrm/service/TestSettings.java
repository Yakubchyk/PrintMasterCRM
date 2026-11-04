package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.AreaUnit;
import com.springboot.printmastercrm.entity.Setting;

import java.math.BigDecimal;

/** Настройки для тестов: приладка 100, оттиск 2, фольга 50 за м², печать 0,10, клише 0,5 за см² (минимум 20), коэффициент 1,25. */
final class TestSettings {

    private TestSettings() {
    }

    static Setting standard() {
        Setting s = new Setting();
        s.setId(1L);
        s.setMontageWorkPrice(new BigDecimal("100"));
        s.setOneOttiskPrice(new BigDecimal("2"));
        s.setFoilPrice(new BigDecimal("50"));
        s.setFoilAreaUnit(AreaUnit.M2);
        s.setPricePrint(new BigDecimal("0.10"));
        s.setClichePrice(new BigDecimal("0.5"));
        s.setClicheAreaUnit(AreaUnit.CM2);
        s.setClicheMinPrice(new BigDecimal("20"));
        s.setMarkup(new BigDecimal("1.25"));
        return s;
    }
}
