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

package org.apache.dubbo.admin.config;

import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.admin.common.exception.ConfigurationException;
import org.apache.dubbo.admin.common.util.Constants;
import org.apache.dubbo.admin.dao.RegistryDao;
import org.apache.dubbo.admin.registry.config.GovernanceConfiguration;
import org.apache.dubbo.admin.registry.config.impl.ZookeeperConfiguration;
import org.apache.dubbo.admin.registry.metadata.MetaDataCollector;
import org.apache.dubbo.admin.registry.metadata.impl.NoOpMetadataCollector;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


@Configuration
public class ConfigCenter {

    //centers in dubbo 2.7
    @Value("${admin.config-center:}")
    private String configCenter;

    @Value("${admin.registry.address:}")
    private String registryAddress;

    @Value("${admin.metadata-report.address:}")
    private String metadataAddress;

    @Value("${admin.registry.group:}")
    private String group;

    @Value("${admin.config-center.username:}")
    private String username;
    @Value("${admin.config-center.password:}")
    private String password;

    private static final Logger logger = LoggerFactory.getLogger(ConfigCenter.class);

    private URL configCenterUrl;
    private URL metadataUrl;

    @Resource
    private RegistryDao registryDao;

    /*
     * generate dynamic configuration client
     */
    @Bean("governanceConfiguration")
    GovernanceConfiguration getDynamicConfiguration() {
        if (StringUtils.isNotEmpty(configCenter)) {
            configCenterUrl = formUrl(configCenter, group, username, password);
            GovernanceConfiguration dynamicConfiguration = getGovernanceConfigurationFromURL(configCenterUrl);
            if (dynamicConfiguration != null) {
                return dynamicConfiguration;
            }
        }
        if (StringUtils.isNotEmpty(registryAddress)) {
            URL registryUrl = formUrl(registryAddress, group, username, password);
            GovernanceConfiguration dynamicConfiguration = getGovernanceConfigurationFromURL(registryUrl);
            logger.warn("you are using dubbo.registry.address, which is not recommend, please refer to: https://github.com/apache/incubator-dubbo-admin/wiki/Dubbo-Admin-configuration");
            return dynamicConfiguration;
        }
        throw new ConfigurationException("Either config center or registry address is needed, please refer to https://github.com/apache/incubator-dubbo-admin/wiki/Dubbo-Admin-configuration");
    }

    /**
     * generate dynamic configuration client
     *
     * @return
     */
    @Bean("dynamicConfigurations")
    List<GovernanceConfiguration> getDynamicConfigurations() {
        List<GovernanceConfiguration> dynamicConfigurations = new ArrayList<>();
        // 动态配置注册中心 从数据库读取
        List<org.apache.dubbo.admin.model.domain.Registry> list = registryDao.getAllRegistryOfAuto();
        logger.info("registry list = " + list);
        if (list != null && !list.isEmpty()) {
            for (org.apache.dubbo.admin.model.domain.Registry registry : list) {
                URL registryUrl = formUrl(Constants.REGISTRY_ZOOKEEPER_PREFIX + registry.getRegAddress(), registry.getRegGroup(), registry.getUsername(), registry.getPassword());
                GovernanceConfiguration dynamicConfiguration = getZookeeperConfigurationFromURL(registryUrl);
                dynamicConfiguration.setUrl(registryUrl);
                dynamicConfiguration.init();
                if (dynamicConfiguration != null) {
                    dynamicConfigurations.add(dynamicConfiguration);
                }
            }
        }
        return dynamicConfigurations;
    }

    /**
     * generate registry client
     *
     * @return
     */
    @Bean("registries")
    List<Registry> getRegistries(GovernanceConfiguration governanceConfiguration) {
        List<Registry> registries = new LinkedList<>();
        RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();

//        String config = governanceConfiguration.getConfig(Constants.GLOBAL_CONFIG_PATH);
//
//        if (StringUtils.isNotEmpty(config)) {
//            for (String s : config.split("\n")) {
//                if (s.startsWith(Constants.REGISTRY_ADDRESS)) {
//                    String registryAddress = s.split("=")[1].trim();
//                    URL registryUrl = formUrl(registryAddress, group, username, password);
//                    registries.add(registryFactory.getRegistry(registryUrl));
//                }
//            }
//        }

        if (registries.isEmpty()) {
            if (StringUtils.isBlank(registryAddress)) {
                throw new ConfigurationException("Either config center or registry address is needed, please refer to https://github.com/apache/incubator-dubbo-admin/wiki/Dubbo-Admin-configuration");
            }
            URL registryUrl = formUrl(registryAddress, group, username, password);
            registries.add(registryFactory.getRegistry(registryUrl));
        }

        // 动态配置注册中心 从数据库读取
        List<org.apache.dubbo.admin.model.domain.Registry> list = registryDao.getAllRegistryOfAuto();
        logger.info("registry list = " + list);
        if (list != null && !list.isEmpty()) {
            for (org.apache.dubbo.admin.model.domain.Registry registry : list) {
                URL registryUrl = formUrl(Constants.REGISTRY_ZOOKEEPER_PREFIX + registry.getRegAddress(), registry.getRegGroup(), registry.getUsername(), registry.getPassword());
                registries.add(registryFactory.getRegistry(registryUrl));
            }
        }

        return registries;
    }

    /*
     * generate metadata client
     */
    @Bean
    MetaDataCollector getMetadataCollector(GovernanceConfiguration governanceConfiguration) {
        MetaDataCollector metaDataCollector = new NoOpMetadataCollector();

//        String config = governanceConfiguration.getConfig(Constants.GLOBAL_CONFIG_PATH);
//
//        if (StringUtils.isNotEmpty(config)) {
//            for (String s : config.split("\n")) {
//                if (s.startsWith(Constants.METADATA_ADDRESS)) {
//                    metadataUrl = formUrl(s.split("=")[1].trim(), group, username, password);
//                }
//            }
//        }

        if (metadataUrl == null) {
            if (StringUtils.isNotEmpty(metadataAddress)) {
                metadataUrl = formUrl(metadataAddress, group, username, password);
            }
        }
        if (metadataUrl != null) {
            metaDataCollector = ExtensionLoader.getExtensionLoader(MetaDataCollector.class).getExtension(metadataUrl.getProtocol());
            metaDataCollector.setUrl(metadataUrl);
            metaDataCollector.init();
        } else {
            logger.warn("you are using dubbo.registry.address, which is not recommend, please refer to: https://github.com/apache/incubator-dubbo-admin/wiki/Dubbo-Admin-configuration");
        }
        return metaDataCollector;
    }

    public static URL formUrl(String config, String group, String username, String password) {
        URL url = URL.valueOf(config);
        if (StringUtils.isNotEmpty(group)) {
            url = url.addParameter(Constants.GROUP_KEY, group);
        }
        if (StringUtils.isNotEmpty(username)) {
            url = url.setUsername(username);
        }
        if (StringUtils.isNotEmpty(password)) {
            url = url.setPassword(password);
        }
        return url;
    }

    private GovernanceConfiguration getGovernanceConfigurationFromURL(URL url) {
        GovernanceConfiguration dynamicConfiguration =
                ExtensionLoader.getExtensionLoader(GovernanceConfiguration.class).getExtension(url.getProtocol());
        dynamicConfiguration.setUrl(url);
        dynamicConfiguration.init();
        return dynamicConfiguration;
    }

    private GovernanceConfiguration getZookeeperConfigurationFromURL(URL url) {
        GovernanceConfiguration dynamicConfiguration = new ZookeeperConfiguration();
        dynamicConfiguration.setUrl(url);
        dynamicConfiguration.init();
        return dynamicConfiguration;
    }
}
