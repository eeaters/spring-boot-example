package io.eeaters.log;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * web日志打印配置
 *
 * @author eeaters
 * @since 0.0.1-SNAPSHOT
 */
@Configuration
public class WebLogConfig implements WebMvcConfigurer{


    @Autowired
    private ProjectLogService logService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new GetRequestParamAdvice(logService));
    }

}
