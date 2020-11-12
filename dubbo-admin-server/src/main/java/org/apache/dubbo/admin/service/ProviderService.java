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
package org.apache.dubbo.admin.service;

import org.apache.dubbo.admin.model.domain.Provider;
import org.apache.dubbo.admin.model.dto.ProviderDTO;
import org.apache.dubbo.admin.model.dto.ServiceDTO;
import org.apache.dubbo.metadata.identifier.MetadataIdentifier;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ProviderService
 */
public interface ProviderService {

    void create(Provider provider);

    /**
     * 启用服务 dubbo2.6
     *
     * @param id
     * @param registryAddress
     */
    void enableProvider(String id, String registryAddress);

    /**
     * 禁用服务 dubbo2.6
     *
     * @param id
     * @param registryAddress
     */
    void disableProvider(String id, String registryAddress);

    /**
     * 设置权重
     *
     * @param id
     * @param weight
     * @param registryAddress
     */
    void adjustWeight(String id, Integer weight, String registryAddress);

    void deleteStaticProvider(String id, String registryAddress);

    void updateProvider(Provider provider, String registryAddress);

    Provider findProvider(String id, String registryAddress);

    String getProviderMetaData(MetadataIdentifier providerIdentifier);

    /**
     * Get all provider's service name
     *
     * @return list of all provider's service name
     */
    Set<String> findServices(String registryAddress);

    /**
     * 获取有权限的所有的ServiceName
     *
     * @return
     */
    Set<String> findMyServices(Set<String> applications, String registryAddress);

    /**
     * 获取所有的ServiceName 带注册中心地址
     *
     * @return
     */
    Set<String> findServicesWithRegistry(String registryAddress);


    String findServiceVersion(String serviceName, String application, String registryAddress);

    String findVersionInApplication(String application, String registryAddress);

    List<String> findAddresses(String registryAddress);

    List<String> findAddressesByApplication(String application, String registryAddress);

    List<String> findAddressesByService(String serviceName, String registryAddress);

    List<String> findApplicationsByServiceName(String serviceName, String registryAddress);

    /**
     * Get provider list with specific service name.
     *
     * @param serviceName specific service name, cannot be fuzzy string
     * @return list of provider object
     */
    List<Provider> findByService(String serviceName, String registryAddress);

    /**
     *
     * @param serviceName
     * @param registryAddress
     * @return
     */
    List<Provider> findByService2(String serviceName, String registryAddress);

    /**
     * Get provider list with specific service name and registry
     *
     * @param serviceName
     * @param registry
     * @return
     */
    List<Provider> findByServiceAndRegistry(String serviceName, String registry);

    /**
     * Get provider list with specific service name without registry
     *
     * @param serviceName
     * @return
     */
    List<Provider> findByServiceWithoutRegistry(String serviceName, String registryAddress);

    List<Provider> findByAppandService(String app, String serviceName, String registryAddress);

    /**
     * 测试
     * @param service
     * @param registryAddress
     * @return
     */
    @Deprecated
    String test(String service, String registryAddress);

    List<Provider> findAll(String registryAddress);

    /**
     * Get provider list with specific ip address.
     *
     * @param providerAddress provider's ip address
     * @return list of provider object
     */
    List<Provider> findByAddress(String providerAddress, String registryAddress);

    List<String> findServicesByAddress(String providerAddress, String registryAddress);

    Set<String> findApplications(String registryAddress);

    /**
     * Get provider list with specific application name.
     *
     * @param application specific application name
     * @return list of provider object
     */
    List<Provider> findByApplication(String application, String registryAddress);

    List<String> findServicesByApplication(String application, String registryAddress);

    List<String> findMethodsByService(String serviceName, String registryAddress);

    Provider findByServiceAndAddress(String service, String address, String registryAddress);

    /**
     * 根据服务和md5获取详细信息
     *
     * @param service 服务 group/service:version@registry
     * @param id      md5
     * @return
     */
    ProviderDTO findByServiceAndId(String service, String id, String registryAddress);

    /**
     * Get a set of service data object.
     * <p>
     * ServiceDTO object contains base information include
     * service name , application, group and version.
     *
     * @param pattern {@code String} type of search
     * @param filter  {@code String} input filter string
     * @param env     {@code String}the environment of front end
     * @return a set of services for fore-end page
     */
    @Deprecated
    Set<ServiceDTO> getServiceDTOS(String pattern, String filter, String env, String registryAddress);

    /**
     * @param pattern
     * @param filter
     * @param env
     * @param registryAddress
     * @return
     */
    Set<ServiceDTO> getServiceDTOS2(String pattern, String filter, String env, String registryAddress);
}
