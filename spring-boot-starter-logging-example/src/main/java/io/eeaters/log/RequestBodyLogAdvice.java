package io.eeaters.log;


import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Type;

/**
 * @author eeaters
 * @since 0.0.1-SNAPSHOT
 */
@ControllerAdvice
@Component
@AllArgsConstructor
public class RequestBodyLogAdvice extends RequestBodyAdviceAdapter {

    private final ProjectLogService logService;
    private final HttpServletRequest servletRequest;

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        logService.logRequest(servletRequest, body);
        return super.afterBodyRead(body, inputMessage, parameter, targetType, converterType);
    }
}
