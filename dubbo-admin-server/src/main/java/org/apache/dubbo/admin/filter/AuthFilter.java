package org.apache.dubbo.admin.filter;

import org.apache.dubbo.admin.common.util.CookieUtil;
import org.apache.dubbo.admin.service.AuthService;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * 接口权限校验
 *
 * @author heyudev
 * @date 2019/05/14
 */
@Order(value = 1)
@WebFilter(filterName = "authFilter", urlPatterns = "/dev/*")
public class AuthFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthFilter.class);

    private static final String COOKIE_NAME = "accessToken";

    private final static String LOG_TRACE_ID = "logTraceId";

    @Autowired
    AuthService authService;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOGGER.info("init");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String uuid = UUID.randomUUID().toString();
        MDC.put(LOG_TRACE_ID, uuid);
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        Cookie[] cookies = req.getCookies();
        if (cookies == null || cookies.length == 0) {
            LOGGER.warn("cookies is empty");
            notLogin(req, resp);
            return;
        }
        String accessToken = CookieUtil.getAccessToken(req);
        if (accessToken == null || "".equals(accessToken)) {
            LOGGER.warn("no accessToken cookies");
            notLogin(req, resp);
            return;
        }
//        if (!authService.auth(cookie.get().getValue())) {
//            notLogin(req, resp);
//            return;
//        }
        //记录操作人
        MDC.put(COOKIE_NAME, accessToken);
        LOGGER.info("request URI = {},accessToken = {}", req.getRequestURI(), accessToken);
        chain.doFilter(request, response);
        MDC.remove(LOG_TRACE_ID);
        MDC.remove(COOKIE_NAME);
    }

    @Override
    public void destroy() {
        LOGGER.info("destroy");
    }

    /**
     * 未登陆直接返回
     *
     * @param req
     * @param resp
     * @throws IOException
     */
    private void notLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpStatus.SC_UNAUTHORIZED);
        LOGGER.warn("auth error");
//        resp.getWriter().write("/login");
    }
}
