package org.apache.dubbo.admin.interceptor;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * @author heyudev
 * @date 2019/07/16
 */
//@Component
public class LogInterceptor implements HandlerInterceptor {

    private final static String LOG_TRACE_ID = "logTraceId";

    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o) {
        String uuid = UUID.randomUUID().toString();
        MDC.put(LOG_TRACE_ID, uuid);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) {
        MDC.remove(LOG_TRACE_ID);
    }
}
