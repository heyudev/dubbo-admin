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

import com.google.gson.Gson;
import org.apache.dubbo.admin.common.CommonResponse;
import org.apache.dubbo.admin.common.EnvEnum;
import org.apache.dubbo.admin.common.OperationEnum;
import org.apache.dubbo.admin.common.exception.ParamValidationException;
import org.apache.dubbo.admin.common.request.ProviderRequest;
import org.apache.dubbo.admin.common.request.ServiceDetailRequest;
import org.apache.dubbo.admin.common.request.ServiceRequest;
import org.apache.dubbo.admin.common.util.Constants;
import org.apache.dubbo.admin.common.util.CookieUtil;
import org.apache.dubbo.admin.common.util.Tool;
import org.apache.dubbo.admin.model.domain.Consumer;
import org.apache.dubbo.admin.model.domain.Provider;
import org.apache.dubbo.admin.model.domain.Registry;
import org.apache.dubbo.admin.model.dto.ConsumerDTO;
import org.apache.dubbo.admin.model.dto.ProviderDTO;
import org.apache.dubbo.admin.model.dto.ServiceDTO;
import org.apache.dubbo.admin.model.dto.ServiceDetailDTO;
import org.apache.dubbo.admin.service.AuthService;
import org.apache.dubbo.admin.service.ConsumerService;
import org.apache.dubbo.admin.service.ProviderService;
import org.apache.dubbo.admin.service.RegistryService;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.identifier.MetadataIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/{env}")
public class ServiceController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceController.class);

    private final ProviderService providerService;
    private final ConsumerService consumerService;
    private final Gson gson;

    @Autowired
    public ServiceController(ProviderService providerService, ConsumerService consumerService) {
        this.providerService = providerService;
        this.consumerService = consumerService;
        this.gson = new Gson();
    }

    @Autowired
    private RegistryService registryService;

    @Autowired
    AuthService authService;

    /**
     * 服务查询
     *
     * @param pattern
     * @param filter
     * @param env
     * @param pageable
     * @return
     */
    @RequestMapping(value = "/service", method = RequestMethod.GET)
    public CommonResponse searchService(@RequestParam String pattern,
                                        @RequestParam String filter,
                                        @PathVariable String env,
                                        @RequestParam String registryAddress,
                                        Pageable pageable, ServletRequest request) {
        LOGGER.info("searchService pattern = {},filter = {},registryAddress = {},pageable = {}", pattern, filter, registryAddress, pageable);
        final Set<ServiceDTO> serviceDTOS = providerService.getServiceDTOS2(pattern, filter, env, registryAddress);

        //设置Service操作权限
        setPermission(request, serviceDTOS);

        final int total = serviceDTOS.size();
        final List<ServiceDTO> content =
                serviceDTOS.stream()
                        .skip(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .collect(Collectors.toList());

        final Page<ServiceDTO> page = new PageImpl<>(content, pageable, total);
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        commonResponse.setData(page);
        return commonResponse;
    }

    /**
     * 设置应用操作权限
     *
     * @param request
     * @param serviceDTOS
     */
    private void setPermission(ServletRequest request, Set<ServiceDTO> serviceDTOS) {
        Set<String> set = getPermissionsApplication(request);
//        LOGGER.info("permission set = {}", set);
        for (ServiceDTO serviceDTO : serviceDTOS) {
            if (set.contains(serviceDTO.getAppName())) {
                serviceDTO.setPermission(1);
            } else {
                serviceDTO.setPermission(0);
            }
        }
    }

    /**
     * 设置应用操作权限
     *
     * @param request
     * @param providers
     */
    private void setPermission(ServletRequest request, List<Provider> providers) {
        Set<String> set = getPermissionsApplication(request);
//        LOGGER.info("permission set = {}", set);
        for (Provider provider : providers) {
            if (set.contains(provider.getApplication())) {
                provider.setPermission(1);
            } else {
                provider.setPermission(0);
            }
        }
    }

    /**
     * 获取有权限的应用
     *
     * @param request
     * @return
     */
    private Set<String> getPermissionsApplication(ServletRequest request) {
        HttpServletRequest req = (HttpServletRequest) request;
        //AuthFilter 已对cookie做校验
        String accessToken = CookieUtil.getAccessToken(req);
        return authService.getApplication(accessToken);
    }

    //    /**
//     * 服务详情
//     *
//     * @param service group*service:version@registry
//     * @param env
//     * @return
//     */
//    @ApiOperation(value = "查询服务详情", notes = "查询服务详情")
//    @ApiImplicitParams({
//            @ApiImplicitParam(name = "service", value = "服务名", required = true, dataType = "string"),
//            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string"),
//            @ApiImplicitParam(name = "registryAddress", value = "注册中心地址：10.2.39.11:2181 或 10.2.39.11:2181,10.2.39.12:2181,10.2.39.13:2181", required = true, dataType = "string")
//    })
//    @RequestMapping(value = "/service/{service}", method = RequestMethod.GET)
//    public CommonResponse serviceDetail(@PathVariable String service, @PathVariable String env, @RequestParam String registryAddress) {
//        service = service.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
//        String group = Tool.getGroup(service);
//        String version = Tool.getVersion(service);
//        String interfaze = Tool.getInterface((service));
//        List<Provider> providers = providerService.findByService(service, registryAddress);
//
//        List<Consumer> consumers = consumerService.findByService(service, registryAddress);
//
//        String application = null;
//        if (providers != null && providers.size() > 0) {
//            application = providers.get(0).getApplication();
//        }
//        MetadataIdentifier identifier = new MetadataIdentifier(interfaze, version, group, Constants.PROVIDER_SIDE, application);
//        String metadata = providerService.getProviderMetaData(identifier);
//        ServiceDetailDTO serviceDetailDTO = new ServiceDetailDTO();
//        serviceDetailDTO.setConsumers(consumers);
//        serviceDetailDTO.setProviders(providers);
//        if (metadata != null) {
//            FullServiceDefinition serviceDefinition = gson.fromJson(metadata, FullServiceDefinition.class);
//            serviceDetailDTO.setMetadata(serviceDefinition);
//        }
//        serviceDetailDTO.setConsumers(consumers);
//        serviceDetailDTO.setProviders(providers);
//        serviceDetailDTO.setService(service);
//        serviceDetailDTO.setApplication(application);
//
//        CommonResponse commonResponse = CommonResponse.createCommonResponse();
//        commonResponse.setData(serviceDetailDTO);
//        return commonResponse;
//    }
    @PostMapping(value = "/service/detail")
    public CommonResponse serviceDetail(@PathVariable String env, @RequestBody ServiceRequest serviceRequest, ServletRequest request) {
        LOGGER.info("serviceDetail serviceRequest = {}", serviceRequest);
        if (serviceRequest == null || serviceRequest.getService() == null || serviceRequest.getRegistryAddress() == null) {
            throw new ParamValidationException("service or registryAddress is null");
        }
        String service = serviceRequest.getService();
        String registryAddress = serviceRequest.getRegistryAddress();
        service = service.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
        String group = Tool.getGroup(service);
        String version = Tool.getVersion(service);
        String interfaze = Tool.getInterface((service));
        List<Provider> providers = providerService.findByService2(service, registryAddress);

        List<Consumer> consumers = consumerService.findByService(service, registryAddress);

        String application = null;
        if (providers != null && providers.size() > 0) {
            application = providers.get(0).getApplication();
        }
        //设置权限
        setPermission(request, providers);

        MetadataIdentifier identifier = new MetadataIdentifier(interfaze, version, group, Constants.PROVIDER_SIDE, application);
        String metadata = providerService.getProviderMetaData(identifier);
        ServiceDetailDTO serviceDetailDTO = new ServiceDetailDTO();
        serviceDetailDTO.setConsumers(consumers);
        serviceDetailDTO.setProviders(providers);
        if (metadata != null) {
            FullServiceDefinition serviceDefinition = gson.fromJson(metadata, FullServiceDefinition.class);
            serviceDetailDTO.setMetadata(serviceDefinition);
        }
        serviceDetailDTO.setConsumers(consumers);
        serviceDetailDTO.setProviders(providers);
        serviceDetailDTO.setService(service);
        serviceDetailDTO.setGroup(group);
        serviceDetailDTO.setVersion(version);
        serviceDetailDTO.setApplication(application);
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        commonResponse.setData(serviceDetailDTO);
        return commonResponse;
    }

//    /**
//     * provider 详情
//     *
//     * @param service
//     * @param id
//     * @param env
//     * @return
//     */
//    @ApiOperation(value = "查询Provider详情", notes = "查询Provider详情")
//    @ApiImplicitParams({
//            @ApiImplicitParam(name = "service", value = "服务名", required = true, dataType = "string"),
//            @ApiImplicitParam(name = "id", value = "provider的id", required = true, dataType = "string"),
//            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string"),
//            @ApiImplicitParam(name = "registryAddress", value = "注册中心地址：10.2.39.11:2181 或 10.2.39.11:2181,10.2.39.12:2181,10.2.39.13:2181", required = true, dataType = "string")
//    })
//    @RequestMapping(value = "/service/provider/{service}/{id}", method = RequestMethod.GET)
//    public CommonResponse providerDetail(@PathVariable String service, @PathVariable String id, @PathVariable String env, @RequestParam String registryAddress) {
//        service = service.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
//        ProviderDTO providerDTO = providerService.findByServiceAndId(service, id, registryAddress);
//        CommonResponse commonResponse = CommonResponse.createCommonResponse();
//        commonResponse.setData(providerDTO);
//        return commonResponse;
//    }


    /**
     * provider 详情
     *
     * @param env
     * @param serviceDetailRequest
     * @return
     */
    @PostMapping(value = "/service/provider/detail")
    public CommonResponse providerDetail(@PathVariable String env, @RequestBody ServiceDetailRequest serviceDetailRequest) {
        LOGGER.info("providerDetail serviceDetailRequest = {}", serviceDetailRequest);
        if (serviceDetailRequest == null || serviceDetailRequest.getService() == null || serviceDetailRequest.getRegistryAddress() == null) {
            throw new ParamValidationException("service or registryAddress or id is null");
        }
        String service = serviceDetailRequest.getService();
        String registryAddress = serviceDetailRequest.getRegistryAddress();
        String id = serviceDetailRequest.getId();
        service = service.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
        ProviderDTO providerDTO = providerService.findByServiceAndId(service, id, registryAddress);
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        commonResponse.setData(providerDTO);
        return commonResponse;
    }


    /**
     * Provider操作
     *
     * @param env
     * @param providerRequest
     * @return
     */
    @PostMapping(value = "/service/provider/operate")
    public CommonResponse providerOperate(@PathVariable String env, @RequestBody ProviderRequest providerRequest) {
        LOGGER.info("providerOperate providerRequest = {}", providerRequest);
        if (providerRequest == null || providerRequest.getRegistryAddress() == null || providerRequest.getService() == null || providerRequest.getOperation() == null
                || providerRequest.getIds() == null || providerRequest.getIds().isEmpty()) {
            throw new ParamValidationException("parameter vaildate failure");
        }
        String registryAddress = providerRequest.getRegistryAddress();
        List<String> ids = providerRequest.getIds();
        Integer operation = providerRequest.getOperation();
        String service = providerRequest.getService();
        CommonResponse commonResponse = CommonResponse.createCommonResponse();

        if (OperationEnum.ENABLE.getValue() == operation.intValue()) {
            for (String id : ids) {
                providerService.enableProvider(id, registryAddress);
            }
        } else if (OperationEnum.DISABLE.getValue() == operation.intValue()) {
            Registry registry = registryService.getRegistryByAddress(registryAddress);
            if (registry == null || registry.getEnv() == null) {
                commonResponse.fail("注册中心参数错误！");
                return commonResponse;
            }
            //如果是生产环境 不能全部禁用服务
            if (EnvEnum.RELEASE.getValue() == registry.getEnv().intValue()) {
                List<Provider> list = providerService.findByService2(service, registryAddress);
                Set<String> enableProviders = new HashSet<>();
                for (Provider provider : list) {
                    if ("启用".equals(provider.getStatus())) {
                        enableProviders.add(provider.getHash());
                    }
                }
                for (String id : ids) {
                    if (enableProviders.contains(id)) {
                        enableProviders.remove(id);
                    }
                }
                if (enableProviders.size() == 0) {
                    commonResponse.fail("生产服务不能全部被禁用！");
                    return commonResponse;
                }
            }
            for (String id : ids) {
                providerService.disableProvider(id, registryAddress);
            }
        }
        commonResponse.success("操作成功");
        LOGGER.info("providerOperate commonResponse = {}", commonResponse);
        return commonResponse;
    }

    /**
     * 权重调整
     *
     * @param env
     * @param serviceDetailRequest
     * @return
     */
    @PostMapping(value = "/service/provider/adjustWeight")
    public CommonResponse adjustWeight(@PathVariable String env, @RequestBody ServiceDetailRequest serviceDetailRequest) {
        LOGGER.info("adjustWeight serviceDetailRequest = {}", serviceDetailRequest);
        if (serviceDetailRequest == null || serviceDetailRequest.getRegistryAddress() == null && serviceDetailRequest.getId() == null) {
            throw new ParamValidationException("parameter vaildate failure");
        }
        if (serviceDetailRequest.getWeight() == null || serviceDetailRequest.getWeight() <= 0) {
            throw new ParamValidationException("weight must be greater than 0");
        }
        String registryAddress = serviceDetailRequest.getRegistryAddress();
        String id = serviceDetailRequest.getId();
        Integer weight = serviceDetailRequest.getWeight();
        providerService.adjustWeight(id, weight, registryAddress);
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        commonResponse.success("操作成功");
        LOGGER.info("adjustWeight commonResponse = {}", commonResponse);
        return commonResponse;
    }


//    /**
//     * consumer 详情
//     *
//     * @param service
//     * @param id
//     * @param env
//     * @return
//     */
//    @ApiOperation(value = "查询Consumer详情", notes = "查询Consumer详情")
//    @ApiImplicitParams({
//            @ApiImplicitParam(name = "service", value = "服务名", required = true, dataType = "string"),
//            @ApiImplicitParam(name = "id", value = "consumer的id", required = true, dataType = "string"),
//            @ApiImplicitParam(name = "env", value = "环境：预留字段，传dev即可", required = true, dataType = "string"),
//            @ApiImplicitParam(name = "registryAddress", value = "注册中心地址：10.2.39.11:2181 或 10.2.39.11:2181,10.2.39.12:2181,10.2.39.13:2181", required = true, dataType = "string")
//    })
//    @RequestMapping(value = "/service/consumer/{service}/{id}", method = RequestMethod.GET)
//    public CommonResponse consumerDetail(@PathVariable String service, @PathVariable String id, @PathVariable String env, @RequestParam String registryAddress) {
//        service = service.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
//        ConsumerDTO consumerDTO = consumerService.findByServiceAndId(service, id, registryAddress);
//
//        CommonResponse commonResponse = CommonResponse.createCommonResponse();
//        commonResponse.setData(consumerDTO);
//        return commonResponse;
//    }

    /**
     * @param env
     * @param serviceDetailRequest
     * @return
     */
    @PostMapping(value = "/service/consumer/detail")
    public CommonResponse consumerDetail(@PathVariable String env, @RequestBody ServiceDetailRequest serviceDetailRequest) {
        LOGGER.info("consumerDetail serviceDetailRequest = {}", serviceDetailRequest);
        if (serviceDetailRequest == null || serviceDetailRequest.getService() == null || serviceDetailRequest.getRegistryAddress() == null) {
            throw new ParamValidationException("service or registryAddress or id is null");
        }
        String service = serviceDetailRequest.getService();
        String registryAddress = serviceDetailRequest.getRegistryAddress();
        String id = serviceDetailRequest.getId();
        service = service.replace(Constants.ANY_VALUE, Constants.PATH_SEPARATOR);
        ConsumerDTO consumerDTO = consumerService.findByServiceAndId(service, id, registryAddress);

        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        commonResponse.setData(consumerDTO);
        return commonResponse;
    }


    @RequestMapping(value = "/services", method = RequestMethod.GET)
    public CommonResponse allServices(@PathVariable String env, @RequestParam String registryAddress) {
        LOGGER.info("allServices registryAddress = {}", registryAddress);
        Set<String> stringSet = providerService.findServices(registryAddress);
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        commonResponse.setData(stringSet);
        return commonResponse;
    }

    @RequestMapping(value = "/applications", method = RequestMethod.GET)
    public CommonResponse allApplications(@PathVariable String env, @RequestParam String registryAddress) {
        LOGGER.info("allApplications registryAddress = {}", registryAddress);
        Set<String> stringSet = providerService.findApplications(registryAddress);

        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        commonResponse.setData(stringSet);
        return commonResponse;
    }

    @RequestMapping(value = "/myServices", method = RequestMethod.GET)
    public CommonResponse myServices(@PathVariable String env, ServletRequest request, @RequestParam String registryAddress) {
        LOGGER.info("myServices registryAddress = {}", registryAddress);
        Set<String> stringSet = providerService.findMyServices(getPermissionsApplication(request), registryAddress);
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        commonResponse.setData(stringSet);
        return commonResponse;
    }

    @RequestMapping(value = "/myApplications", method = RequestMethod.GET)
    public CommonResponse myApplications(@PathVariable String env, ServletRequest request) {
        LOGGER.info("myApplications...");
        Set<String> stringSet = getPermissionsApplication(request);
        CommonResponse commonResponse = CommonResponse.createCommonResponse();
        commonResponse.setData(stringSet);
        return commonResponse;
    }
}
