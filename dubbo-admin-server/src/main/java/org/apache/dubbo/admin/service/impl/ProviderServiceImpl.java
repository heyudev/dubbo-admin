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

import com.alibaba.fastjson.JSON;
import org.apache.dubbo.admin.common.exception.ParamValidationException;
import org.apache.dubbo.admin.common.util.Constants;
import org.apache.dubbo.admin.common.util.Pair;
import org.apache.dubbo.admin.common.util.ParseUtils;
import org.apache.dubbo.admin.common.util.SyncUtils;
import org.apache.dubbo.admin.common.util.Tool;
import org.apache.dubbo.admin.model.domain.Provider;
import org.apache.dubbo.admin.model.dto.ProviderDTO;
import org.apache.dubbo.admin.model.dto.ServiceDTO;
import org.apache.dubbo.admin.service.OverrideService;
import org.apache.dubbo.admin.service.ProviderService;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.identifier.MetadataIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
public class ProviderServiceImpl extends AbstractService implements ProviderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderServiceImpl.class);

    @Autowired
    OverrideService overrideService;

    @Override
    public void create(Provider provider) {
        URL url = provider.toUrl();
        registries.forEach(registry -> registry.register(url));
    }

    @Override
    public void enableProvider(String id, String registryAddress) {
        Provider oldProvider = findProvider(id, registryAddress);
        if (oldProvider == null) {
            throw new IllegalStateException("Provider was changed!");
        }
        LOGGER.info("oldProvider = {}", JSON.toJSONString(oldProvider));
        if (oldProvider.isDynamic()) {
            // Make sure we only have one override configured disable property.
            if (!oldProvider.isEnabled()) {
                org.apache.dubbo.admin.model.domain.Override override = new org.apache.dubbo.admin.model.domain.Override();
                override.setAddress(oldProvider.getAddress());
                override.setService(oldProvider.getService());
                override.setEnabled(true);
                override.setParams(Constants.DISABLED_KEY + "=false");
                LOGGER.info("isDynamic !oldProvider.isEnabled() override = {}", JSON.toJSONString(override));
                overrideService.saveOverride6(override, registryAddress);
//                return;
            }
            //
            List<org.apache.dubbo.admin.model.domain.Override> oList = overrideService.findByServiceAndAddress(oldProvider.getService(), oldProvider.getAddress(), registryAddress);
            LOGGER.info("isDynamic oldProvider.isEnabled() oList = {}", JSON.toJSONString(oList));
            for (org.apache.dubbo.admin.model.domain.Override o : oList) {
                Map<String, String> params = StringUtils.parseQueryString(o.getParams());
                if (params.containsKey(Constants.DISABLED_KEY)) {
                    if (params.get(Constants.DISABLED_KEY).equals("true")) {
                        LOGGER.info("isDynamic disabled=true, o = {} , params = {}", JSON.toJSONString(o), JSON.toJSONString(params));
                        overrideService.deleteOverride6(o.getHash(), registryAddress);
                    }
                }
            }
        } else {
            oldProvider.setEnabled(true);
            LOGGER.info("!isDynamic oldProvider = {}", JSON.toJSONString(oldProvider));
            updateProvider(oldProvider, registryAddress);
        }
    }

    @Override
    public void disableProvider(String id, String registryAddress) {
        if (id == null) {
            throw new IllegalStateException("no provider id");
        }
        Provider oldProvider = findProvider(id, registryAddress);
        LOGGER.info("oldProvider = {}", JSON.toJSONString(oldProvider));
        if (oldProvider == null) {
            throw new IllegalStateException("Provider was changed!");
        }

        if (oldProvider.isDynamic()) {
            // Make sure we only have one override configured disable property.
            if (oldProvider.isEnabled()) {
                org.apache.dubbo.admin.model.domain.Override override = new org.apache.dubbo.admin.model.domain.Override();
                override.setAddress(oldProvider.getAddress());
                override.setService(oldProvider.getService());
                override.setEnabled(true);
                override.setParams(Constants.DISABLED_KEY + "=true");
                LOGGER.info("isDynamic oldProvider.isEnabled() override = {}", JSON.toJSONString(override));
                overrideService.saveOverride6(override, registryAddress);
//                return;
            }
            List<org.apache.dubbo.admin.model.domain.Override> oList = overrideService.findByServiceAndAddress(oldProvider.getService(), oldProvider.getAddress(), registryAddress);
            LOGGER.info("isDynamic !oldProvider.isEnabled() oList = {}", JSON.toJSONString(oList));

            for (org.apache.dubbo.admin.model.domain.Override o : oList) {
                Map<String, String> params = StringUtils.parseQueryString(o.getParams());
                if (params.containsKey(Constants.DISABLED_KEY)) {
                    if (params.get(Constants.DISABLED_KEY).equals("false")) {
                        LOGGER.info("isDynamic disabled=false, o = {} , params = {}", JSON.toJSONString(o), JSON.toJSONString(params));
                        overrideService.deleteOverride6(o.getHash(), registryAddress);
                    }
                }
            }
        } else {
            oldProvider.setEnabled(false);
            LOGGER.info("!isDynamic oldProvider = {}", JSON.toJSONString(oldProvider));
            updateProvider(oldProvider, registryAddress);
        }
    }

    @Override
    public void adjustWeight(String id, Integer weight, String registryAddress) {
        if (id == null) {
            throw new IllegalStateException("no provider id");
        }
        Provider oldProvider = findProvider(id, registryAddress);
        if (oldProvider == null) {
            throw new IllegalStateException("Provider was changed!");
        }
        LOGGER.info("oldProvider = {}", JSON.toJSONString(oldProvider));
        Map<String, String> map = StringUtils.parseQueryString(oldProvider.getParameters());
        LOGGER.info("map = {}", JSON.toJSONString(map));
        String oldWeight = map.get(Constants.WEIGHT_KEY);
        LOGGER.info("oldWeight = {}", oldWeight);
        Integer oldWeightNumber = 100;
        if (oldWeight != null && !"".equals(oldWeight.trim())) {
            oldWeightNumber = Integer.parseInt(oldWeight);
        }
        if (oldProvider.isDynamic()) {
            // Make sure we only have one override configured disable property.
            List<org.apache.dubbo.admin.model.domain.Override> overrides = overrideService.findByServiceAndAddress(oldProvider.getService(), oldProvider.getAddress(), registryAddress);
            LOGGER.info("isDynamic overrides = {}", JSON.toJSONString(overrides));
            if (overrides == null || overrides.size() == 0) {
                if (weight.intValue() != Constants.DEFAULT_WEIGHT) {
                    org.apache.dubbo.admin.model.domain.Override override = new org.apache.dubbo.admin.model.domain.Override();
                    override.setAddress(oldProvider.getAddress());
                    override.setService(oldProvider.getService());
                    override.setEnabled(true);
                    override.setParams(Constants.WEIGHT_KEY + "=" + weight);
                    LOGGER.info("isDynamic overrides is null,override = {}", JSON.toJSONString(override));
                    overrideService.saveOverride6(override, registryAddress);
                }
            } else {
                for (org.apache.dubbo.admin.model.domain.Override override : overrides) {
                    Map<String, String> params = StringUtils.parseQueryString(override.getParams());
                    String overrideWeight = params.get(Constants.WEIGHT_KEY);
                    Integer overrideWeightNumber;
                    if (overrideWeight == null || overrideWeight.length() == 0) {
                        overrideWeightNumber = oldWeightNumber;
                    } else {
                        overrideWeightNumber = Integer.parseInt(overrideWeight);
                    }
                    if (weight.equals(overrideWeightNumber)) {
                        params.remove(Constants.WEIGHT_KEY);
                    } else {
                        params.put(Constants.WEIGHT_KEY, String.valueOf(weight));
                    }
                    if (params.size() > 0) {
                        override.setParams(StringUtils.toQueryString(params));
                        LOGGER.info("isDynamic updateOverride6 override = {}", JSON.toJSONString(override));
                        overrideService.updateOverride6(override, registryAddress);
                    } else {
                        LOGGER.info("isDynamic deleteOverride6 override = {}", JSON.toJSONString(override));
                        overrideService.deleteOverride6(override.getHash(), registryAddress);
                    }
                }
            }
        } else {
            if (weight == Constants.DEFAULT_WEIGHT) {
                map.remove(Constants.WEIGHT_KEY);
            } else {
                map.put(Constants.WEIGHT_KEY, String.valueOf(weight));
            }
            LOGGER.info("!isDynamic map = {}", JSON.toJSONString(map));
            oldProvider.setParameters(StringUtils.toQueryString(map));
            LOGGER.info("!isDynamic oldProvider = {}", JSON.toJSONString(oldProvider));
            updateProvider(oldProvider, registryAddress);
        }
    }

    @Override
    public String getProviderMetaData(MetadataIdentifier providerIdentifier) {
        return metaDataCollector.getProviderMetaData(providerIdentifier);
    }


    @Override
    public void deleteStaticProvider(String id, String registryAddress) {
        URL oldProvider = findProviderUrl(id, registryAddress);
        if (oldProvider == null) {
            throw new IllegalStateException("Provider was changed!");
        }
        registries.forEach(registry -> registry.unregister(oldProvider));
    }

    @Override
    public void updateProvider(Provider provider, String registryAddress) {
        String hash = provider.getHash();
        if (hash == null) {
            throw new IllegalStateException("no provider id");
        }

        URL oldProvider = findProviderUrl(hash, registryAddress);
        if (oldProvider == null) {
            throw new IllegalStateException("Provider was changed!");
        }
        URL newProvider = provider.toUrl();

        getRedistry(registryAddress).unregister(oldProvider);
        getRedistry(registryAddress).register(newProvider);
    }

    @Override
    public Provider findProvider(String id, String registryAddress) {
        return SyncUtils.url2Provider(findProviderUrlPair(id, registryAddress));
    }

    public Pair<String, URL> findProviderUrlPair(String id, String registryAddress) {
        return SyncUtils.filterFromCategory(getSingleRegistryCache(registryAddress), Constants.PROVIDERS_CATEGORY, id);
    }

    @Override
    public Set<String> findServices(String registryAddress) {
        Set<String> ret = new HashSet<>();
        ConcurrentMap<String, Map<String, URL>> providerUrls = getSingleRegistryCache(registryAddress).get(Constants.PROVIDERS_CATEGORY);
        if (providerUrls != null) {
            ret.addAll(providerUrls.keySet());
        }
        return ret;
    }

    @Override
    public Set<String> findMyServices(Set<String> applications, String registryAddress) {
        Set<String> ret = new HashSet<>();
        ConcurrentMap<String, Map<String, URL>> providerUrls = getSingleRegistryCache(registryAddress).get(Constants.PROVIDERS_CATEGORY);
        if (providerUrls != null) {
            for (Map.Entry<String, Map<String, URL>> entry : providerUrls.entrySet()) {
                for (Map.Entry<String, URL> entry1 : entry.getValue().entrySet()) {
                    if (applications.contains(entry1.getValue().getParameter(Constants.APPLICATION))) {
                        ret.add(entry.getKey());
                        break;
                    }
                }
            }
        }
        return ret;
    }

    @Override
    public Set<String> findServicesWithRegistry(String registryAddress) {
        Set<String> ret = new HashSet<>();
        ConcurrentMap<String, Map<String, URL>> providerUrls = getSingleRegistryCache(registryAddress).get(Constants.PROVIDERS_CATEGORY);
        if (providerUrls != null) {
            ret.addAll(providerUrls.keySet());
        }
        return ret;
    }

    @Override
    public List<String> findAddresses(String registryAddress) {
        List<String> ret = new ArrayList<String>();

        ConcurrentMap<String, Map<String, URL>> providerUrls = getSingleRegistryCache(registryAddress).get(Constants.PROVIDERS_CATEGORY);
        if (null == providerUrls) {
            return ret;
        }

        for (Map.Entry<String, Map<String, URL>> e1 : providerUrls.entrySet()) {
            Map<String, URL> value = e1.getValue();
            for (Map.Entry<String, URL> e2 : value.entrySet()) {
                URL u = e2.getValue();
                String app = u.getAddress();
                if (app != null) {
                    ret.add(app);
                }
            }
        }

        return ret;
    }

    @Override
    public List<String> findAddressesByApplication(String application, String registryAddress) {
        List<String> ret = new ArrayList<String>();
        ConcurrentMap<String, Map<String, URL>> providerUrls = getSingleRegistryCache(registryAddress).get(Constants.PROVIDERS_CATEGORY);
        for (Map.Entry<String, Map<String, URL>> e1 : providerUrls.entrySet()) {
            Map<String, URL> value = e1.getValue();
            for (Map.Entry<String, URL> e2 : value.entrySet()) {
                URL u = e2.getValue();
                if (application.equals(u.getParameter(Constants.APPLICATION))) {
                    String addr = u.getAddress();
                    if (addr != null) {
                        ret.add(addr);
                    }
                }
            }
        }

        return ret;
    }

    @Override
    public List<String> findAddressesByService(String service, String registryAddress) {
        List<String> ret = new ArrayList<String>();
        ConcurrentMap<String, Map<String, URL>> providerUrls = getSingleRegistryCache(registryAddress).get(Constants.PROVIDERS_CATEGORY);
        if (null == providerUrls) {
            return ret;
        }

        for (Map.Entry<String, URL> e2 : providerUrls.get(service).entrySet()) {
            URL u = e2.getValue();
            String app = u.getAddress();
            if (app != null) {
                ret.add(app);
            }
        }

        return ret;
    }

    @Override
    public List<String> findApplicationsByServiceName(String service, String registryAddress) {
        List<String> ret = new ArrayList<String>();
        ConcurrentMap<String, Map<String, URL>> providerUrls = getSingleRegistryCache(registryAddress).get(Constants.PROVIDERS_CATEGORY);
        if (null == providerUrls) {
            return ret;
        }

        Map<String, URL> value = providerUrls.get(service);
        if (value == null) {
            return ret;
        }
        for (Map.Entry<String, URL> e2 : value.entrySet()) {
            URL u = e2.getValue();
            String app = u.getParameter(Constants.APPLICATION);
            if (app != null) {
                ret.add(app);
            }
        }

        return ret;
    }

    @Override
    public List<Provider> findByService(String serviceName, String registryAddress) {
        return SyncUtils.url2ProviderList(findProviderUrlByService(serviceName, registryAddress));
    }

    @Override
    public List<Provider> findByService2(String serviceName, String registryAddress) {
        return SyncUtils.url2ProviderList2(findProviderUrlByService(serviceName, registryAddress), overrideService, registryAddress);
    }

    @Override
    public List<Provider> findByServiceAndRegistry(String serviceName, String registry) {
        return SyncUtils.url2ProviderList(findProviderUrlByServiceAndRegistry(serviceName, registry));
    }

    @Override
    public List<Provider> findByServiceWithoutRegistry(String serviceName, String registryAddress) {
        return SyncUtils.url2ProviderList(findProviderUrlByServiceWithoutRegistry(serviceName, registryAddress));
    }

    @Override
    public List<Provider> findByAppandService(String app, String serviceName, String registryAddress) {
        return SyncUtils.url2ProviderList(findProviderUrlByAppandService(app, serviceName, registryAddress));
    }

    @Override
    public String test(String service, String registryAddress) {
        Map<String, URL> result = findProviderUrlByService(service, registryAddress);
        return JSON.toJSONString(result);
    }

    private Map<String, URL> findProviderUrlByService(String service, String registryAddress) {
        Map<String, String> filter = new HashMap<String, String>();
        filter.put(Constants.CATEGORY_KEY, Constants.PROVIDERS_CATEGORY);
        filter.put(SyncUtils.SERVICE_FILTER_KEY, service);

        return SyncUtils.filterFromCategory(getSingleRegistryCache(registryAddress), filter);
    }

    private Map<String, URL> findProviderUrlByServiceAndRegistry(String service, String registryAddress) {
        Map<String, String> filter = new HashMap<>(3);
        filter.put(Constants.CATEGORY_KEY, Constants.PROVIDERS_CATEGORY);
        filter.put(SyncUtils.SERVICE_FILTER_KEY, service);

        return SyncUtils.filterFromCategoryAndRegistry(getSingleRegistryCache(registryAddress), filter);
    }

    private Map<String, URL> findProviderUrlByServiceWithoutRegistry(String service, String registryAddress) {
        Map<String, String> filter = new HashMap<String, String>();
        filter.put(Constants.CATEGORY_KEY, Constants.PROVIDERS_CATEGORY);
        filter.put(SyncUtils.SERVICE_FILTER_KEY, service);

        return SyncUtils.filterFromCategoryWithoutRegistry(getSingleRegistryCache(registryAddress), filter);
    }

    @Override
    public List<Provider> findAll(String registryAddress) {
        return SyncUtils.url2ProviderList(findAllProviderUrl(registryAddress));
    }

    private Map<String, URL> findAllProviderUrl(String registryAddress) {
        Map<String, String> filter = new HashMap<String, String>();
        filter.put(Constants.CATEGORY_KEY, Constants.PROVIDERS_CATEGORY);
        return SyncUtils.filterFromCategory(getSingleRegistryCache(registryAddress), filter);
    }

    @Override
    public List<Provider> findByAddress(String providerAddress, String registryAddress) {
        return SyncUtils.url2ProviderList(findProviderUrlByAddress(providerAddress, registryAddress));
    }

    public Map<String, URL> findProviderUrlByAddress(String address, String registryAddress) {
        Map<String, String> filter = new HashMap<String, String>();
        filter.put(Constants.CATEGORY_KEY, Constants.PROVIDERS_CATEGORY);
        filter.put(SyncUtils.ADDRESS_FILTER_KEY, address);

        return SyncUtils.filterFromCategory(getSingleRegistryCache(registryAddress), filter);
    }

    @Override
    public List<String> findServicesByAddress(String address, String registryAddress) {
        List<String> ret = new ArrayList<String>();

        ConcurrentMap<String, Map<String, URL>> providerUrls = getSingleRegistryCache(registryAddress).get(Constants.PROVIDERS_CATEGORY);
        if (providerUrls == null || address == null || address.length() == 0) {
            return ret;
        }

        for (Map.Entry<String, Map<String, URL>> e1 : providerUrls.entrySet()) {
            Map<String, URL> value = e1.getValue();
            for (Map.Entry<String, URL> e2 : value.entrySet()) {
                URL u = e2.getValue();
                if (address.equals(u.getAddress())) {
                    ret.add(e1.getKey());
                    //TODO Support multiple registry centers 暂未使用
                    break;
                }
            }
        }

        return ret;
    }

    @Override
    public Set<String> findApplications(String registryAddress) {
        Set<String> ret = new HashSet<>();
        ConcurrentMap<String, Map<String, URL>> providerUrls = getSingleRegistryCache(registryAddress).get(Constants.PROVIDERS_CATEGORY);
        if (providerUrls == null) {
            return ret;
        }

        for (Map.Entry<String, Map<String, URL>> e1 : providerUrls.entrySet()) {
            Map<String, URL> value = e1.getValue();
            for (Map.Entry<String, URL> e2 : value.entrySet()) {
                URL u = e2.getValue();
                String app = u.getParameter(Constants.APPLICATION);
                if (app != null) {
                    ret.add(app);
                }
            }
        }

        return ret;
    }

    @Override
    public List<Provider> findByApplication(String application, String registryAddress) {
        return SyncUtils.url2ProviderList(findProviderUrlByApplication(application, registryAddress));
    }

    @Override
    public String findVersionInApplication(String application, String registryAddress) {
        List<String> services = findServicesByApplication(application, registryAddress);
        if (services == null || services.size() == 0) {
            throw new ParamValidationException("there is no service for application: " + application + ",registryAddress: " + registryAddress);
        }
        return findServiceVersion(services.get(0), application, registryAddress);
    }

    @Override
    public String findServiceVersion(String serviceName, String application, String registryAddress) {
        String version = "2.6";
        Map<String, URL> result = findProviderUrlByAppandService(application, serviceName, registryAddress);
        if (result != null && result.size() > 0) {
            URL url = result.values().stream().findFirst().get();
            if (url.getParameter(Constants.SPECIFICATION_VERSION_KEY) != null) {
                version = url.getParameter(Constants.SPECIFICATION_VERSION_KEY);
            }
        }
        return version;
    }

    private Map<String, URL> findProviderUrlByAppandService(String app, String service, String registryAddress) {
        Map<String, String> filter = new HashMap<>();
        filter.put(Constants.CATEGORY_KEY, Constants.PROVIDERS_CATEGORY);
        filter.put(Constants.APPLICATION, app);
        filter.put(SyncUtils.SERVICE_FILTER_KEY, service);
        return SyncUtils.filterFromCategory(getSingleRegistryCache(registryAddress), filter);
    }


    private Map<String, URL> findProviderUrlByApplication(String application, String registryAddress) {
        Map<String, String> filter = new HashMap<>();
        filter.put(Constants.CATEGORY_KEY, Constants.PROVIDERS_CATEGORY);
        filter.put(Constants.APPLICATION, application);
        return SyncUtils.filterFromCategory(getSingleRegistryCache(registryAddress), filter);
    }

    @Override
    public List<String> findServicesByApplication(String application, String registryAddress) {
        List<String> ret = new ArrayList<String>();

        ConcurrentMap<String, Map<String, URL>> providerUrls = getSingleRegistryCache(registryAddress).get(Constants.PROVIDERS_CATEGORY);
        if (providerUrls == null || application == null || application.length() == 0) {
            return ret;
        }

        for (Map.Entry<String, Map<String, URL>> e1 : providerUrls.entrySet()) {
            Map<String, URL> value = e1.getValue();
            for (Map.Entry<String, URL> e2 : value.entrySet()) {
                URL u = e2.getValue();
                if (application.equals(u.getParameter(Constants.APPLICATION))) {
                    ret.add(e1.getKey());
                    //TODO Support multiple registry centers 无影响
                    break;
                }
            }
        }

        return ret;
    }

    @Override
    public List<String> findMethodsByService(String service, String registryAddress) {
        List<String> ret = new ArrayList<String>();

        ConcurrentMap<String, Map<String, URL>> providerUrls = getSingleRegistryCache(registryAddress).get(Constants.PROVIDERS_CATEGORY);
        if (providerUrls == null || service == null || service.length() == 0) {
            return ret;
        }
        //TODO Support multiple registry centers 暂未使用
        Map<String, URL> providers = providerUrls.get(service);
        if (null == providers || providers.isEmpty()) {
            return ret;
        }

        Entry<String, URL> p = providers.entrySet().iterator().next();
        String value = p.getValue().getParameter("methods");
        if (value == null || value.length() == 0) {
            return ret;
        }
        String[] methods = value.split(ParseUtils.METHOD_SPLIT);
        if (methods == null || methods.length == 0) {
            return ret;
        }

        for (String m : methods) {
            ret.add(m);
        }
        return ret;
    }

    private URL findProviderUrl(String id, String registryAddress) {
        return findProvider(id, registryAddress).toUrl();
    }

    @Override
    public Provider findByServiceAndAddress(String service, String address, String registryAddress) {
        return SyncUtils.url2Provider(findProviderUrl(service, address, registryAddress));
    }

    @Override
    public ProviderDTO findByServiceAndId(String service, String id, String registryAddress) {
        Pair<String, URL> pair = findProviderUrlByServiceAndId(service, id, registryAddress);
        List<org.apache.dubbo.admin.model.domain.Override> overrides = overrideService.findByServiceAndAddress(pair.getValue().getServiceKey(), pair.getValue().getAddress(), registryAddress);
        return SyncUtils.url2ProviderDTO(pair, overrides);
    }

    private Pair<String, URL> findProviderUrlByServiceAndId(String service, String id, String registryAddress) {
        Map<String, String> filter = new HashMap<String, String>();
        filter.put(Constants.CATEGORY_KEY, Constants.PROVIDERS_CATEGORY);
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

    private Pair<String, URL> findProviderUrl(String service, String address, String registryAddress) {
        Map<String, String> filter = new HashMap<String, String>();
        filter.put(Constants.CATEGORY_KEY, Constants.PROVIDERS_CATEGORY);
        filter.put(SyncUtils.ADDRESS_FILTER_KEY, address);

        Map<String, URL> ret = SyncUtils.filterFromCategory(getSingleRegistryCache(registryAddress), filter);
        if (ret.isEmpty()) {
            return null;
        } else {
            String key = ret.entrySet().iterator().next().getKey();
            return new Pair<String, URL>(key, ret.get(key));
        }
    }

    @Override
    public Set<ServiceDTO> getServiceDTOS(String pattern, String filter, String env, String registryAddress) {
        List<Provider> providers = new ArrayList<>();
        if (filter == null || "".equals(filter.trim())) {
            filter = "*";
        }
        if (!filter.contains(Constants.ANY_VALUE) && !filter.contains(Constants.INTERROGATION_POINT)) {
            // filter with specific string
            if (Constants.IP.equals(pattern)) {
                providers = findByAddress(filter, registryAddress);
            } else if (Constants.SERVICE.equals(pattern)) {
                providers = findByService(filter, registryAddress);
                //fix
//                providers = findByServiceWithoutRegistry(filter, registryAddress);
            } else if (Constants.APPLICATION.equals(pattern)) {
                providers = findByApplication(filter, registryAddress);
            }
        } else {
            // filter with fuzzy search
            Set<String> candidates = Collections.emptySet();
            if (Constants.SERVICE.equals(pattern)) {
                candidates = findServicesWithRegistry(registryAddress);
            } else if (Constants.APPLICATION.equals(pattern)) {
                candidates = findApplications(registryAddress);
            }
            // replace dot symbol and asterisk symbol to java-based regex pattern
            filter = filter.toLowerCase().replace(Constants.PUNCTUATION_POINT, Constants.PUNCTUATION_SEPARATOR_POINT);
            // filter start with [* 、? 、+] will triggering PatternSyntaxException
            if (filter.startsWith(Constants.ANY_VALUE)
                    || filter.startsWith(Constants.INTERROGATION_POINT) || filter.startsWith(Constants.PLUS_SIGNS)) {
                filter = Constants.PUNCTUATION_POINT + filter;
            }
            // search with no case insensitive
            Pattern regex = Pattern.compile(filter, Pattern.CASE_INSENSITIVE);
            for (String candidate : candidates) {
                Matcher matcher = regex.matcher(candidate);
                if (matcher.matches() || matcher.lookingAt()) {
                    if (Constants.SERVICE.equals(pattern)) {
                        providers.addAll(findByService(candidate, registryAddress));
                    } else {
                        providers.addAll(findByApplication(candidate, registryAddress));
                    }
                }
            }
        }

        Set<ServiceDTO> result = convertProviders2DTO(providers);
        return result;
    }

    /**
     * Convert provider list to ServiceDTO list
     *
     * @param providers list of providers
     * @return ServiceDTO list of front page
     */
    public Set<ServiceDTO> convertProviders2DTO(List<Provider> providers) {
        Set<ServiceDTO> result = new TreeSet<>();
        for (Provider provider : providers) {
            String app = provider.getApplication();
            String service = provider.getService();
            String group = Tool.getGroup(service);
            String version = Tool.getVersion(service);
            String interfaze = Tool.getInterface(service);
            String registry = provider.getRegistry();
            ServiceDTO s = new ServiceDTO();
            s.setAppName(app);
            s.setService(interfaze);
            s.setGroup(group);
            s.setVersion(version);
            s.setRegistry(registry);
            result.add(s);
        }
        return result;
    }

    @Override
    public Set<ServiceDTO> getServiceDTOS2(String pattern, String filter, String env, String registryAddress) {
        ConcurrentMap<String, ConcurrentMap<String, Map<String, URL>>> urls = getSingleRegistryCache(registryAddress);
        if (urls == null) {
            return new HashSet<>();
        }
        ConcurrentMap<String, Map<String, URL>> providerUrls = urls.get(Constants.PROVIDERS_CATEGORY);
        Map<String, URL> map = new HashMap<>(16);

        if (filter == null || "".equals(filter.trim())) {
            for (Map.Entry<String, Map<String, URL>> entry : providerUrls.entrySet()) {
                for (Map.Entry<String, URL> entry1 : entry.getValue().entrySet()) {
                    map.put(entry1.getKey(), entry1.getValue());
                }
            }
        } else {
            if (Constants.SERVICE.equals(pattern)) {
                for (Map.Entry<String, Map<String, URL>> entry : providerUrls.entrySet()) {
                    if (entry.getKey().contains(filter.trim())) {
                        for (Map.Entry<String, URL> entry1 : entry.getValue().entrySet()) {
                            map.put(entry1.getKey(), entry1.getValue());
                        }
                    }
                }
            } else if (Constants.APPLICATION.equals(pattern)) {
                for (Map.Entry<String, Map<String, URL>> entry : providerUrls.entrySet()) {
                    for (Map.Entry<String, URL> entry1 : entry.getValue().entrySet()) {
                        if (entry1.getValue().getParameter(Constants.APPLICATION).contains(filter.trim())) {
                            map.put(entry1.getKey(), entry1.getValue());
                        }
                    }
                }
            } else if (Constants.IP.equals(pattern)) {
                for (Map.Entry<String, Map<String, URL>> entry : providerUrls.entrySet()) {
                    for (Map.Entry<String, URL> entry1 : entry.getValue().entrySet()) {
                        if (entry1.getValue().getIp().contains(filter.trim())) {
                            map.put(entry1.getKey(), entry1.getValue());
                        }
                    }
                }
            }
        }

        List<Provider> providers = SyncUtils.url2ProviderList(map);
        Set<ServiceDTO> result = convertProviders2DTO(providers);
        return result;
    }

}
