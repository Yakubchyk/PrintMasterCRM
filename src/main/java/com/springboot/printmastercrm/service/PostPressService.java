package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.PostPress;
import com.springboot.printmastercrm.entity.Setting;
import com.springboot.printmastercrm.repository.PostPressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class PostPressService {

    private static final BigDecimal SQ_CM_IN_SQ_M = new BigDecimal("10000");
    /** Применяется ко всей итоговой сумме любого расчёта. */
    public static final BigDecimal COEFFICIENT_NDS = new BigDecimal("1.25");
    private static final BigDecimal COEFFICIENT_WORK = new BigDecimal("1.05");
    /** Минимальная площадь оттиска, см². */
    private static final BigDecimal MIN_AREA = new BigDecimal("50");

    @Autowired
    private PostPressRepository postPressRepository;

    @Autowired
    private SettingService settingService;

    public List<PostPress> findByCustomerId(Long customerId) {
        return postPressRepository.findByCustomerId(customerId);
    }

    public void save(PostPress postPress) {
        Setting settings = settingService.getSettings();
        BigDecimal totalCost = calculateTotal(postPress, settings);
        postPress.setTotalPrice(totalCost);
        postPressRepository.save(postPress);
    }


    public void deletePostPressById(Long id) {
        if (!postPressRepository.existsById(id)) {
            throw new IllegalArgumentException("PostPress not found with ID: " + id);
        }
        postPressRepository.deleteById(id);
    }

    public BigDecimal calculateTotal(PostPress postPress, Setting settings) {
        BigDecimal quantity = BigDecimal.valueOf(postPress.getQuantity());
        BigDecimal area = BigDecimal.valueOf(postPress.getWidthSM())
                .multiply(BigDecimal.valueOf(postPress.getLengthSM()));
        if (area.compareTo(MIN_AREA) <= 0) {
            area = MIN_AREA;
        }
        BigDecimal squareMeters = area.divide(SQ_CM_IN_SQ_M, 10, RoundingMode.HALF_UP);

        BigDecimal foil = quantity.multiply(squareMeters)
                .multiply(COEFFICIENT_WORK)
                .multiply(settings.getOneQuadratMetterFoilPrice());

        BigDecimal work = settings.getMontageWorkPrice()
                .add(settings.getOneOttiskPrice().multiply(quantity))
                .multiply(COEFFICIENT_WORK);

        BigDecimal totalCost = work.add(foil)
                .multiply(COEFFICIENT_NDS)
                .setScale(2, RoundingMode.HALF_UP);

        postPress.setTotalPrice(totalCost);
        return totalCost;
    }

    public List<PostPress> findAll() {
        return postPressRepository.findAll();

    }

    public PostPress findById(Long postPressId) {
        return postPressRepository.findById(postPressId)
                .orElseThrow(() -> new IllegalArgumentException("PostPress not found with ID: " + postPressId));
    }


}
