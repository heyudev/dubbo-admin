package org.apache.dubbo.admin.service;

import java.util.Set;

/**
 * 鉴权
 *
 * @author heyudev
 * @date 2019/05/13
 */
public interface AuthService {
    /**
     * 登陆
     *
     * @param userName
     * @param password
     * @return
     */
    String login(String userName, String password);

    /**
     * 权限校验
     *
     * @param accessToken
     * @return
     */
    boolean auth(String accessToken);

    /**
     * 跟俊用户名获取应用信息
     *
     * @param userName
     * @return
     */
    Set<String> getApplication(String userName);
}
