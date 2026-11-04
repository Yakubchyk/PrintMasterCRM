package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.LengthUnit;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class LengthUnitTest {

    @Test
    void centimetresStayAsTheyAre() {
        assertThat(LengthUnit.CM.toCm(new BigDecimal("12.5"))).isEqualByComparingTo("12.5");
        assertThat(LengthUnit.CM.format(new BigDecimal("12.500"))).isEqualTo("12.5");
        assertThat(LengthUnit.CM.format(new BigDecimal("100.000"))).isEqualTo("100");
    }

    @Test
    void millimetresConvertToCentimetresAndBack() {
        assertThat(LengthUnit.MM.toCm(new BigDecimal("125"))).isEqualByComparingTo("12.5");
        assertThat(LengthUnit.MM.format(new BigDecimal("12.500"))).isEqualTo("125");
        assertThat(LengthUnit.MM.format(new BigDecimal("0.050"))).isEqualTo("0.5");
    }

    @Test
    void nullIsShownAsEmpty() {
        assertThat(LengthUnit.CM.format(null)).isEmpty();
    }
}
