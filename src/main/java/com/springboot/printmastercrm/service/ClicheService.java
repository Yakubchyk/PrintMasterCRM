package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Cliche;
import com.springboot.printmastercrm.entity.Customer;
import com.springboot.printmastercrm.entity.Material;
import com.springboot.printmastercrm.entity.MaterialCategory;
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
        return create(customer, widthCm, lengthCm, BigDecimal.ZERO, quantity, null);
    }

    /**
     * Размеры — рабочее поле клише. Технологические поля marginCm добавляются с каждой стороны,
     * цена считается по общему размеру (рабочее поле + поля).
     */
    @Transactional
    public Cliche create(Customer customer, BigDecimal widthCm, BigDecimal lengthCm, BigDecimal marginCm,
                         int quantity, Material material) {
        if (widthCm == null || lengthCm == null || widthCm.signum() <= 0 || lengthCm.signum() <= 0 || quantity <= 0) {
            throw new BusinessRuleException("Ширина, длина и количество должны быть больше нуля");
        }
        BigDecimal margin = marginCm == null ? BigDecimal.ZERO : marginCm;
        if (margin.signum() < 0) {
            throw new BusinessRuleException("Технологические поля не могут быть отрицательными");
        }
        if (material != null && material.getCategory() != MaterialCategory.MAGNESIUM
                && material.getCategory() != MaterialCategory.BRASS) {
            throw new BusinessRuleException("Для клише выберите магний или латунь");
        }
        Cliche cliche = new Cliche();
        cliche.setCustomer(customer);
        cliche.setWidthCm(widthCm);
        cliche.setLengthCm(lengthCm);
        cliche.setMarginCm(margin);
        cliche.setMaterial(material);
        cliche.setQuantity(quantity);
        BigDecimal doubleMargin = margin.multiply(BigDecimal.valueOf(2));
        cliche.setTotalPrice(calculateTotal(widthCm.add(doubleMargin), lengthCm.add(doubleMargin), quantity,
                settingService.getSettings()));
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
     * Размеры здесь — общие (с полями). Одно клише = max(площадь * цена за единицу площади, минимальная цена).
     * Итог = цена одного * количество (коэффициент здесь не применяется — он только для тиснения).
     */
    public BigDecimal calculateTotal(BigDecimal widthCm, BigDecimal lengthCm, int quantity, Setting settings) {
        BigDecimal area = widthCm.multiply(lengthCm);
        BigDecimal one = area.multiply(settings.getClichePrice())
                .divide(settings.getClicheAreaUnit().getCm2PerUnit(), 10, RoundingMode.HALF_UP);
        if (one.compareTo(settings.getClicheMinPrice()) < 0) {
            one = settings.getClicheMinPrice();
        }
        return one.multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
