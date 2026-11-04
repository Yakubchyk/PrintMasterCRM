package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Printing;
import com.springboot.printmastercrm.entity.Setting;
import com.springboot.printmastercrm.repository.PrintingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class PrintingService {

    @Autowired
    private PrintingRepository printingRepository;

    @Autowired
    private SettingService settingService;

    public Printing save(Printing printing) {

        Setting setting = settingService.getSettings();


        BigDecimal totalCost = calculateCost(printing, setting);

        printing.setTotalCost(totalCost);
        return printingRepository.save(printing);
    }

    public void deleteById(Long id) {
        printingRepository.deleteById(id);
    }

    public List<Printing> findByCustomerId(Long customerId) {
        return printingRepository.findByCustomerId(customerId);
    }

    public BigDecimal calculateCost(Printing printing, Setting setting) {
        if (printing.getQuantity() == null) {
            throw new IllegalArgumentException("Quantity cannot be null");
        }
        BigDecimal colorMultiplier = "4+4".equals(printing.getColour()) ? new BigDecimal("2") : BigDecimal.ONE;

        return setting.getPricePrint()
                .multiply(colorMultiplier)
                .multiply(BigDecimal.valueOf(printing.getQuantity()))
                .multiply(getDensityMultiplier(printing.getPapier()))
                .multiply(PostPressService.COEFFICIENT_NDS)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getDensityMultiplier(String papier) {
        if (papier == null) {
            return BigDecimal.ONE;
        }
        switch (papier) {
            case "150g/m2":
                return new BigDecimal("1.2");
            case "200g/m2":
                return new BigDecimal("1.4");
            case "300g/m2":
                return new BigDecimal("1.6");
            default: // 90g/m2 и неизвестные значения
                return BigDecimal.ONE;
        }
    }

    public List<Printing> findAll() {
        return printingRepository.findAll();

    }
}