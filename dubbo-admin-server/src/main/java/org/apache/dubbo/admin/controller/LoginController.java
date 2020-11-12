package org.apache.dubbo.admin.controller;

import com.alibaba.fastjson.JSON;
import org.apache.dubbo.admin.common.CommonResponse;
import org.apache.dubbo.admin.common.util.Constants;
import org.apache.dubbo.admin.common.util.Tool;
import org.apache.dubbo.admin.model.domain.Provider;
import org.apache.dubbo.admin.model.domain.Registry;
import org.apache.dubbo.admin.service.*;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 登陆
 *
 * @author heyudev
 * @date 2019/05/09
 */
@Deprecated
@RestController
@RequestMapping(value = "/")
public class LoginController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);

    private static final String LOGIN_COOKIE = "accessToken";

    @Autowired
    AuthService authService;

    @Autowired
    private RegistryService registryService;

    @Autowired
    private RegistrySubscribeService registrySubscribeService;

    @Autowired
    private ProviderService providerService;

    @Autowired
    private ConsumerService consumerService;

    @Autowired
    OverrideService overrideService;

    /**
     * 登陆
     *
     * @param username
     * @param password
     * @param request
     * @param response
     * @return
     */
    @PostMapping(value = "/login")
    public CommonResponse login(@RequestParam String username, @RequestParam String password, HttpServletRequest request, HttpServletResponse response) {
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        if (username == null || "".equals(username) || password == null || "".equals(password)) {
            commonResponse.fail("用户名密码不能为空！");
            response.setStatus(HttpStatus.SC_UNAUTHORIZED);
            return commonResponse;
        }
        String accessToken = authService.login(username, password);
        if (accessToken == null) {
            commonResponse.fail("登陆失败，请确认用户名密码是否正确！");
            response.setStatus(HttpStatus.SC_UNAUTHORIZED);
            return commonResponse;
        }
        Cookie cookie = new Cookie(LOGIN_COOKIE, accessToken);
        cookie.setPath("/");
        //超时时间 2个小时
        cookie.setMaxAge(60 * 60 * 2);
        response.addCookie(cookie);
        return commonResponse;
    }

    /**
     * 探活
     *
     * @return
     */
    @GetMapping(value = "/healthcheck")
    public String healthcheck() {
        return "success";
    }


    @PostMapping(value = "/registry/add")
    public CommonResponse add(@RequestBody Registry registry) {
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        if (registry == null
                || registry.getRegName() == null || Objects.equals(registry.getRegName(), "")
                || registry.getEnv() == null || Objects.equals(registry.getEnv(), 0)
                || registry.getRegAddress() == null || Objects.equals(registry.getRegAddress(), "")) {
            return commonResponse.fail("参数错误");
        }
        if (registry.getRegGroup() == null || Objects.equals(registry.getRegGroup(), "")) {
            registry.setRegGroup("dubbo");
        }
        if (registry.getAuto() == null) {
            registry.setAuto(1);
        }
        if (registry.getMonitor() == null) {
            registry.setMonitor(0);
        }
        if (registry.getState() == null) {
            registry.setState(0);
        }
        commonResponse.setData(registryService.add(registry));
        return commonResponse;
    }


    @PostMapping(value = "/registry/addAndSubscribe")
    public CommonResponse addAndSubscribe(@RequestBody Registry registry) {
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        if (registry == null
                || registry.getRegName() == null || Objects.equals(registry.getRegName(), "")
                || registry.getEnv() == null || Objects.equals(registry.getEnv(), 0)
                || registry.getRegAddress() == null || Objects.equals(registry.getRegAddress(), "")) {
            return commonResponse.fail("参数错误");
        }
        if (registry.getRegGroup() == null || Objects.equals(registry.getRegGroup(), "")) {
            registry.setRegGroup("dubbo");
        }
        if (registry.getAuto() == null) {
            registry.setAuto(1);
        }
        if (registry.getMonitor() == null) {
            registry.setMonitor(0);
        }
        if (registry.getState() == null) {
            registry.setState(0);
        }
        commonResponse.setData(registrySubscribeService.addAndSubscribe(registry));
        return commonResponse;
    }

    @PostMapping(value = "/registry/update")
    public CommonResponse update(@RequestBody Registry registry) {
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        LOGGER.info("update registry = {}", registry);
        commonResponse.setData(registryService.update(registry));
        return commonResponse;
    }

    @PostMapping(value = "/registry/updateByAppCode")
    public CommonResponse updateByAppCode(@RequestBody Registry registry) {
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        Registry registry1 = new Registry();
        registry1.setAppCode(registry.getAppCode());
        if (registry.getEnv() != null && registry.getEnv().intValue() != 0) {
            registry1.setEnv(registry.getEnv());
        }
        if (registry.getAuto() != null) {
            registry1.setAuto(registry.getAuto());
        }
        if (registry.getRegAddress() != null) {
            registry1.setRegAddress(registry.getRegAddress());
        }
        if (registry.getRegName() != null) {
            registry1.setRegName(registry.getRegName());
        }
        if (registry.getMonitor() != null) {
            registry1.setMonitor(registry.getMonitor());
        }
        if (registry.getState() == null) {
            registry1.setState(0);
        } else {
            registry1.setState(registry.getState());
        }
        commonResponse.setData(registryService.updateByAppCode(registry1));
        return commonResponse;
    }

    @PostMapping(value = "/registry/delete")
    public CommonResponse delete(@RequestBody Registry registry) {
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        Registry result = registryService.getRegistry(registry);
        registrySubscribeService.unSubscribe(result);
        commonResponse.setData(registryService.delete(registry));
        return commonResponse;
    }

    @PostMapping(value = "/search")
    public String search(@RequestBody Map<String, Object> params) {
//    public String search(@RequestParam String registryAddress, @RequestParam String service, @RequestParam String address, @RequestParam String category, @RequestParam String hash) {
//        String service = params.get("service").toString();
//        String address = params.get("address").toString();
//        String registryAddress = params.get("registryAddress").toString();
//        service = service.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
//        String group = Tool.getGroup(service);
//        String version = Tool.getVersion(service);
//        String interfaze = Tool.getInterface((service));
//        List<org.apache.dubbo.admin.model.domain.Override> overrides = overrideService.findByServiceAndAddress(service, address, registryAddress);
//        LOGGER.info("overrides = {}", JSON.toJSONString(overrides));
//        String result = providerService.test(service, registryAddress);
//        LOGGER.info("result = {}", result);
        List<Registry> list = registryService.getAllRegistryOfMonitor();
        LOGGER.info("list = {}", list);
        return "ok";
    }
}
