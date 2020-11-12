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
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.admin.common.exception.SystemException;
import org.apache.dubbo.admin.common.util.*;
import org.apache.dubbo.admin.model.adapter.DynamicConfigDTO2OverrideDTOAdapter;
import org.apache.dubbo.admin.model.adapter.LoadBalance2OverrideAdapter;
import org.apache.dubbo.admin.model.adapter.WeightToOverrideAdapter;
import org.apache.dubbo.admin.model.domain.LoadBalance;
import org.apache.dubbo.admin.model.domain.Override;
import org.apache.dubbo.admin.model.domain.Weight;
import org.apache.dubbo.admin.model.dto.BalancingDTO;
import org.apache.dubbo.admin.model.dto.DynamicConfigDTO;
import org.apache.dubbo.admin.model.dto.WeightDTO;
import org.apache.dubbo.admin.model.store.OverrideConfig;
import org.apache.dubbo.admin.model.store.OverrideDTO;
import org.apache.dubbo.admin.registry.config.GovernanceConfiguration;
import org.apache.dubbo.admin.service.OverrideService;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
public class OverrideServiceImpl extends AbstractService implements OverrideService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OverrideServiceImpl.class);

    private String prefix = Constants.CONFIG_KEY;

    @java.lang.Override
    public void saveOverride(DynamicConfigDTO override) {
        String id = ConvertUtil.getIdFromDTO(override);
        String path = getPath(id);
        GovernanceConfiguration dynamicConfiguration = getDynamicConfiguration(override.getRegistryAddress());
        String exitConfig = dynamicConfiguration.getConfig(path);
        List<OverrideConfig> configs = new ArrayList<>();
        OverrideDTO existOverride = new DynamicConfigDTO2OverrideDTOAdapter(override);
        if (exitConfig != null) {
            existOverride = YamlParser.loadObject(exitConfig, OverrideDTO.class);
            if (existOverride.getConfigs() != null) {
                for (OverrideConfig overrideConfig : existOverride.getConfigs()) {
                    if (Constants.CONFIGS.contains(overrideConfig.getType())) {
                        configs.add(overrideConfig);
                    }
                }
            }
        }
        configs.addAll(override.getConfigs());
        existOverride.setEnabled(override.isEnabled());
        existOverride.setConfigs(configs);
        dynamicConfiguration.setConfig(path, YamlParser.dumpObject(existOverride));


        //for2.6
        if (StringUtils.isNotEmpty(override.getService())) {
            List<Override> result = convertDTOtoOldOverride(override);
            Registry registry = getRedistry(override.getRegistryAddress());
            if (registry == null) {
                logger.error("get registry failure,registryAddress = " + override.getRegistryAddress());
                return;
            }
            for (Override o : result) {
                registry.register(o.toUrl().addParameter(Constants.COMPATIBLE_CONFIG, true));
            }
        }
    }


    @java.lang.Override
    public void updateOverride(DynamicConfigDTO update) {
        String id = ConvertUtil.getIdFromDTO(update);
        String path = getPath(id);
        GovernanceConfiguration dynamicConfiguration = getDynamicConfiguration(update.getRegistryAddress());
        //dynamicConfiguration NPE
        String exitConfig = dynamicConfiguration.getConfig(path);
        if (exitConfig == null) {
            //throw exception
        }
        OverrideDTO overrideDTO = YamlParser.loadObject(exitConfig, OverrideDTO.class);
        DynamicConfigDTO old = OverrideUtils.createFromOverride(overrideDTO);
        List<OverrideConfig> configs = new ArrayList<>();
        if (overrideDTO.getConfigs() != null) {
            List<OverrideConfig> overrideConfigs = overrideDTO.getConfigs();
            for (OverrideConfig config : overrideConfigs) {
                if (Constants.CONFIGS.contains(config.getType())) {
                    configs.add(config);
                }
            }
        }
        configs.addAll(update.getConfigs());
        overrideDTO.setConfigs(configs);
        overrideDTO.setEnabled(update.isEnabled());
        dynamicConfiguration.setConfig(path, YamlParser.dumpObject(overrideDTO));
        //for 2.6
        if (StringUtils.isNotEmpty(update.getService())) {
            List<Override> oldOverrides = convertDTOtoOldOverride(old);
            List<Override> updatedOverrides = convertDTOtoOldOverride(update);
            Registry registry = getRedistry(update.getRegistryAddress());
            if (registry == null) {
                logger.error("get registry failure,registryAddress = " + update.getRegistryAddress());
                return;
            }
            for (Override o : oldOverrides) {
                registry.unregister(o.toUrlforUnRegister().addParameter(Constants.COMPATIBLE_CONFIG, true));
            }
            for (Override o : updatedOverrides) {
                registry.register(o.toUrl().addParameter(Constants.COMPATIBLE_CONFIG, true));
            }
        }
    }

    @java.lang.Override
    public void deleteOverride(String id, String registryAddress) {
        if (StringUtils.isEmpty(id)) {
            // throw exception
        }
        String path = getPath(id);
        GovernanceConfiguration dynamicConfiguration = getDynamicConfiguration(registryAddress);
        //dynamicConfiguration NPE
        String config = dynamicConfiguration.getConfig(path);
        if (config == null) {
            //throw exception
        }
        OverrideDTO overrideDTO = YamlParser.loadObject(config, OverrideDTO.class);
        DynamicConfigDTO old = OverrideUtils.createFromOverride(overrideDTO);
        List<OverrideConfig> newConfigs = new ArrayList<>();
        if (overrideDTO.getConfigs() != null && overrideDTO.getConfigs().size() > 0) {
            for (OverrideConfig overrideConfig : overrideDTO.getConfigs()) {
                if (Constants.CONFIGS.contains(overrideConfig.getType())) {
                    newConfigs.add(overrideConfig);
                }
            }
            if (newConfigs.size() == 0) {
                dynamicConfiguration.deleteConfig(path);
            } else {
                overrideDTO.setConfigs(newConfigs);
                dynamicConfiguration.setConfig(path, YamlParser.dumpObject(overrideDTO));
                throw new SystemException("newConfigs is not null,delete failure");
            }
        } else {
            dynamicConfiguration.deleteConfig(path);
        }
        //for 2.6
        if (overrideDTO.getScope().equals(Constants.SERVICE)) {
            List<Override> overrides = convertDTOtoOldOverride(old);
            Registry registry = getRedistry(registryAddress);
            if (registry == null) {
                logger.error("get registry failure,registryAddress = " + registryAddress);
                throw new SystemException("get registry failure,registryAddress = " + registryAddress);
            }
            for (Override o : overrides) {
                registry.unregister(o.toUrlforUnRegister().addParameter(Constants.COMPATIBLE_CONFIG, true));
            }
        }

    }

    @java.lang.Override
    public void enableOverride(String id, String registryAddress) {
        if (StringUtils.isEmpty(id)) {
            //throw exception
        }
        String path = getPath(id);
        GovernanceConfiguration dynamicConfiguration = getDynamicConfiguration(registryAddress);
        //dynamicConfiguration NPE
        String config = dynamicConfiguration.getConfig(path);
        if (config == null) {
            //throw exception
        }
        OverrideDTO override = YamlParser.loadObject(config, OverrideDTO.class);
        DynamicConfigDTO old = OverrideUtils.createFromOverride(override);
        override.setEnabled(true);
        dynamicConfiguration.setConfig(path, YamlParser.dumpObject(override));

        //2.6
        if (override.getScope().equals(Constants.SERVICE)) {
            List<Override> overrides = convertDTOtoOldOverride(old);
            Registry registry = getRedistry(registryAddress);
            if (registry == null) {
                logger.error("get registry failure,registryAddress = " + registryAddress);
                return;
            }
            for (Override o : overrides) {
                o.setEnabled(false);
                registry.unregister(o.toUrlforUnRegister().addParameter(Constants.COMPATIBLE_CONFIG, true));
                o.setEnabled(true);
                registry.register(o.toUrl().addParameter(Constants.COMPATIBLE_CONFIG, true));
            }

        }
    }

    @java.lang.Override
    public void disableOverride(String id, String registryAddress) {
        if (StringUtils.isEmpty(id)) {
            //throw exception
        }
        String path = getPath(id);
        GovernanceConfiguration dynamicConfiguration = getDynamicConfiguration(registryAddress);
        //dynamicConfiguration NPE
        if (dynamicConfiguration.getConfig(path) == null) {
            //throw exception
        }
        String config = dynamicConfiguration.getConfig(path);
        OverrideDTO override = YamlParser.loadObject(config, OverrideDTO.class);
        DynamicConfigDTO old = OverrideUtils.createFromOverride(override);
        override.setEnabled(false);
        dynamicConfiguration.setConfig(path, YamlParser.dumpObject(override));

        //for 2.6
        if (override.getScope().equals(Constants.SERVICE)) {
            List<Override> overrides = convertDTOtoOldOverride(old);
            Registry registry = getRedistry(registryAddress);
            if (registry == null) {
                logger.error("get registry failure,registryAddress = " + registryAddress);
                return;
            }
            for (Override o : overrides) {
                o.setEnabled(true);
                registry.unregister(o.toUrlforUnRegister().addParameter(Constants.COMPATIBLE_CONFIG, true));
                o.setEnabled(false);
                registry.register(o.toUrl().addParameter(Constants.COMPATIBLE_CONFIG, true));
            }
        }
    }

    @java.lang.Override
    public DynamicConfigDTO findOverride(String id, String registryAddress) {
        if (StringUtils.isEmpty(id)) {
            //throw exception
        }
        String path = getPath(id);
        GovernanceConfiguration dynamicConfiguration = getDynamicConfiguration(registryAddress);
        //dynamicConfiguration NPE
        String config = dynamicConfiguration.getConfig(path);
        if (config != null) {
            OverrideDTO overrideDTO = YamlParser.loadObject(config, OverrideDTO.class);
            return OverrideUtils.createFromOverride(overrideDTO);
        }
        return null;
    }

    @java.lang.Override
    public void saveWeight(WeightDTO weightDTO) {
        String id = ConvertUtil.getIdFromDTO(weightDTO);
        String scope = ConvertUtil.getScopeFromDTO(weightDTO);
        String path = getPath(id);
        GovernanceConfiguration dynamicConfiguration = getDynamicConfiguration(weightDTO.getRegistryAddress());
        //dynamicConfiguration NPE
        String config = dynamicConfiguration.getConfig(path);
        OverrideConfig overrideConfig = OverrideUtils.weightDTOtoConfig(weightDTO);
        OverrideDTO overrideDTO = insertConfig(config, overrideConfig, id, scope, Constants.WEIGHT);
        dynamicConfiguration.setConfig(path, YamlParser.dumpObject(overrideDTO));

        //for 2.6
        if (scope.equals(Constants.SERVICE)) {
            registerWeight(weightDTO);
        }

    }

    @java.lang.Override
    public void updateWeight(WeightDTO weightDTO) {
        String id = ConvertUtil.getIdFromDTO(weightDTO);
        String scope = ConvertUtil.getScopeFromDTO(weightDTO);
        String path = getPath(id);
        GovernanceConfiguration dynamicConfiguration = getDynamicConfiguration(weightDTO.getRegistryAddress());
        //dynamicConfiguration NPE
        String config = dynamicConfiguration.getConfig(path);
        WeightDTO oldWeight = null;
        if (config != null) {
            OverrideDTO overrideDTO = YamlParser.loadObject(config, OverrideDTO.class);
            List<OverrideConfig> configs = overrideDTO.getConfigs();
            if (configs != null && configs.size() > 0) {
                for (OverrideConfig overrideConfig : configs) {
                    if (Constants.WEIGHT.equals(overrideConfig.getType())) {
                        if (overrideDTO.getScope().equals(Constants.SERVICE)) {
                            oldWeight = OverrideUtils.configtoWeightDTO(overrideConfig, scope, id);
                        }
                        int index = configs.indexOf(overrideConfig);
                        OverrideConfig newConfig = OverrideUtils.weightDTOtoConfig(weightDTO);
                        configs.set(index, newConfig);
                        break;
                    }
                }
                dynamicConfiguration.setConfig(path, YamlParser.dumpObject(overrideDTO));
            } else {
                //throw exception
            }
        } else {
            //throw exception
        }

        //for 2.6
        if (oldWeight != null) {
            unregisterWeight(oldWeight, weightDTO.getRegistryAddress());
            registerWeight(weightDTO);
        }

    }

    @java.lang.Override
    public void deleteWeight(String id, String registryAddress) {
        String path = getPath(id);
        GovernanceConfiguration dynamicConfiguration = getDynamicConfiguration(registryAddress);
        //dynamicConfiguration NPE
        String config = dynamicConfiguration.getConfig(path);
        OverrideConfig oldConfig = null;
        if (config != null) {
            OverrideDTO overrideDTO = YamlParser.loadObject(config, OverrideDTO.class);
            List<OverrideConfig> configs = overrideDTO.getConfigs();
            if (configs != null) {
                for (OverrideConfig overrideConfig : configs) {
                    if (Constants.WEIGHT.equals(overrideConfig.getType())) {
                        if (overrideDTO.getScope().equals(Constants.SERVICE)) {
                            oldConfig = overrideConfig;
                        }
                        configs.remove(overrideConfig);
                        break;
                    }
                }
                if (configs.size() == 0) {
                    dynamicConfiguration.deleteConfig(path);
                } else {
                    dynamicConfiguration.setConfig(path, YamlParser.dumpObject(overrideDTO));
                    throw new SystemException("configs is not null,delete failure");
                }

            }

            //for 2.6
            if (oldConfig != null) {
                String key = overrideDTO.getKey();
                WeightDTO weightDTO = OverrideUtils.configtoWeightDTO(oldConfig, overrideDTO.getScope(), key);
                unregisterWeight(weightDTO, registryAddress);
            }
        }
    }

    @java.lang.Override
    public WeightDTO findWeight(String id, String registryAddress) {
        String path = getPath(id);
        GovernanceConfiguration dynamicConfiguration = getDynamicConfiguration(registryAddress);
        //dynamicConfiguration NPE
        String config = dynamicConfiguration.getConfig(path);
        if (config != null) {
            OverrideDTO overrideDTO = YamlParser.loadObject(config, OverrideDTO.class);
            List<OverrideConfig> configs = overrideDTO.getConfigs();
            if (configs != null) {
                for (OverrideConfig overrideConfig : configs) {
                    if (Constants.WEIGHT.equals(overrideConfig.getType())) {
                        WeightDTO weightDTO = OverrideUtils.configtoWeightDTO(overrideConfig, overrideDTO.getScope(), id);
                        return weightDTO;
                    }
                }
            }
        }
        return null;
    }

    @java.lang.Override
    public void saveBalance(BalancingDTO balancingDTO) {
        String id = ConvertUtil.getIdFromDTO(balancingDTO);
        String scope = ConvertUtil.getScopeFromDTO(balancingDTO);
        String path = getPath(id);
        GovernanceConfiguration dynamicConfiguration = getDynamicConfiguration(balancingDTO.getRegistryAddress());
        //dynamicConfiguration NPE
        String config = dynamicConfiguration.getConfig(path);
        OverrideConfig overrideConfig = OverrideUtils.balancingDTOtoConfig(balancingDTO);
        OverrideDTO overrideDTO = insertConfig(config, overrideConfig, id, scope, Constants.BALANCING);
        dynamicConfiguration.setConfig(path, YamlParser.dumpObject(overrideDTO));

        //for 2.6

        if (scope.equals(Constants.SERVICE)) {
            registerBalancing(balancingDTO);
        }
    }

    @java.lang.Override
    public void updateBalance(BalancingDTO balancingDTO, String registryAddress) {
        String id = ConvertUtil.getIdFromDTO(balancingDTO);
        String scope = ConvertUtil.getScopeFromDTO(balancingDTO);
        String path = getPath(id);
        GovernanceConfiguration dynamicConfiguration = getDynamicConfiguration(registryAddress);
        //dynamicConfiguration NPE
        String config = dynamicConfiguration.getConfig(path);
        BalancingDTO oldBalancing = null;
        if (config != null) {
            OverrideDTO overrideDTO = YamlParser.loadObject(config, OverrideDTO.class);
            List<OverrideConfig> configs = overrideDTO.getConfigs();
            if (configs != null && configs.size() > 0) {
                for (OverrideConfig overrideConfig : configs) {
                    if (Constants.BALANCING.equals(overrideConfig.getType())) {
                        if (overrideDTO.getScope().equals(Constants.SERVICE)) {
                            oldBalancing = OverrideUtils.configtoBalancingDTO(overrideConfig, Constants.SERVICE, overrideDTO.getKey());
                        }
                        int index = configs.indexOf(overrideConfig);
                        OverrideConfig newConfig = OverrideUtils.balancingDTOtoConfig(balancingDTO);
                        configs.set(index, newConfig);
                        break;
                    }
                }
                dynamicConfiguration.setConfig(path, YamlParser.dumpObject(overrideDTO));
            } else {
                //throw exception
            }
        } else {
            //throw exception
        }

        //for 2.6
        if (oldBalancing != null) {
            unregisterBalancing(oldBalancing, registryAddress);
            registerBalancing(balancingDTO);
        }
    }

    @java.lang.Override
    public void deleteBalance(String id, String registryAddress) {
        String path = getPath(id);
        GovernanceConfiguration dynamicConfiguration = getDynamicConfiguration(registryAddress);
        //dynamicConfiguration NPE
        String config = dynamicConfiguration.getConfig(path);
        OverrideConfig oldConfig = null;
        if (config != null) {
            OverrideDTO overrideDTO = YamlParser.loadObject(config, OverrideDTO.class);
            List<OverrideConfig> configs = overrideDTO.getConfigs();
            if (configs != null) {
                for (OverrideConfig overrideConfig : configs) {
                    if (Constants.BALANCING.equals(overrideConfig.getType())) {
                        if (overrideDTO.getScope().equals(Constants.SERVICE)) {
                            oldConfig = overrideConfig;
                        }
                        configs.remove(overrideConfig);
                        break;
                    }
                }
                if (configs.size() == 0) {
                    dynamicConfiguration.deleteConfig(path);
                } else {
                    dynamicConfiguration.setConfig(path, YamlParser.dumpObject(overrideDTO));
                    throw new SystemException("configs is not null,delete failure");
                }
            }
            //for 2.6
            if (oldConfig != null) {
                String key = overrideDTO.getKey();
                BalancingDTO balancingDTO = OverrideUtils.configtoBalancingDTO(oldConfig, Constants.SERVICE, key);
                unregisterBalancing(balancingDTO, registryAddress);
            }
        }
    }

    @java.lang.Override
    public BalancingDTO findBalance(String id, String registryAddress) {
        String path = getPath(id);
        GovernanceConfiguration dynamicConfiguration = getDynamicConfiguration(registryAddress);
        //dynamicConfiguration NPE
        String config = dynamicConfiguration.getConfig(path);
        if (config != null) {
            OverrideDTO overrideDTO = YamlParser.loadObject(config, OverrideDTO.class);
            List<OverrideConfig> configs = overrideDTO.getConfigs();
            if (configs != null) {
                for (OverrideConfig overrideConfig : configs) {
                    if (Constants.BALANCING.equals(overrideConfig.getType())) {
                        BalancingDTO balancingDTO = OverrideUtils.configtoBalancingDTO(overrideConfig, overrideDTO.getScope(), id);
                        return balancingDTO;
                    }
                }
            }
        }
        return null;
    }

    private OverrideDTO insertConfig(String config, OverrideConfig overrideConfig, String key, String scope, String
            configType) {
        OverrideDTO overrideDTO = null;
        if (config == null) {
            overrideDTO = new OverrideDTO();
            overrideDTO.setKey(key);
            overrideDTO.setScope(scope);
            List<OverrideConfig> configs = new ArrayList<>();
            configs.add(overrideConfig);
            overrideDTO.setConfigs(configs);
        } else {
            overrideDTO = YamlParser.loadObject(config, OverrideDTO.class);
            List<OverrideConfig> configs = overrideDTO.getConfigs();
            if (configs != null) {
                for (OverrideConfig o : configs) {
                    if (configType.equals(o.getType())) {
                        configs.remove(o);
                        break;
                    }
                }
                configs.add(overrideConfig);
            } else {
                configs = new ArrayList<>();
                configs.add(overrideConfig);
            }
            overrideDTO.setConfigs(configs);
        }
        return overrideDTO;
    }

    private void overrideDTOToParams(Override override, OverrideConfig config) {
        Map<String, Object> parameters = config.getParameters();
        StringBuilder params = new StringBuilder();

        if (parameters != null) {
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                String value = entry.getKey() + "=" + entry.getValue();
                params.append(value).append("&");
            }
        }
        if (StringUtils.isNotEmpty(params)) {
            int length = params.length();
            if (params.charAt(length - 1) == '&') {
                params.deleteCharAt(length - 1);
            }
        }
        override.setParams(params.toString());
    }

    private List<Override> convertDTOtoOldOverride(DynamicConfigDTO overrideDTO) {
        List<Override> result = new ArrayList<>();
        List<OverrideConfig> configs = overrideDTO.getConfigs();
        for (OverrideConfig config : configs) {
            if (Constants.CONFIGS.contains(config.getType())) {
                continue;
            }
            List<String> apps = config.getApplications();
            List<String> addresses = config.getAddresses();
            for (String address : addresses) {
                if (apps != null && apps.size() > 0) {
                    for (String app : apps) {
                        Override override = new Override();
                        override.setService(overrideDTO.getService());
                        override.setAddress(address);
                        override.setEnabled(overrideDTO.isEnabled());
                        overrideDTOToParams(override, config);
                        override.setApplication(app);
                        result.add(override);
                    }
                } else {
                    Override override = new Override();
                    override.setService(overrideDTO.getService());
                    override.setAddress(address);
                    override.setEnabled(overrideDTO.isEnabled());
                    overrideDTOToParams(override, config);
                    result.add(override);
                }
            }
        }
        return result;
    }

    private String getPath(String key) {
        return prefix + Constants.PATH_SEPARATOR + key + Constants.PATH_SEPARATOR + Constants.CONFIGURATOR;
    }

    private void unregisterWeight(WeightDTO weightDTO, String registryAddress) {
        List<String> addresses = weightDTO.getAddresses();
        if (addresses != null) {
            Weight weight = new Weight();
            weight.setService(weightDTO.getService());
            weight.setWeight(weightDTO.getWeight());
            for (String address : addresses) {
                weight.setAddress(address);
                Override override = new WeightToOverrideAdapter(weight);
                Registry registry = getRedistry(registryAddress);
                if (registry == null) {
                    logger.error("get registry failure,registryAddress = " + registryAddress);
                    return;
                }
                registry.unregister(override.toUrlforUnRegister());
            }
        }
    }

    private void registerWeight(WeightDTO weightDTO) {
        List<String> addresses = weightDTO.getAddresses();
        if (addresses != null) {
            Weight weight = new Weight();
            weight.setService(weightDTO.getService());
            weight.setWeight(weightDTO.getWeight());
            for (String address : addresses) {
                weight.setAddress(address);
                Override override = new WeightToOverrideAdapter(weight);
                Registry registry = getRedistry(weightDTO.getRegistryAddress());
                if (registry == null) {
                    logger.error("get registry failure,registryAddress = " + weightDTO.getRegistryAddress());
                    return;
                }
                registry.register(override.toUrl());
            }
        }
    }

    private void unregisterBalancing(BalancingDTO balancingDTO, String registryAddress) {
        LoadBalance loadBalance = new LoadBalance();
        loadBalance.setService(balancingDTO.getService());
        loadBalance.setMethod(balancingDTO.getMethodName());
        loadBalance.setStrategy(balancingDTO.getStrategy());
        Registry registry = getRedistry(registryAddress);
        if (registry == null) {
            logger.error("get registry failure,registryAddress = " + registryAddress);
            return;
        }
        registry.unregister(new LoadBalance2OverrideAdapter(loadBalance).toUrlforUnRegister());
    }

    private void registerBalancing(BalancingDTO balancingDTO) {
        LoadBalance loadBalance = new LoadBalance();
        loadBalance.setService(balancingDTO.getService());
        loadBalance.setMethod(balancingDTO.getMethodName());
        loadBalance.setStrategy(balancingDTO.getStrategy());
        Registry registry = getRedistry(balancingDTO.getRegistryAddress());
        if (registry == null) {
            logger.error("get registry failure,registryAddress = " + balancingDTO.getRegistryAddress());
            return;
        }
        registry.register(new LoadBalance2OverrideAdapter(loadBalance).toUrl());
    }


    @java.lang.Override
    public List<Override> findByServiceAndAddress(String service, String address, String registryAddress) {
        return SyncUtils.url2OverrideList(findOverrideUrl(service, address, null, registryAddress));
    }

    @java.lang.Override
    public void saveOverride6(Override override, String registryAddress) {
        URL url = override.toUrl();
        LOGGER.info("saveOverride6 url = {}, fullString = {}", JSON.toJSONString(url), url.toFullString());
        getRedistry(registryAddress).register(url);
    }

    @java.lang.Override
    public void deleteOverride6(String hash, String registryAddress) {
        URL oldOverride = findOverrideUrl3(hash, registryAddress);
        if (oldOverride == null) {
            throw new IllegalStateException("Route was changed!");
        }
        LOGGER.info("deleteOverride6 oldOverride = {}, fullString = {}", JSON.toJSONString(oldOverride), oldOverride.toFullString());
        getRedistry(registryAddress).unregister(oldOverride);
    }

    @java.lang.Override
    public void updateOverride6(Override override, String registryAddress) {
        String id = override.getHash();
        if (id == null) {
            throw new IllegalStateException("no override id");
        }
        URL oldOverride = findOverrideUrl3(id, registryAddress);
        if (oldOverride == null) {
            throw new IllegalStateException("Route was changed!");
        }
        URL newOverride = override.toUrl();
        LOGGER.info("updateOverride6 oldOverride = {}, fullString = {}", JSON.toJSONString(oldOverride), oldOverride.toFullString());
        getRedistry(registryAddress).unregister(oldOverride);
        LOGGER.info("updateOverride6 newOverride = {}, fullString = {}", JSON.toJSONString(newOverride), newOverride.toFullString());
        getRedistry(registryAddress).register(newOverride);
    }

    URL findOverrideUrl(String hash, String registryAddress) {
        return findById(hash, registryAddress).toUrlforUnRegister();
    }

    public Override findById(String hash, String registryAddress) {
        return SyncUtils.url2Override(findOverrideUrlPair(hash, registryAddress));
    }

    private Pair<String, URL> findOverrideUrlPair(String hash, String registryAddress) {
        return SyncUtils.filterFromCategory(getSingleRegistryCache(registryAddress), Constants.CONFIGURATORS_CATEGORY, hash);
    }

    URL findOverrideUrl2(String hash, String registryAddress) {
        return findById2(hash, registryAddress).toUrlforUnRegister();
    }

    public Override findById2(String hash, String registryAddress) {
        return SyncUtils.url2Override2(findOverrideUrlPair(hash, registryAddress));
    }

    URL findOverrideUrl3(String hash, String registryAddress) {
        URL url = findOverrideUrlPair(hash, registryAddress).getValue();
        return url.removeParameter(Constants.REGISTRY_KEY);
    }


    /**
     * @param service
     * @param address
     * @param application
     * @return
     */
    private Map<String, URL> findOverrideUrl(String service, String address, String application, String registryAddress) {
        Map<String, String> filter = new HashMap<>();
        filter.put(Constants.CATEGORY_KEY, Constants.CONFIGURATORS_CATEGORY);
        if (service != null && service.length() > 0) {
            filter.put(SyncUtils.SERVICE_FILTER_KEY, service);
        }
        if (address != null && address.length() > 0) {
            filter.put(SyncUtils.ADDRESS_FILTER_KEY, address);
        }
        if (application != null && application.length() > 0) {
            filter.put(Constants.APPLICATION_KEY, application);
        }
        return SyncUtils.filterFromCategory2(getSingleRegistryCache(registryAddress), filter);
    }
}
