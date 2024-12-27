package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Printing;
import com.springboot.printmastercrm.entity.Setting;
import com.springboot.printmastercrm.repository.PrintingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PrintingService {

    @Autowired
    private PrintingRepository printingRepository;

    @Autowired
    private SettingService settingService;

    public Printing save(Printing printing) {

        Setting setting = settingService.getSettings();


        double totalCost = calculateCost(printing, setting);

        printing.setTotalCost(totalCost);
        return printingRepository.save(printing);
    }

    public void deleteById(Long id) {
        printingRepository.deleteById(id);
    }

    public List<Printing> findByCustomerId(Long customerId) {
        return printingRepository.findByCustomerId(customerId);
    }

    public double calculateCost(Printing printing, Setting setting) {
        if (printing.getQuantity() == null) {
            throw new IllegalArgumentException("Quantity cannot be null");
        }
        double basePrice = setting.getPricePrint();
        double colorMultiplier = "4+4".equals(printing.getColour()) ? 2 : 1;
        double densityMultiplier = getDensityMultiplier(printing.getPapier());
        double rawCost = basePrice * colorMultiplier * printing.getQuantity() * densityMultiplier;

        BigDecimal roundedCost = BigDecimal.valueOf(rawCost).setScale(2, BigDecimal.ROUND_HALF_UP);

        return roundedCost.doubleValue();
    }

    private double getDensityMultiplier(String papier) {
        switch (papier) {
            case "90g/m2":
                return 1.0;
            case "150g/m2":
                return 1.2;
            case "200g/m2":
                return 1.4;
            case "300g/m2":
                return 1.6;
            default:
                return 1.0;
        }
    }

    public List<Printing> findAll() {
        return printingRepository.findAll();

    }
}