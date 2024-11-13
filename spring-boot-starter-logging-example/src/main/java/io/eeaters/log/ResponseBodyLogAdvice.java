package io.eeaters.log;


import lombok.AllArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * ResponseBody 响应
 *
 * @author eeaters
 * @since 0.0.1-SNAPSHOT
 */
@AllArgsConstructor
@ControllerAdvice
@Component
public class ResponseBodyLogAdvice implements ResponseBodyAdvice<Object> {

    private final ProjectLogService logService;

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class selectedConverterType,
                                  ServerHttpRequest servletRequest,
                                  ServerHttpResponse serverHttpResponse) {

        if (servletRequest instanceof ServletServerHttpRequest request
                &&
                serverHttpResponse instanceof ServletServerHttpResponse response) {
            logService.logResponse(request.getServletRequest(), response.getServletResponse(), body);
        }
        return null;
    }
}
