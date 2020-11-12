package org.apache.dubbo.admin.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Splitter;
import org.apache.dubbo.admin.common.exception.PermissionDeniedException;
import org.apache.dubbo.admin.common.util.DesUtils;
import org.apache.dubbo.admin.service.AuthService;
import org.apache.dubbo.admin.common.util.HttpClientUtil;
import org.apache.dubbo.admin.common.util.HttpPoolClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author heyudev
 * @date 2019/05/13
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final HttpPoolClient HTTP_POOL_CLIENT = HttpClientUtil.useDefault();

    @Value(value = "${auth.url}")
    private String appcenterUrl;

    private static final String APPCENTER_LOGIN = "/auth/login";

    private static final String APPCENTER_ALIAS = "/openapi/app/listAppAndAlias";

    private static final String ALIAS_URL = "/openapi/app/listAppByToken";

    private static final String AUTH_KEY = "dubbomadmin";

    @Override
    public String login(String userName, String password) {
        LOGGER.info("login userName = {},password = {}", userName, password);
        Map<String, Object> params = new HashMap<>(2);
        params.put("username", userName);
        params.put("password", password);
        try {
            Optional optional = HTTP_POOL_CLIENT.postJson(appcenterUrl + APPCENTER_LOGIN, JSON.toJSONString(params));
            if (optional.isPresent()) {
                JSONObject result = JSONObject.parseObject(optional.get().toString());
                if (Objects.equals(result.getString("code"), "0")) {
                    return DesUtils.encrypt(userName + ";" + AUTH_KEY);
                }
            }
        } catch (Exception e) {
            LOGGER.error("login error", e);
            throw new PermissionDeniedException("Login fail", e);
        }
        return null;
    }

    @Override
    public boolean auth(String accessToken) {
        try {
            String result = DesUtils.decrypt(accessToken);
            if (result != null) {
                List<String> list = Splitter.on(";").omitEmptyStrings().trimResults().splitToList(result);
                if (list != null && list.size() == 2 && Objects.equals(AUTH_KEY, list.get(1))) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.error("auth error", e);
        }
        return false;
    }

    @Override
    public Set<String> getApplication(String accessToken) {
        LOGGER.info("getApplication accessToken = {}", accessToken);
        Set<String> applications = new HashSet<>();
        if (accessToken == null || "".equals(accessToken)) {
            return applications;
        }
        try {
            Optional<String> optional = HTTP_POOL_CLIENT.get(appcenterUrl + ALIAS_URL + "?accessToken=" + accessToken + "&from=dubbom-admin");
            if (optional.isPresent()) {
                JSONObject jsonObject = JSONObject.parseObject(optional.get());
                if (Objects.equals(jsonObject.getString("code"), "0")) {
                    JSONArray data = jsonObject.getJSONArray("data");
                    if (data == null || data.isEmpty()) {
                        return applications;
                    }
                    for (int i = 0; i < data.size(); i++) {
                        JSONObject jsonObject1 = (JSONObject) data.get(i);
                        if (Objects.equals(jsonObject1.getString("alias"), null) || Objects.equals(jsonObject1.getString("alias"), "")) {
                            applications.add(jsonObject1.getString("appCode"));
                        } else {
                            applications.add(jsonObject1.getString("alias"));
                        }
                    }
                    return applications;
                }
            }
        } catch (Exception e) {
            LOGGER.error("getApplication error ", e);
        }
        return applications;
    }
}
