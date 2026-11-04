package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Cliche;
import com.springboot.printmastercrm.entity.Customer;
import com.springboot.printmastercrm.entity.Setting;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.exception.NotFoundException;
import com.springboot.printmastercrm.repository.ClicheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClicheService {

    private final ClicheRepository clicheRepository;
    private final SettingService settingService;

    public List<Cliche> findByCustomerId(Long customerId) {
        return clicheRepository.findByCustomerIdOrderByIdDesc(customerId);
    }

    @Transactional
    public Cliche create(Customer customer, BigDecimal widthCm, BigDecimal lengthCm, int quantity) {
        if (widthCm == null || lengthCm == null || widthCm.signum() <= 0 || lengthCm.signum() <= 0 || quantity <= 0) {
            throw new BusinessRuleException("Ширина, длина и количество должны быть больше нуля");
        }
        Cliche cliche = new Cliche();
        cliche.setCustomer(customer);
        cliche.setWidthCm(widthCm);
        cliche.setLengthCm(lengthCm);
        cliche.setQuantity(quantity);
        cliche.setTotalPrice(calculateTotal(widthCm, lengthCm, quantity, settingService.getSettings()));
        return clicheRepository.save(cliche);
    }

    @Transactional
    public void delete(Long customerId, Long id) {
        Cliche cliche = clicheRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Расчёт не найден"));
        if (!cliche.getCustomer().getId().equals(customerId)) {
            throw new NotFoundException("Расчёт не найден");
        }
        clicheRepository.delete(cliche);
    }

    /**
     * Одно клише = max(площадь * цена за единицу площади, минимальная цена).
     * Итог = цена одного * количество * коэффициент.
     */
    public BigDecimal calculateTotal(BigDecimal widthCm, BigDecimal lengthCm, int quantity, Setting settings) {
        BigDecimal area = widthCm.multiply(lengthCm);
        BigDecimal one = area.multiply(settings.getClichePrice())
                .divide(settings.getClicheAreaUnit().getCm2PerUnit(), 10, RoundingMode.HALF_UP);
        if (one.compareTo(settings.getClicheMinPrice()) < 0) {
            one = settings.getClicheMinPrice();
        }
        return one.multiply(BigDecimal.valueOf(quantity))
                .multiply(settings.getMarkup())
                .setScale(2, RoundingMode.HALF_UP);
    }
}
