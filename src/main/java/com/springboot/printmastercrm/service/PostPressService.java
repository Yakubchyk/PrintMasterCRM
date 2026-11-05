package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Customer;
import com.springboot.printmastercrm.entity.Material;
import com.springboot.printmastercrm.entity.MaterialCategory;
import com.springboot.printmastercrm.entity.PostPress;
import com.springboot.printmastercrm.entity.Setting;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.exception.NotFoundException;
import com.springboot.printmastercrm.repository.PostPressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostPressService {

    /** Минимальная площадь оттиска, см². Всё, что меньше, считается как эта площадь. */
    static final BigDecimal MIN_AREA_CM2 = new BigDecimal("50");
    /** Запас на работу и фольгу. */
    static final BigDecimal COEFFICIENT_WORK = new BigDecimal("1.05");

    private final PostPressRepository postPressRepository;
    private final SettingService settingService;

    public List<PostPress> findByCustomerId(Long customerId) {
        return postPressRepository.findByCustomerIdOrderByIdDesc(customerId);
    }

    @Transactional
    public PostPress create(Customer customer, BigDecimal widthCm, BigDecimal lengthCm, int quantity) {
        return create(customer, widthCm, lengthCm, quantity, null);
    }

    /** foil — рулон фольги со склада (раздел «Фольга»); по размеру оттиска и тиражу он резервируется в заказе. */
    @Transactional
    public PostPress create(Customer customer, BigDecimal widthCm, BigDecimal lengthCm, int quantity,
                            Material foil) {
        requirePositive(widthCm, lengthCm, quantity);
        if (foil != null && foil.getCategory() != MaterialCategory.FOIL) {
            throw new BusinessRuleException("Для тиснения выберите материал из раздела «Фольга»");
        }
        PostPress postPress = new PostPress();
        postPress.setCustomer(customer);
        postPress.setWidthCm(widthCm);
        postPress.setLengthCm(lengthCm);
        postPress.setQuantity(quantity);
        postPress.setMaterial(foil);
        postPress.setTotalPrice(calculateTotal(widthCm, lengthCm, quantity, settingService.getSettings()));
        return postPressRepository.save(postPress);
    }

    @Transactional
    public void delete(Long customerId, Long id) {
        PostPress postPress = postPressRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Расчёт не найден"));
        if (!postPress.getCustomer().getId().equals(customerId)) {
            throw new NotFoundException("Расчёт не найден");
        }
        postPressRepository.delete(postPress);
    }

    /**
     * (работа + фольга) * коэффициент, где
     * работа = (приладка + оттиск * тираж) * 1.05,
     * фольга = тираж * площадь * 1.05 * цена фольги за единицу площади.
     */
    public BigDecimal calculateTotal(BigDecimal widthCm, BigDecimal lengthCm, int quantity, Setting settings) {
        BigDecimal qty = BigDecimal.valueOf(quantity);
        BigDecimal area = widthCm.multiply(lengthCm);
        if (area.compareTo(MIN_AREA_CM2) <= 0) {
            area = MIN_AREA_CM2;
        }

        BigDecimal foil = qty.multiply(area)
                .multiply(COEFFICIENT_WORK)
                .multiply(settings.getFoilPrice())
                .divide(settings.getFoilAreaUnit().getCm2PerUnit(), 10, RoundingMode.HALF_UP);

        BigDecimal work = settings.getMontageWorkPrice()
                .add(settings.getOneOttiskPrice().multiply(qty))
                .multiply(COEFFICIENT_WORK);

        return work.add(foil)
                .multiply(settings.getMarkup())
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static void requirePositive(BigDecimal widthCm, BigDecimal lengthCm, int quantity) {
        if (widthCm == null || lengthCm == null || widthCm.signum() <= 0 || lengthCm.signum() <= 0 || quantity <= 0) {
            throw new BusinessRuleException("Ширина, длина и тираж должны быть больше нуля");
        }
    }
}
