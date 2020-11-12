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
import org.apache.dubbo.admin.model.dto.DynamicConfigDTO;
import org.apache.dubbo.admin.service.OverrideService;
import org.apache.dubbo.admin.service.ProviderService;
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
@RequestMapping("/{env}/rules/override")
public class OverridesController extends BaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalanceController.class);

    private final OverrideService overrideService;
    private final ProviderService providerService;

    @Autowired
    public OverridesController(OverrideService overrideService, ProviderService providerService) {
        this.overrideService = overrideService;
        this.providerService = providerService;
    }

    @ApiOperation(value = "创建动态配置", notes = "创建动态配置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "overrideDTO", value = "配置信息", required = true, dataType = "DynamicConfigDTO"),
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string")
    })
    @RequestMapping(method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse createOverride(@RequestBody DynamicConfigDTO overrideDTO, @PathVariable String env) {
        LOGGER.info("createOverride overrideDTO = {}", overrideDTO);
        validateParameter(overrideDTO);
        validateRegistryAddress(overrideDTO.getRegistryAddress());
        validateService(overrideDTO.getService());

        String serviceName = overrideDTO.getService();
        String application = overrideDTO.getApplication();
        String registryAddress = overrideDTO.getRegistryAddress();
        if (StringUtils.isEmpty(serviceName) && StringUtils.isEmpty(application)) {
            throw new ParamValidationException("serviceName and application are Empty!");
        }
        if (StringUtils.isNotEmpty(application) && providerService.findVersionInApplication(application, registryAddress).equals("2.6")) {
            throw new VersionValidationException("dubbo 2.6 does not support application scope dynamic config");
        }
        overrideService.saveOverride(overrideDTO);
        CommonResponse commonResponse = CommonResponse.createCommonResponse("创建成功！");
        commonResponse.setData(true);
        LOGGER.info("createOverride commonResponse = {}", commonResponse);
        return commonResponse;
    }

    @ApiOperation(value = "更新动态配置", notes = "更新动态配置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "overrideDTO", value = "配置信息", required = true, dataType = "DynamicConfigDTO"),
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string"),
    })
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public CommonResponse updateOverride(@RequestBody DynamicConfigDTO overrideDTO, @PathVariable String env) {
        LOGGER.info("updateOverride overrideDTO = {}", overrideDTO);
        validateParameter(overrideDTO);
        validateRegistryAddress(overrideDTO.getRegistryAddress());
        validateId(overrideDTO.getId());

        String id = overrideDTO.getId();
        String registryAddress = overrideDTO.getRegistryAddress();
        id = id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
        DynamicConfigDTO old = overrideService.findOverride(id, registryAddress);
        if (old == null) {
            throw new ResourceNotFoundException("Unknown ID!");
        }
        overrideDTO.setId(id);
        overrideService.updateOverride(overrideDTO);
        CommonResponse commonResponse = CommonResponse.createCommonResponse("更新成功！");
        commonResponse.setData(true);
        LOGGER.info("updateOverride commonResponse = {}", commonResponse);
        return commonResponse;
    }

    @ApiOperation(value = "查询动态配置列表", notes = "查询动态配置列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "application", value = "应用名", required = false, dataType = "string"),
            @ApiImplicitParam(name = "service", value = "服务名", required = false, dataType = "string"),
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string"),
            @ApiImplicitParam(name = "registryAddress", value = "注册中心地址：10.2.39.11:2181 或 10.2.39.11:2181,10.2.39.12:2181,10.2.39.13:2181", required = true, dataType = "string")
    })
    @RequestMapping(method = RequestMethod.GET)
    public CommonResponse searchOverride(@RequestParam(required = false) String service,
                                         @RequestParam(required = false) String application,
                                         @PathVariable String env, @RequestParam String registryAddress) {
        LOGGER.info("searchOverride service = {},application = {},registryAddress = {}", service, application, registryAddress);
        DynamicConfigDTO override = null;
        List<DynamicConfigDTO> result = new ArrayList<>();
        if (StringUtils.isNotBlank(service)) {
            override = overrideService.findOverride(service, registryAddress);
        } else if (StringUtils.isNotBlank(application)) {
            override = overrideService.findOverride(application, registryAddress);
        } else {
            throw new ParamValidationException("Either Service or application is required.");
        }
        if (override != null) {
            override.setRegistryAddress(registryAddress);
            result.add(override);
        }
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        commonResponse.setData(result);
        return commonResponse;
    }

    @ApiOperation(value = "查看动态配置信息", notes = "查看动态配置信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string"),
            @ApiImplicitParam(name = "baseRequest", value = "需要参数：id：配置ID；registryAddress:注册中心", required = true, dataType = "BaseRequest")
    })
    @RequestMapping(value = "/detail", method = RequestMethod.POST)
    public CommonResponse detailOverride(@PathVariable String env, @RequestBody BaseRequest baseRequest) {
        LOGGER.info("detailOverride baseRequest = {}", baseRequest);
        validateBaseRequest(baseRequest);

        String id = baseRequest.getId();
        String registryAddress = baseRequest.getRegistryAddress();
        id = id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
        DynamicConfigDTO override = overrideService.findOverride(id, registryAddress);
        if (override == null) {
            throw new ResourceNotFoundException("Unknown ID!");
        }
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        commonResponse.setData(override);
        return commonResponse;
    }

    @ApiOperation(value = "删除动态配置", notes = "删除动态配置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string"),
            @ApiImplicitParam(name = "baseRequest", value = "需要参数：id：配置ID；registryAddress:注册中心", required = true, dataType = "BaseRequest")
    })
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public CommonResponse deleteOverride(@PathVariable String env, @RequestBody BaseRequest baseRequest) {
        LOGGER.info("deleteOverride baseRequest = {}", baseRequest);
        validateBaseRequest(baseRequest);
        String id = baseRequest.getId();
        String registryAddress = baseRequest.getRegistryAddress();
        id = id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
        CommonResponse commonResponse = CommonResponse.createCommonResponse("删除成功！");
        try {
            overrideService.deleteOverride(id, registryAddress);
        } catch (SystemException e) {
            commonResponse.fail(e.getMessage());
        }
        LOGGER.info("deleteOverride commonResponse = {}", commonResponse);
//        commonResponse.setData(true);
        return commonResponse;
    }

    @ApiOperation(value = "启用动态配置", notes = "启用动态配置")
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
        overrideService.enableOverride(id, registryAddress);
        CommonResponse commonResponse = CommonResponse.createCommonResponse("启用成功！");
        commonResponse.setData(true);
        LOGGER.info("enableRoute commonResponse = {}", commonResponse);
        return commonResponse;
    }

    @ApiOperation(value = "禁用动态配置", notes = "禁用动态配置")
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
        overrideService.disableOverride(id, registryAddress);
        CommonResponse commonResponse = CommonResponse.createCommonResponse("禁用成功！");
        commonResponse.setData(true);
        LOGGER.info("disableRoute commonResponse = {}", commonResponse);
        return commonResponse;
    }
}
