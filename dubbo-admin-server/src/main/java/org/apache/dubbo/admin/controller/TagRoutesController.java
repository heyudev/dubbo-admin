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
import org.apache.dubbo.admin.common.exception.VersionValidationException;
import org.apache.dubbo.admin.common.util.Constants;
import org.apache.dubbo.admin.model.dto.TagRouteDTO;
import org.apache.dubbo.admin.service.ProviderService;
import org.apache.dubbo.admin.service.RouteService;
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
@Deprecated
@RestController
@RequestMapping("/{env}/rules/route/tag")
public class TagRoutesController {


    private final RouteService routeService;
    private final ProviderService providerService;

    @Autowired
    public TagRoutesController(RouteService routeService, ProviderService providerService) {
        this.routeService = routeService;
        this.providerService = providerService;
    }

    @ApiOperation(value = "创建标签路由配置", notes = "创建标签路由配置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "weightDTO", value = "配置信息", required = true, dataType = "WeightDTO"),
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string"),
            @ApiImplicitParam(name = "registryAddress", value = "注册中心地址：10.2.39.11:2181 或 10.2.39.11:2181,10.2.39.12:2181,10.2.39.13:2181", required = true, dataType = "string")
    })
    @RequestMapping(method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse createRule(@RequestBody TagRouteDTO routeDTO, @PathVariable String env, @RequestParam String registryAddress) {
        String app = routeDTO.getApplication();
        if (StringUtils.isEmpty(app)) {
            throw new ParamValidationException("app is Empty!");
        }
        if (providerService.findVersionInApplication(app, registryAddress).equals("2.6")) {
            throw new VersionValidationException("dubbo 2.6 does not support tag route");
        }
        routeService.createTagRoute(routeDTO, registryAddress);
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        commonResponse.setData(true);
        return commonResponse;
    }

    @ApiOperation(value = "更新标签路由配置", notes = "更新标签路由配置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "标签路由配置ID", required = true, dataType = "string"),
            @ApiImplicitParam(name = "weightDTO", value = "配置信息", required = true, dataType = "WeightDTO"),
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string"),
            @ApiImplicitParam(name = "registryAddress", value = "注册中心地址：10.2.39.11:2181 或 10.2.39.11:2181,10.2.39.12:2181,10.2.39.13:2181", required = true, dataType = "string")
    })
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public CommonResponse updateRule(@PathVariable String id, @RequestBody TagRouteDTO routeDTO, @PathVariable String env, @RequestParam String registryAddress) {

        id = id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
        String app = routeDTO.getApplication();
        if (providerService.findVersionInApplication(app, registryAddress).equals("2.6")) {
            throw new VersionValidationException("dubbo 2.6 does not support tag route");
        }
        if (routeService.findTagRoute(id, registryAddress) == null) {
            throw new ResourceNotFoundException("can not find tag route, Id: " + id);
        }
        routeService.updateTagRoute(routeDTO, registryAddress);
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        commonResponse.setData(true);
        return commonResponse;

    }

    @ApiOperation(value = "查询标签路由配置列表", notes = "查询标签路由配置列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "application", value = "应用名", required = false, dataType = "string"),
            @ApiImplicitParam(name = "service", value = "服务名", required = false, dataType = "string"),
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string"),
            @ApiImplicitParam(name = "registryAddress", value = "注册中心地址：10.2.39.11:2181 或 10.2.39.11:2181,10.2.39.12:2181,10.2.39.13:2181", required = true, dataType = "string")
    })
    @RequestMapping(method = RequestMethod.GET)
    public CommonResponse searchRoutes(@RequestParam String application, @PathVariable String env, @RequestParam String registryAddress) {
        if (StringUtils.isBlank(application)) {
            throw new ParamValidationException("application is required.");
        }
        List<TagRouteDTO> result = new ArrayList<>();
        String version = "2.6";
        try {
            version = providerService.findVersionInApplication(application, registryAddress);
        } catch (ParamValidationException e) {
            //ignore
        }
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        if (version.equals("2.6")) {
            commonResponse.setData(result);
            return commonResponse;
        }

        TagRouteDTO tagRoute = routeService.findTagRoute(application, registryAddress);
        if (tagRoute != null) {
            result.add(tagRoute);
        }
        commonResponse.setData(result);
        return commonResponse;
    }

    @ApiOperation(value = "查看标签路由配置信息", notes = "查看标签路由配置信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "标签路由配置ID", required = true, dataType = "string"),
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string"),
            @ApiImplicitParam(name = "registryAddress", value = "注册中心地址：10.2.39.11:2181 或 10.2.39.11:2181,10.2.39.12:2181,10.2.39.13:2181", required = true, dataType = "string")
    })
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public CommonResponse detailRoute(@PathVariable String id, @PathVariable String env, @RequestParam String registryAddress) {
        id = id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
        TagRouteDTO tagRoute = routeService.findTagRoute(id, registryAddress);
        if (tagRoute == null) {
            throw new ResourceNotFoundException("Unknown ID!");
        }
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        commonResponse.setData(tagRoute);
        return commonResponse;
    }

    @ApiOperation(value = "删除标签路由配置", notes = "删除标签路由配置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "标签路由配置ID", required = true, dataType = "string"),
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string"),
            @ApiImplicitParam(name = "registryAddress", value = "注册中心地址：10.2.39.11:2181 或 10.2.39.11:2181,10.2.39.12:2181,10.2.39.13:2181", required = true, dataType = "string")
    })
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public CommonResponse deleteRoute(@PathVariable String id, @PathVariable String env, @RequestParam String registryAddress) {
        id = id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
        routeService.deleteTagRoute(id, registryAddress);
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        commonResponse.setData(true);
        return commonResponse;
    }

    @ApiOperation(value = "启用标签路由配置", notes = "启用标签路由配置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "标签路由配置ID", required = true, dataType = "string"),
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string"),
            @ApiImplicitParam(name = "registryAddress", value = "注册中心地址：10.2.39.11:2181 或 10.2.39.11:2181,10.2.39.12:2181,10.2.39.13:2181", required = true, dataType = "string")
    })

    @RequestMapping(value = "/enable/{id}", method = RequestMethod.PUT)
    public CommonResponse enableRoute(@PathVariable String id, @PathVariable String env, @RequestParam String registryAddress) {
        id = id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
        routeService.enableTagRoute(id, registryAddress);
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        commonResponse.setData(true);
        return commonResponse;
    }

    @ApiOperation(value = "禁用标签路由配置", notes = "禁用标签路由配置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "标签路由配置ID", required = true, dataType = "string"),
            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string"),
            @ApiImplicitParam(name = "registryAddress", value = "注册中心地址：10.2.39.11:2181 或 10.2.39.11:2181,10.2.39.12:2181,10.2.39.13:2181", required = true, dataType = "string")
    })

    @RequestMapping(value = "/disable/{id}", method = RequestMethod.PUT)
    public CommonResponse disableRoute(@PathVariable String id, @PathVariable String env, @RequestParam String registryAddress) {
        id = id.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
        routeService.disableTagRoute(id, registryAddress);
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        commonResponse.setData(true);
        return commonResponse;
    }
}

