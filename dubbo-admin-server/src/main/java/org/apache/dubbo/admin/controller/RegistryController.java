package org.apache.dubbo.admin.controller;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.dubbo.admin.common.CommonResponse;
import org.apache.dubbo.admin.common.EnvEnum;
import org.apache.dubbo.admin.model.domain.Registry;
import org.apache.dubbo.admin.service.RegistryService;
import org.apache.dubbo.admin.service.RegistrySubscribeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author heyudev
 * @date 2019/06/06
 */
@RestController
@RequestMapping(value = "/{env}/registry")
public class RegistryController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryController.class);

    @Autowired
    private RegistryService registryService;
    @Autowired
    private RegistrySubscribeService registrySubscribeService;

    @ApiOperation(value = "添加并订阅注册中心", notes = "添加并订阅注册中心")
    @ApiImplicitParam(name = "registry", value = "注册中心信息", required = true, dataType = "Registry")
    @PostMapping(value = "/addAndSubscribe")
    public CommonResponse addAndSubscribe(@RequestBody Registry registry) {
        LOGGER.info("addAndSubscribe registry = {}", registry);
        CommonResponse commonResponse = CommonResponse.createCommonResponse("添加成功！");
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
        if (Objects.equals(registry.getState(), 0)) {
            commonResponse.setData(registryService.add(registry));
        } else {
            commonResponse.setData(registrySubscribeService.addAndSubscribe(registry));
        }
        LOGGER.info("addAndSubscribe commonResponse = {}", commonResponse);
        return commonResponse;
    }

    @ApiOperation(value = "添加注册中心", notes = "添加注册中心")
    @ApiImplicitParam(name = "registry", value = "注册中心信息", required = true, dataType = "Registry")
    @PostMapping(value = "/add")
    public CommonResponse add(@RequestBody Registry registry) {
        LOGGER.info("add registry = {}", registry);
        CommonResponse commonResponse = CommonResponse.createCommonResponse("添加成功！");
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
        LOGGER.info("add commonResponse = {}", commonResponse);
        return commonResponse;
    }

    @ApiOperation(value = "订阅", notes = "订阅")
    @ApiImplicitParam(name = "registry", value = "注册中心信息", required = true, dataType = "Registry")
    @PostMapping(value = "/subscribe")
    public CommonResponse subscribe(@RequestBody Registry registry) {
        LOGGER.info("subscribe registry = {}", registry);
        CommonResponse commonResponse = CommonResponse.createCommonResponse("订阅成功！");
        if (registry == null || registry.getId() == null || registry.getId().equals(0)) {
            commonResponse.fail("参数错误：ID不能为空");
            return commonResponse;
        }
        Registry result = registryService.getRegistry(registry);
        registrySubscribeService.subscribe(result);
        LOGGER.info("subscribe commonResponse = {}", commonResponse);
        return commonResponse;
    }

    @ApiOperation(value = "取消订阅", notes = "取消订阅")
    @ApiImplicitParam(name = "registry", value = "注册中心信息", required = true, dataType = "Registry")
    @PostMapping(value = "/unSubscribe")
    public CommonResponse unSubscribe(@RequestBody Registry registry) {
        LOGGER.info("unSubscribe registry = {}", registry);
        CommonResponse commonResponse = CommonResponse.createCommonResponse("取消订阅成功！");
        if (registry == null || registry.getId() == null || registry.getId().equals(0)) {
            commonResponse.fail("参数错误：ID不能为空");
            return commonResponse;
        }
        Registry result = registryService.getRegistry(registry);
        registrySubscribeService.unSubscribe(result);
        LOGGER.info("unSubscribe commonResponse = {}", commonResponse);
        return commonResponse;
    }

    @ApiOperation(value = "查询注册中心列表", notes = "查询注册中心列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "env", value = "环境:dev beta pre release", required = true, dataType = "string"),
            @ApiImplicitParam(name = "page", value = "页码", required = false, dataType = "int"),
            @ApiImplicitParam(name = "size", value = "每页条数", required = false, dataType = "int"),
    })
    @PostMapping(value = "/getRegistryList")
    public CommonResponse getRegistryList(Pageable pageable, @PathVariable String env) {
        LOGGER.info("getRegistryList pageable = {},env = {}", pageable, env);
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        List<Registry> list = registryService.getRegistryList();
        commonResponse.setData(list);
        return commonResponse;
    }

    @ApiOperation(value = "查询注册中心列表", notes = "查询注册中心列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "env", value = "环境:dev beta pre release", required = true, dataType = "string"),
            @ApiImplicitParam(name = "page", value = "页码", required = false, dataType = "int"),
            @ApiImplicitParam(name = "size", value = "每页条数", required = false, dataType = "int"),
    })
    @PostMapping(value = "/getAllRegistry")
    public CommonResponse getAllRegistry(Pageable pageable, @PathVariable String env) {
        LOGGER.info("getAllRegistry pageable = {},env = {}", pageable, env);
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        List<Registry> list = getRegistries(env);
        commonResponse.setData(list);
        return commonResponse;
    }

    @ApiOperation(value = "查询注册中心列表，排除不可用的zk集群", notes = "查询注册中心列表，排除不可用的zk集群")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "env", value = "环境:dev beta pre release", required = true, dataType = "string"),
            @ApiImplicitParam(name = "page", value = "页码", required = false, dataType = "int"),
            @ApiImplicitParam(name = "size", value = "每页条数", required = false, dataType = "int"),
    })
    @PostMapping(value = "/getAllRegistryFilter")
    public CommonResponse getAllRegistryFilter(Pageable pageable, @PathVariable String env) {
        LOGGER.info("getAllRegistryFilter pageable = {},env = {}", pageable, env);
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
//        List<Registry> registryList = new ArrayList<>();
        List<Registry> list = getRegistries(env);
//        String exclude1 = "10.10.97.81:4181";
//        for (Registry registry : list) {
//            if (!Objects.equals(exclude1, registry.getRegAddress())) {
//                registryList.add(registry);
//            }
//        }
//        commonResponse.setData(registryList);
        commonResponse.setData(list);
        return commonResponse;
    }

    /**
     * 获取注册中心
     *
     * @param env
     * @return
     */
    private List<Registry> getRegistries(String env) {
        int envCode = EnvEnum.getValueByName(env);
        List<Registry> list;
        if (envCode == -1) {
            list = registryService.getAllRegistry();
        } else {
            list = registryService.getAllRegistryByEnv(envCode);
            if (EnvEnum.BETA.getValue() == envCode) {
                //TODO 获取docker registries
                List<Registry> dockerList = new ArrayList<>();
                list.addAll(dockerList);
            }
        }
        return list;
    }

    @PostMapping(value = "/getAllRegistryOfAuto")
    public CommonResponse getAllRegistryOfAuto(Pageable pageable, @PathVariable String env) {
        LOGGER.info("getAllRegistryOfAuto pageable = {}", pageable);
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        commonResponse.setData(registryService.getAllRegistryOfAuto());
        return commonResponse;
    }

}
