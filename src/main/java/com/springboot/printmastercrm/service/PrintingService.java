package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Customer;
import com.springboot.printmastercrm.entity.Printing;
import com.springboot.printmastercrm.entity.Setting;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.exception.NotFoundException;
import com.springboot.printmastercrm.repository.PrintingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrintingService {

    public static final List<String> PAPERS = List.of("90g/m2", "150g/m2", "200g/m2", "300g/m2");
    public static final List<String> COLOURS = List.of("4+0", "4+4");

    private final PrintingRepository printingRepository;
    private final SettingService settingService;

    public List<Printing> findByCustomerId(Long customerId) {
        return printingRepository.findByCustomerIdOrderByIdDesc(customerId);
    }

    @Transactional
    public Printing create(Customer customer, String papier, String colour, int quantity) {
        if (quantity <= 0) {
            throw new BusinessRuleException("Тираж должен быть больше нуля");
        }
        if (!PAPERS.contains(papier) || !COLOURS.contains(colour)) {
            throw new BusinessRuleException("Выберите бумагу и цветность из списка");
        }
        Printing printing = new Printing();
        printing.setCustomer(customer);
        printing.setPapier(papier);
        printing.setColour(colour);
        printing.setQuantity(quantity);
        printing.setTotalCost(calculateCost(papier, colour, quantity, settingService.getSettings()));
        return printingRepository.save(printing);
    }

    @Transactional
    public void delete(Long customerId, Long id) {
        Printing printing = printingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Расчёт не найден"));
        if (!printing.getCustomer().getId().equals(customerId)) {
            throw new NotFoundException("Расчёт не найден");
        }
        printingRepository.delete(printing);
    }

    /** Цена печати * цветность * тираж * множитель плотности бумаги * коэффициент. */
    public BigDecimal calculateCost(String papier, String colour, Integer quantity, Setting setting) {
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity cannot be null");
        }
        BigDecimal colorMultiplier = "4+4".equals(colour) ? new BigDecimal("2") : BigDecimal.ONE;

        return setting.getPricePrint()
                .multiply(colorMultiplier)
                .multiply(BigDecimal.valueOf(quantity))
                .multiply(densityMultiplier(papier))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal densityMultiplier(String papier) {
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
}
