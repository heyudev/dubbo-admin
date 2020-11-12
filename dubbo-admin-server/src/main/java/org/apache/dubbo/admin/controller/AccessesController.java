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
import org.apache.dubbo.admin.model.dto.AccessDTO;
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
import java.util.Objects;

@RestController
@RequestMapping("/{env}/rules/access")
public class AccessesController extends BaseController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessesController.class);

    private final RouteService routeService;
    private final ProviderService providerService;

    @Autowired
    public AccessesController(RouteService routeService, ProviderService providerService) {
        this.routeService = routeService;
        this.providerService = providerService;
    }

    @ApiOperation(value = "查询黑白名单列表", notes = "查询黑白名单列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "application", value = "应用名", required = false, dataType = "string"),
            @ApiImplicitParam(name = "service", value = "服务名", required = false, dataType = "string"),
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string"),
            @ApiImplicitParam(name = "registryAddress", value = "注册中心地址：10.2.39.11:2181 或 10.2.39.11:2181,10.2.39.12:2181,10.2.39.13:2181", required = true, dataType = "string")
    })
    @RequestMapping(method = RequestMethod.GET)
    public CommonResponse searchAccess(@RequestParam(required = false) String service,
                                       @RequestParam(required = false) String application,
                                       @PathVariable String env, @RequestParam String registryAddress) {
        LOGGER.info("searchAccess service = {},application = {},registryAddress = {}", service, application, registryAddress);
        validateRegistryAddress(registryAddress);
        validateService(service);
        //TODO 支持应用维度配置
//        if (StringUtils.isBlank(service) && StringUtils.isBlank(application)) {
//            throw new ParamValidationException("Either service or application is required");
//        }
        List<AccessDTO> accessDTOS = new ArrayList<>();
        AccessDTO accessDTO;
        if (StringUtils.isNotBlank(application)) {
            accessDTO = routeService.findAccess(application, registryAddress);
        } else {
            accessDTO = routeService.findAccess(service, registryAddress);
        }
        if (accessDTO != null) {
            accessDTO.setRegistryAddress(registryAddress);
            accessDTO.setEnabled(true);
            accessDTOS.add(accessDTO);
        }
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        commonResponse.setData(accessDTOS);
        return commonResponse;
    }

    @ApiOperation(value = "查看黑白名单信息", notes = "查看黑白名单信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string"),
            @ApiImplicitParam(name = "baseRequest", value = "需要参数：id：配置ID；registryAddress:注册中心", required = true, dataType = "BaseRequest")
    })
    @RequestMapping(value = "/detail", method = RequestMethod.POST)
    public CommonResponse detailAccess(@PathVariable String env, @RequestBody BaseRequest baseRequest) {
        LOGGER.info("detailAccess baseRequest = {}", baseRequest);
        validateBaseRequest(baseRequest);
        String id = baseRequest.getId();
        String registryAddress = baseRequest.getRegistryAddress();
        id = id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
        AccessDTO accessDTO = routeService.findAccess(id, registryAddress);
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        commonResponse.setData(accessDTO);
        return commonResponse;
    }

    @ApiOperation(value = "删除黑白名单配置", notes = "删除黑白名单配置：当同一个服务的条件路由配置条件为空时，才能删除此配置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string"),
            @ApiImplicitParam(name = "baseRequest", value = "需要参数：id：配置ID；registryAddress:注册中心", required = true, dataType = "BaseRequest")
    })
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public CommonResponse deleteAccess(@PathVariable String env, @RequestBody BaseRequest baseRequest) {
        LOGGER.info("deleteAccess baseRequest = {}", baseRequest);
        validateBaseRequest(baseRequest);
        String id = baseRequest.getId();
        String registryAddress = baseRequest.getRegistryAddress();
        id = id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
        CommonResponse commonResponse = CommonResponse.createCommonResponse("删除成功！");
        try {
            routeService.deleteAccess(id, registryAddress);
        } catch (SystemException e) {
            commonResponse.fail(e.getMessage());
        }
        LOGGER.info("deleteAccess commonResponse = {}", commonResponse);
        return commonResponse;
    }

    @ApiOperation(value = "创建黑白名单配置", notes = "创建黑白名单配置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "accessDTO", value = "配置信息", required = true, dataType = "AccessDTO"),
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string")
    })
    @RequestMapping(method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse createAccess(@RequestBody AccessDTO accessDTO, @PathVariable String env) {
        LOGGER.info("createAccess accessDTO = {}", accessDTO);
        validateParameter(accessDTO);
        validateRegistryAddress(accessDTO.getRegistryAddress());
        validateService(accessDTO.getService());
//        if ((StringUtils.isBlank(accessDTO.getService()) && StringUtils.isBlank(accessDTO.getApplication())) || StringUtils.isBlank(accessDTO.getRegistryAddress())) {
//            throw new ParamValidationException("Either Service or application is required.");
//        }
        String application = accessDTO.getApplication();
        if (StringUtils.isNotEmpty(application) && "2.6".equals(providerService.findVersionInApplication(application, accessDTO.getRegistryAddress()))) {
            throw new VersionValidationException("dubbo 2.6 does not support application scope blackwhite list config");
        }
        if (accessDTO.getBlacklist() == null && accessDTO.getWhitelist() == null) {
            throw new ParamValidationException("One of Blacklist/Whitelist is required.");
        }
        routeService.createAccess(accessDTO);
        CommonResponse commonResponse = CommonResponse.createCommonResponse("创建成功！");
        LOGGER.info("createAccess commonResponse = {}", commonResponse);
        return commonResponse;
    }

    @ApiOperation(value = "更新黑白名单配置", notes = "更新黑白名单配置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "accessDTO", value = "配置信息", required = true, dataType = "AccessDTO"),
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string")
    })
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public CommonResponse updateAccess(@RequestBody AccessDTO accessDTO, @PathVariable String env) {
        LOGGER.info("updateAccess accessDTO = {}", accessDTO);
        validateParameter(accessDTO);
        validateRegistryAddress(accessDTO.getRegistryAddress());
        validateId(accessDTO.getId());
        String id = accessDTO.getId();
        String registryAddress = accessDTO.getRegistryAddress();
        id = id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
        accessDTO.setId(id);
        ConditionRouteDTO route = routeService.findConditionRoute(id, registryAddress);
        if (Objects.isNull(route)) {
            throw new ResourceNotFoundException("Unknown ID!");
        }
        routeService.updateAccess(accessDTO);
        CommonResponse commonResponse = CommonResponse.createCommonResponse("更新成功！");
        LOGGER.info("updateAccess commonResponse = {}", commonResponse);
        return commonResponse;
    }
}
