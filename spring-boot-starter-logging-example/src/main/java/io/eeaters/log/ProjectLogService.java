package io.eeaters.log;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.eeaters.util.NonExSupplier;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.AbstractRequestLoggingFilter;

import java.util.Enumeration;

/**
 * 项目日志打印
 *
 * @see org.springframework.web.filter.AbstractRequestLoggingFilter
 * @author eeaters
 * @since 0.0.1-SNAPSHOT
 */
@Component
@Slf4j
public class ProjectLogService {


    @Autowired
    private ObjectMapper objectMapper;


    public void logRequest(HttpServletRequest request, Object body) {
        StringBuilder msg = new StringBuilder();
        msg.append(AbstractRequestLoggingFilter.DEFAULT_BEFORE_MESSAGE_PREFIX)
                .append(request.getMethod())
                .append("  ")
                .append(request.getRequestURI());

        String queryString = request.getQueryString();
        if (queryString != null) {
            msg.append('?').append(queryString);
        }
        String client = request.getRemoteAddr();
        if (StringUtils.hasLength(client)) {
            msg.append(", client=").append(client);
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            msg.append(", session=").append(session.getId());
        }
        String user = request.getRemoteUser();
        if (user != null) {
            msg.append(", user=").append(user);
        }
        HttpHeaders headers = new ServletServerHttpRequest(request).getHeaders();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String header = names.nextElement();
            headers.set(header, "masked");
        }
        msg.append(", headers=").append(headers);
        if (body != null) {
            msg.append(", body=").append(NonExSupplier.exec(() -> objectMapper.writeValueAsString(body)));
        }
        msg.append(AbstractRequestLoggingFilter.DEFAULT_BEFORE_MESSAGE_SUFFIX);
        log.info(msg.toString());

    }

    public void logResponse(HttpServletRequest request, HttpServletResponse response, Object body) {
        StringBuilder msg = new StringBuilder();
        msg.append(AbstractRequestLoggingFilter.DEFAULT_AFTER_MESSAGE_PREFIX)
                .append(request.getMethod())
                .append("  ")
                .append(request.getRequestURI());
        String queryString = request.getQueryString();
        if (queryString != null) {
            msg.append("?").append(queryString);
        }
        if (body != null) {
            msg.append(", responseBody=").append(NonExSupplier.exec(() -> objectMapper.writeValueAsString(body)));
        }
        msg.append(AbstractRequestLoggingFilter.DEFAULT_AFTER_MESSAGE_SUFFIX);
        log.info(msg.toString());
    }

}
