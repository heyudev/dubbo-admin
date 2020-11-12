package org.apache.dubbo.admin.common.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * TODO
 *
 * @author supeng
 * @Date 2020/11/13
 */
public class CookieUtil {

    private static final String COOKIE_NAME = "accessToken";

    public static String getAccessToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        Optional<Cookie> cookie = Stream.of(cookies).filter(c -> COOKIE_NAME.equals(c.getName())).findFirst();
        if (cookie.isPresent()) {
            return cookie.get().getValue();
        }
        return null;
    }
}
