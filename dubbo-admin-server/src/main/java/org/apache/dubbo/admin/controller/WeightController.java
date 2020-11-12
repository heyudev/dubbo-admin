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
import org.apache.dubbo.admin.model.dto.WeightDTO;
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
@RequestMapping("/{env}/rules/weight")
public class WeightController extends BaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceController.class);

    private final OverrideService overrideService;
    private final ProviderService providerService;

    @Autowired
    public WeightController(OverrideService overrideService, ProviderService providerService) {
        this.overrideService = overrideService;
        this.providerService = providerService;
    }

    @ApiOperation(value = "创建权重调整配置", notes = "创建权重调整配置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "weightDTO", value = "配置信息", required = true, dataType = "WeightDTO"),
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string")
    })
    @RequestMapping(method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse createWeight(@RequestBody WeightDTO weightDTO, @PathVariable String env) {
        LOGGER.info("createWeight weightDTO = {}", weightDTO);
        validateParameter(weightDTO);
        validateRegistryAddress(weightDTO.getRegistryAddress());
        validateService(weightDTO.getService());

//        if ((StringUtils.isBlank(weightDTO.getService()) && StringUtils.isBlank(weightDTO.getApplication())) || StringUtils.isBlank(weightDTO.getRegistryAddress())) {
//            throw new ParamValidationException("Either Service or application is required.");
//        }
        String application = weightDTO.getApplication();
        if (StringUtils.isNotEmpty(application) && this.providerService.findVersionInApplication(application, weightDTO.getRegistryAddress()).equals("2.6")) {
            throw new VersionValidationException("dubbo 2.6 does not support application scope blackwhite list config");
        }
        overrideService.saveWeight(weightDTO);
        CommonResponse commonResponse = CommonResponse.createCommonResponse("创建成功！");
        commonResponse.setData(true);
        LOGGER.info("createWeight commonResponse = {}", commonResponse);
        return commonResponse;
    }

    @ApiOperation(value = "更新权重调整配置", notes = "更新权重调整配置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "weightDTO", value = "配置信息", required = true, dataType = "WeightDTO"),
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string")
    })
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public CommonResponse updateWeight(@RequestBody WeightDTO weightDTO, @PathVariable String env) {
        LOGGER.info("updateWeight weightDTO = {}", weightDTO);
        validateParameter(weightDTO);
        validateRegistryAddress(weightDTO.getRegistryAddress());

        String id = weightDTO.getId();
        if (id == null) {
            throw new ParamValidationException("Unknown ID!");
        }
        id = id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
        WeightDTO weight = overrideService.findWeight(id, weightDTO.getRegistryAddress());
        if (weight == null) {
            throw new ResourceNotFoundException("Unknown ID!");
        }
        weightDTO.setId(id);
        overrideService.updateWeight(weightDTO);
        CommonResponse commonResponse = CommonResponse.createCommonResponse("更新成功！");
        commonResponse.setData(true);
        LOGGER.info("updateWeight commonResponse = {}", commonResponse);
        return commonResponse;
    }

    @ApiOperation(value = "查询权重调整配置列表", notes = "查询权重调整配置列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "application", value = "应用名", required = false, dataType = "string"),
            @ApiImplicitParam(name = "service", value = "服务名", required = false, dataType = "string"),
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string"),
            @ApiImplicitParam(name = "registryAddress", value = "注册中心地址：10.2.39.11:2181 或 10.2.39.11:2181,10.2.39.12:2181,10.2.39.13:2181", required = true, dataType = "string")
    })
    @RequestMapping(method = RequestMethod.GET)
    public CommonResponse searchWeight(@RequestParam(required = false) String service,
                                       @RequestParam(required = false) String application,
                                       @PathVariable String env, @RequestParam String registryAddress) {
        LOGGER.info("searchWeight service = {},application = {},registryAddress = {}", service, application, registryAddress);
        if (StringUtils.isBlank(service) && StringUtils.isBlank(application)) {
            throw new ParamValidationException("Either service or application is required");
        }
        WeightDTO weightDTO;
        if (StringUtils.isNotBlank(application)) {
            weightDTO = overrideService.findWeight(application, registryAddress);
        } else {
            weightDTO = overrideService.findWeight(service, registryAddress);
        }
        List<WeightDTO> weightDTOS = new ArrayList<>();
        if (weightDTO != null) {
            weightDTO.setRegistryAddress(registryAddress);
            weightDTOS.add(weightDTO);
        }

        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        commonResponse.setData(weightDTOS);
        return commonResponse;
    }

    @ApiOperation(value = "查看权重调整配置信息", notes = "查看权重调整配置信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string"),
            @ApiImplicitParam(name = "baseRequest", value = "需要参数：id：配置ID；registryAddress:注册中心", required = true, dataType = "BaseRequest")
    })
    @RequestMapping(value = "/detail", method = RequestMethod.POST)
    public CommonResponse detailWeight(@PathVariable String env, @RequestBody BaseRequest baseRequest) {
        LOGGER.info("detailWeight baseRequest = {}", baseRequest);
        validateBaseRequest(baseRequest);
        String id = baseRequest.getId();
        String registryAddress = baseRequest.getRegistryAddress();
        id = id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
        WeightDTO weightDTO = overrideService.findWeight(id, registryAddress);
        if (weightDTO == null) {
            throw new ResourceNotFoundException("Unknown ID!");
        }
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        commonResponse.setData(weightDTO);
        return commonResponse;
    }

    @ApiOperation(value = "删除权重调整配置", notes = "删除权重调整配置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string"),
            @ApiImplicitParam(name = "baseRequest", value = "需要参数：id：配置ID；registryAddress:注册中心", required = true, dataType = "BaseRequest")
    })
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public CommonResponse deleteWeight(@PathVariable String env, @RequestBody BaseRequest baseRequest) {
        LOGGER.info("deleteWeight baseRequest = {}", baseRequest);
        validateBaseRequest(baseRequest);
        String id = baseRequest.getId();
        String registryAddress = baseRequest.getRegistryAddress();
        id = id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
        CommonResponse commonResponse = CommonResponse.createCommonResponse("删除成功！");
        try {
            overrideService.deleteWeight(id, registryAddress);
        } catch (SystemException e) {
            commonResponse.fail(e.getMessage());
        }
        LOGGER.info("deleteWeight commonResponse = {}", commonResponse);
//        commonResponse.setData(true);
        return commonResponse;
    }
}
