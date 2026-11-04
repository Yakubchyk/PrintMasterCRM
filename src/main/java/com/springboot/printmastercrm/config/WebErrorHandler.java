package com.springboot.printmastercrm.config;

import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.exception.ExistsException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.net.URI;
import java.util.Map;

/**
 * Ошибки, понятные пользователю (нарушено правило, дубль, неверное число в форме),
 * не показываются страницей ошибки: человек возвращается на ту же страницу с сообщением.
 */
@ControllerAdvice
public class WebErrorHandler {

    @ExceptionHandler({BusinessRuleException.class, ExistsException.class,
            MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public String handle(Exception e, HttpServletRequest request) {
        String message = (e instanceof BusinessRuleException || e instanceof ExistsException)
                ? e.getMessage()
                : "Проверьте введённые данные: одно из полей заполнено неверно";
        Map<String, Object> flash = RequestContextUtils.getOutputFlashMap(request);
        flash.put("error", message);
        return "redirect:" + backPath(request);
    }

    /** Путь страницы, с которой пришёл запрос (только путь и параметры, без адреса сайта). */
    private static String backPath(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return "/";
        }
        try {
            URI uri = new URI(referer);
            String path = uri.getRawPath();
            if (path == null || path.isEmpty() || !path.startsWith("/")) {
                return "/";
            }
            return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
        } catch (Exception ex) {
            return "/";
        }
    }
}
