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
package org.apache.dubbo.admin.service.impl;

import org.apache.dubbo.admin.registry.config.GovernanceConfiguration;
import org.apache.dubbo.admin.registry.metadata.MetaDataCollector;
import org.apache.dubbo.admin.service.RegistryServerSync;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.registry.Registry;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public class AbstractService {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractService.class);

    @Autowired
    protected List<Registry> registries;

    @Resource
    protected List<GovernanceConfiguration> dynamicConfigurations;

    @Deprecated
    @Resource
    protected GovernanceConfiguration governanceConfiguration;

    @Autowired
    protected MetaDataCollector metaDataCollector;

    @Autowired
    private RegistryServerSync sync;


    public ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<String, Map<String, URL>>>> getRegistryCache() {
        return sync.getRegistryCache();
    }

    public ConcurrentMap<String, ConcurrentMap<String, Map<String, URL>>> getSingleRegistryCache(String registryAddress) {
        return getRegistryCache().get(registryAddress);
    }

    public Registry getRedistry(String registryAddress) {
        for (Registry registry : registries) {
            if (registry.getUrl().getAddress().equals(registryAddress)) {
                return registry;
            }
        }
        return null;
    }

    public GovernanceConfiguration getDynamicConfiguration(String registryAddress) {
        for (GovernanceConfiguration governanceConfiguration : dynamicConfigurations) {
            if (governanceConfiguration.getUrl().getAddress().equals(registryAddress)) {
                return governanceConfiguration;
            }
        }
        return null;
    }

}
