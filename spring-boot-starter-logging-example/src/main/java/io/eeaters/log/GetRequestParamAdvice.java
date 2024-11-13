package io.eeaters.log;


import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * get请求入参
 *
 * @author eeaters
 * @since 0.0.1-SNAPSHOT
 */
@Slf4j
public class GetRequestParamAdvice implements HandlerInterceptor {


    private final ProjectLogService logService;

    public GetRequestParamAdvice(ProjectLogService logService) {
        this.logService = logService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (DispatcherType.REQUEST.name().equals(request.getDispatcherType().name())
                && request.getMethod().equals(HttpMethod.GET.name())) {
            logService.logRequest(request, null);
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        if (modelAndView != null) {
            log.info("Returning ModelAndView: {}", modelAndView.getViewName());
        }
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }
}
