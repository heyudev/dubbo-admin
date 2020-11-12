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

import org.apache.dubbo.admin.common.util.Constants;
import org.apache.dubbo.admin.common.util.Pair;
import org.apache.dubbo.admin.common.util.SyncUtils;
import org.apache.dubbo.admin.model.domain.Consumer;
import org.apache.dubbo.admin.model.dto.ConsumerDTO;
import org.apache.dubbo.admin.service.ConsumerService;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.identifier.MetadataIdentifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ConsumerServiceImpl extends AbstractService implements ConsumerService {

    @Override
    public List<Consumer> findByService(String service, String registryAddress) {
        return SyncUtils.url2ConsumerList(findConsumerUrlByService(service, registryAddress));
    }

    @Override
    public ConsumerDTO findByServiceAndId(String service, String id, String registryAddress) {
        return SyncUtils.url2ConsumerDTO(findConsumerUrlByServiceAndId(service, id, registryAddress));
    }

    @Override
    public List<Consumer> findAll(String registryAddress) {
        return SyncUtils.url2ConsumerList(findAllConsumerUrl(registryAddress));
    }

    @Override
    public String getConsumerMetadata(MetadataIdentifier consumerIdentifier) {
        return metaDataCollector.getConsumerMetaData(consumerIdentifier);
    }

    private Map<String, URL> findAllConsumerUrl(String registryAddress) {
        Map<String, String> filter = new HashMap<String, String>();
        filter.put(Constants.CATEGORY_KEY, Constants.CONSUMERS_CATEGORY);
        return SyncUtils.filterFromCategory(getSingleRegistryCache(registryAddress), filter);
    }

    @Override
    public List<Consumer> findByAddress(String consumerAddress, String registryAddress) {
        return SyncUtils.url2ConsumerList(findConsumerUrlByAddress(consumerAddress, registryAddress));
    }

    @Override
    public List<Consumer> findByApplication(String application, String registry) {
        return SyncUtils.url2ConsumerList(findConsumerUrlByApplication(application, registry));
    }

    private Map<String, URL> findConsumerUrlByAddress(String address, String registryAddress) {
        Map<String, String> filter = new HashMap<String, String>();
        filter.put(Constants.CATEGORY_KEY, Constants.CONSUMERS_CATEGORY);
        filter.put(SyncUtils.ADDRESS_FILTER_KEY, address);

        return SyncUtils.filterFromCategory(getSingleRegistryCache(registryAddress), filter);
    }

    public Map<String, URL> findConsumerUrlByService(String service, String registryAddress) {
        Map<String, String> filter = new HashMap<String, String>();
        filter.put(Constants.CATEGORY_KEY, Constants.CONSUMERS_CATEGORY);
        filter.put(SyncUtils.SERVICE_FILTER_KEY, service);

        return SyncUtils.filterFromCategory(getSingleRegistryCache(registryAddress), filter);
    }

    //TODO
    public Map<String, URL> findConsumerUrlByApplication(String application, String registryAddress) {
        Map<String, String> filter = new HashMap<>(3);
        filter.put(Constants.CATEGORY_KEY, Constants.CONSUMERS_CATEGORY);
        filter.put(SyncUtils.APPLICATION_FILTER_KEY, application);

        return SyncUtils.filterFromApplication(getSingleRegistryCache(registryAddress), filter);
    }

    private Pair<String, URL> findConsumerUrlByServiceAndId(String service, String id, String registryAddress) {
        Map<String, String> filter = new HashMap<String, String>();
        filter.put(Constants.CATEGORY_KEY, Constants.CONSUMERS_CATEGORY);
        filter.put(SyncUtils.SERVICE_FILTER_KEY, service);

        Map<String, URL> ret = SyncUtils.filterFromCategory(getSingleRegistryCache(registryAddress), filter);
        if (ret.isEmpty()) {
            return null;
        } else {
            for (Map.Entry<String, URL> entry : ret.entrySet()) {
                if (Objects.equals(entry.getKey(), id)) {
                    return new Pair<>(id, ret.get(id));
                }
            }
            return null;
        }
    }
}
