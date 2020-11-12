/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.admin.controller;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.admin.common.CommonResponse;
import org.apache.dubbo.admin.common.exception.ParamValidationException;
import org.apache.dubbo.admin.common.exception.ResourceNotFoundException;
import org.apache.dubbo.admin.common.exception.SystemException;
import org.apache.dubbo.admin.common.exception.VersionValidationException;
import org.apache.dubbo.admin.common.request.BaseRequest;
import org.apache.dubbo.admin.common.util.Constants;
import org.apache.dubbo.admin.model.dto.ConditionRouteDTO;
import org.apache.dubbo.admin.service.ProviderService;
import org.apache.dubbo.admin.service.RouteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/{env}/rules/route/condition")
public class ConditionRoutesController extends BaseController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConditionRoutesController.class);

    private final RouteService routeService;
    private final ProviderService providerService;

    @Autowired
    public ConditionRoutesController(RouteService routeService, ProviderService providerService) {
        this.routeService = routeService;
        this.providerService = providerService;
    }

    @ApiOperation(value = "创建路由规则信息", notes = "创建路由规则信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "routeDTO", value = "路由规则", required = true, dataType = "ConditionRouteDTO"),
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string")
    })
    @RequestMapping(method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse createRule(@RequestBody ConditionRouteDTO routeDTO, @PathVariable String env) {
        LOGGER.info("createRule routeDTO = {}", routeDTO);
        //校验
        validateParameter(routeDTO);
        validateRegistryAddress(routeDTO.getRegistryAddress());
        validateService(routeDTO.getService());

        String serviceName = routeDTO.getService();
        String app = routeDTO.getApplication();
        String registryAddress = routeDTO.getRegistryAddress();
        if (StringUtils.isEmpty(serviceName) && StringUtils.isEmpty(app)) {
            throw new ParamValidationException("serviceName and app is Empty!");
        }
        if (StringUtils.isNotEmpty(app) && providerService.findVersionInApplication(app, registryAddress).equals("2.6")) {
            throw new VersionValidationException("dubbo 2.6 does not support application scope routing rule");
        }
        routeService.createConditionRoute(routeDTO);
        CommonResponse commonResponse = CommonResponse.createCommonResponse("创建成功！");
        commonResponse.setData(true);
        LOGGER.info("createRule commonResponse = {}", commonResponse);
        return commonResponse;
    }

    @ApiOperation(value = "更新路由规则信息", notes = "更新路由规则信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "newConditionRoute", value = "路由规则", required = true, dataType = "ConditionRouteDTO"),
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string")
    })
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public CommonResponse updateRule(@RequestBody ConditionRouteDTO newConditionRoute, @PathVariable String env) {
        LOGGER.info("updateRule newConditionRoute = {}", newConditionRoute);
        //校验
        validateParameter(newConditionRoute);
        validateRegistryAddress(newConditionRoute.getRegistryAddress());
        validateId(newConditionRoute.getId());

        String id = newConditionRoute.getId();
        String registryAddress = newConditionRoute.getRegistryAddress();
        id = id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
        ConditionRouteDTO oldConditionRoute = routeService.findConditionRoute(id, registryAddress);
        if (oldConditionRoute == null) {
            throw new ResourceNotFoundException("can not find route rule for: " + id);
        }
        newConditionRoute.setId(id);
        routeService.updateConditionRoute(newConditionRoute, registryAddress);
        CommonResponse commonResponse = CommonResponse.createCommonResponse("更新成功！");
        commonResponse.setData(true);
        LOGGER.info("updateRule commonResponse = {}", commonResponse);
        return commonResponse;
    }

    @ApiOperation(value = "查询条件路由配置列表", notes = "查询条件路由配置列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "application", value = "应用名", required = false, dataType = "string"),
            @ApiImplicitParam(name = "service", value = "服务名", required = false, dataType = "string"),
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string"),
            @ApiImplicitParam(name = "registryAddress", value = "注册中心地址：10.2.39.11:2181 或 10.2.39.11:2181,10.2.39.12:2181,10.2.39.13:2181", required = true, dataType = "string")
    })
    @RequestMapping(method = RequestMethod.GET)
    public CommonResponse searchRoutes(@RequestParam(required = false) String application,
                                       @RequestParam(required = false) String service, @PathVariable String env, @RequestParam String registryAddress) {
        LOGGER.info("searchRoutes application = {},service = {},registryAddress = {}", application, service, registryAddress);
        ConditionRouteDTO conditionRoute = null;
        List<ConditionRouteDTO> result = new ArrayList<>();
        if (StringUtils.isNotBlank(application)) {
            conditionRoute = routeService.findConditionRoute(application, registryAddress);
        } else if (StringUtils.isNotBlank(service)) {
            conditionRoute = routeService.findConditionRoute(service, registryAddress);
        } else {
            throw new ParamValidationException("Either Service or application is required.");
        }
        if (conditionRoute != null && conditionRoute.getConditions() != null) {
            conditionRoute.setRegistryAddress(registryAddress);
            result.add(conditionRoute);
        }
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        commonResponse.setData(result);
        return commonResponse;
    }

    @ApiOperation(value = "查看路由规则信息", notes = "查看路由规则信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string"),
            @ApiImplicitParam(name = "baseRequest", value = "需要参数：id：配置ID；registryAddress:注册中心", required = true, dataType = "BaseRequest")
    })
    @RequestMapping(value = "/detail", method = RequestMethod.POST)
    public CommonResponse detailRoute(@PathVariable String env, @RequestBody BaseRequest baseRequest) {
        LOGGER.info("detailRoute baseRequest = {}", baseRequest);
        validateBaseRequest(baseRequest);
        String id = baseRequest.getId();
        String registryAddress = baseRequest.getRegistryAddress();
        id = id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
        ConditionRouteDTO conditionRoute = routeService.findConditionRoute(id, registryAddress);
        if (conditionRoute == null || conditionRoute.getConditions() == null) {
            throw new ResourceNotFoundException("Unknown ID!");
        }
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        commonResponse.setData(conditionRoute);
        return commonResponse;
    }

    @ApiOperation(value = "删除路由规则", notes = "删除路由规则：当同一个服务黑白名单List为空时，才能删除此配置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string"),
            @ApiImplicitParam(name = "baseRequest", value = "需要参数：id：配置ID；registryAddress:注册中心", required = true, dataType = "BaseRequest")
    })
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public CommonResponse deleteRoute(@PathVariable String env, @RequestBody BaseRequest baseRequest) {
        LOGGER.info("deleteRoute baseRequest = {}", baseRequest);
        validateBaseRequest(baseRequest);
        String id = baseRequest.getId();
        String registryAddress = baseRequest.getRegistryAddress();
        id = id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
        CommonResponse commonResponse = CommonResponse.createCommonResponse("删除成功！");
        try {
            routeService.deleteConditionRoute(id, registryAddress);
        } catch (SystemException e) {
            commonResponse.fail(e.getMessage());
        }
        LOGGER.info("deleteRoute commonResponse = {}", commonResponse);
//        commonResponse.setData(true);
        return commonResponse;
    }

    @ApiOperation(value = "启用路由规则", notes = "启用路由规则")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string"),
            @ApiImplicitParam(name = "baseRequest", value = "需要参数：id：配置ID；registryAddress:注册中心", required = true, dataType = "BaseRequest")
    })
    @RequestMapping(value = "/enable", method = RequestMethod.POST)
    public CommonResponse enableRoute(@PathVariable String env, @RequestBody BaseRequest baseRequest) {
        LOGGER.info("enableRoute baseRequest = {}", baseRequest);
        validateBaseRequest(baseRequest);
        String id = baseRequest.getId();
        String registryAddress = baseRequest.getRegistryAddress();
        id = id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
        routeService.enableConditionRoute(id, registryAddress);
        CommonResponse commonResponse = CommonResponse.createCommonResponse("启用成功！");
        commonResponse.setData(true);
        LOGGER.info("enableRoute commonResponse = {}", commonResponse);
        return commonResponse;
    }

    @ApiOperation(value = "禁用路由规则", notes = "禁用路由规则")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string"),
            @ApiImplicitParam(name = "baseRequest", value = "需要参数：id：配置ID；registryAddress:注册中心", required = true, dataType = "BaseRequest")
    })
    @RequestMapping(value = "/disable", method = RequestMethod.POST)
    public CommonResponse disableRoute(@PathVariable String env, @RequestBody BaseRequest baseRequest) {
        LOGGER.info("disableRoute baseRequest = {}", baseRequest);
        validateBaseRequest(baseRequest);
        String id = baseRequest.getId();
        String registryAddress = baseRequest.getRegistryAddress();
        id = id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
        routeService.disableConditionRoute(id, registryAddress);
        CommonResponse commonResponse = CommonResponse.createCommonResponse("禁用成功！");
        commonResponse.setData(true);
        LOGGER.info("disableRoute commonResponse = {}", commonResponse);
        return commonResponse;
    }

}
