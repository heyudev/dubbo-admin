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
package org.apache.dubbo.admin.common.util;

import org.apache.dubbo.admin.model.domain.Consumer;
import org.apache.dubbo.admin.model.domain.Override;
import org.apache.dubbo.admin.model.domain.Provider;
import org.apache.dubbo.admin.model.dto.ConsumerDTO;
import org.apache.dubbo.admin.model.dto.ProviderDTO;
import org.apache.dubbo.admin.service.OverrideService;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class SyncUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncUtils.class);

    public static final DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static final String SERVICE_FILTER_KEY = ".service";

    public static final String ADDRESS_FILTER_KEY = ".address";

    public static final String ID_FILTER_KEY = ".id";

    public static final String APPLICATION_FILTER_KEY = ".application";

    public static final String REGISTRY_FILTER_KEY = ".registry";

    public static Provider url2Provider(Pair<String, URL> pair) {
        if (pair == null) {
            return null;
        }

        String id = pair.getKey();
        URL url = pair.getValue();

        if (url == null)
            return null;

        Provider p = new Provider();
        p.setHash(id);
        p.setService(url.getServiceKey());
        p.setAddress(url.getAddress());
        p.setApplication(url.getParameter(Constants.APPLICATION_KEY));
        p.setUrl(url.toIdentityString());
        p.setParameters(url.toParameterString());
        p.setRegistry(url.getParameter(Constants.REGISTRY_KEY));

        p.setDynamic(url.getParameter("dynamic", true));
        p.setEnabled(url.getParameter(Constants.ENABLED_KEY, true));
        p.setWeight(url.getParameter(Constants.WEIGHT_KEY, Constants.DEFAULT_WEIGHT));
        p.setUsername(url.getParameter("owner"));

        return p;
    }

    /**
     * @param pair
     * @return
     */
    public static ProviderDTO url2ProviderDTO(Pair<String, URL> pair, List<Override> overrides) {
        if (pair == null) {
            return null;
        }

        String id = pair.getKey();
        URL url = pair.getValue();

        if (url == null) {
            return null;
        }

        ProviderDTO p = new ProviderDTO();
        p.setHash(id);
        p.setService(url.getServiceKey());
        p.setAddress(url.getAddress());
        p.setApplication(url.getParameter(Constants.APPLICATION_KEY));
        p.setUrl(url.toIdentityString());
        p.setParameters(url.toParameterString());
        p.setRegistry(url.getParameter(Constants.REGISTRY_KEY));

        p.setDynamic(url.getParameter("dynamic", true));
        p.setEnabled(url.getParameter(Constants.ENABLED_KEY, true));
        //设置默认值
        p.setWeight(url.getParameter(Constants.WEIGHT_KEY, Constants.DEFAULT_WEIGHT));
        p.setUsername(url.getParameter("owner"));


        p.setFullUrl(url.toFullString());
        p.setAppName(url.getParameter(Constants.APPLICATION_KEY));
        p.setPid(url.getParameter("pid"));
        p.setDubbo(url.getParameter("dubbo"));
        p.setGroup(url.getParameter("group"));
        p.setMethods(url.getParameter("methods"));
        p.setSide(url.getParameter("side"));
        p.setVersion(url.getParameter("version"));
        if (!Objects.equals(url.getParameter("timestamp"), null) && !Objects.equals(url.getParameter("timestamp"), "")) {
            Date date = new Date(url.getParameter("timestamp", 0L));
            p.setTimestamp(sdf.format(date) + "(" + url.getParameter("timestamp") + ")");
        }
        p.setRetries(url.getParameter("retries"));
        p.setRevision(url.getParameter("revision"));

        boolean enabled = isProviderEnabled(p, overrides);
        if (enabled) {
            p.setStatus("启用");
        } else {
            p.setStatus("禁用");
        }

        p.setWeight(getProviderWeight(p, overrides));

        return p;
    }

    public static List<Provider> url2ProviderList(Map<String, URL> ps) {
        List<Provider> ret = new ArrayList<>();
        for (Map.Entry<String, URL> entry : ps.entrySet()) {
            ret.add(url2Provider(new Pair<>(entry.getKey(), entry.getValue())));
        }
        return ret;
    }

    /**
     * @param ps
     * @param overrideService
     * @return
     */
    public static List<Provider> url2ProviderList2(Map<String, URL> ps, OverrideService overrideService, String registryAddress) {
        List<Provider> ret = new ArrayList<>();
        for (Map.Entry<String, URL> entry : ps.entrySet()) {
            ret.add(url2Provider2(new Pair<>(entry.getKey(), entry.getValue()), overrideService, registryAddress));
        }
        return ret;
    }

    public static Provider url2Provider2(Pair<String, URL> pair, OverrideService overrideService, String registryAddress) {
        if (pair == null) {
            return null;
        }

        String id = pair.getKey();
        URL url = pair.getValue();

        if (url == null)
            return null;

        Provider p = new Provider();
        p.setHash(id);
        p.setService(url.getServiceKey());
        p.setAddress(url.getAddress());
        p.setApplication(url.getParameter(Constants.APPLICATION_KEY));
        p.setUrl(url.toIdentityString());
        p.setParameters(url.toParameterString());
        p.setRegistry(url.getParameter(Constants.REGISTRY_KEY));

        p.setDynamic(url.getParameter("dynamic", true));
        p.setEnabled(url.getParameter(Constants.ENABLED_KEY, true));
        //设置默认值
        p.setWeight(url.getParameter(Constants.WEIGHT_KEY, Constants.DEFAULT_WEIGHT));
        p.setUsername(url.getParameter("owner"));

        List<Override> overrides = overrideService.findByServiceAndAddress(p.getService(), p.getAddress(), registryAddress);
        boolean enabled = isProviderEnabled(p, overrides);
        if (enabled) {
            p.setStatus("启用");
        } else {
            p.setStatus("禁用");
        }

        p.setWeight(getProviderWeight(p, overrides));

        return p;
    }

    public static Consumer url2Consumer(Pair<String, URL> pair) {
        if (pair == null) {
            return null;
        }

        String id = pair.getKey();
        URL url = pair.getValue();

        if (null == url) {
            return null;
        }

        Consumer c = new Consumer();
        c.setHash(id);
        c.setService(url.getServiceKey());
        c.setAddress(url.getHost());
        c.setApplication(url.getParameter(Constants.APPLICATION_KEY));
        c.setParameters(url.toParameterString());
        c.setRegistry(url.getParameter(Constants.REGISTRY_KEY));

        return c;
    }

    /**
     * @param pair
     * @return
     */
    public static ConsumerDTO url2ConsumerDTO(Pair<String, URL> pair) {
        if (pair == null) {
            return null;
        }

        String id = pair.getKey();
        URL url = pair.getValue();

        if (null == url) {
            return null;
        }

        ConsumerDTO c = new ConsumerDTO();
        c.setHash(id);
        c.setService(url.getServiceKey());
        c.setAddress(url.getHost());
        c.setApplication(url.getParameter(Constants.APPLICATION_KEY));
        c.setParameters(url.toParameterString());
        c.setRegistry(url.getParameter(Constants.REGISTRY_KEY));

        c.setFullUrl(url.toFullString());
        c.setAppName(url.getParameter(Constants.APPLICATION_KEY));
        c.setPid(url.getParameter("pid"));
        c.setDubbo(url.getParameter("dubbo"));
        c.setGroup(url.getParameter("group"));
        c.setMethods(url.getParameter("methods"));
        c.setSide(url.getParameter("side"));
        c.setVersion(url.getParameter("version"));
        if (!Objects.equals(url.getParameter("timestamp"), null) && !Objects.equals(url.getParameter("timestamp"), "")) {
            Date date = new Date(url.getParameter("timestamp", 0L));
            c.setTimestamp(sdf.format(date) + "(" + url.getParameter("timestamp") + ")");
        }
        c.setCheck(url.getParameter("check", false));

        return c;
    }

    public static List<Consumer> url2ConsumerList(Map<String, URL> cs) {
        List<Consumer> list = new ArrayList<Consumer>();
        if (cs == null) {
            return list;
        }
        for (Map.Entry<String, URL> entry : cs.entrySet()) {
            list.add(url2Consumer(new Pair<>(entry.getKey(), entry.getValue())));
        }
        return list;
    }

    public static <SM extends Map<String, Map<String, URL>>> Map<String, URL> filterFromApplication(Map<String, SM> urls, Map<String, String> filter) {
        String c = filter.get(Constants.CATEGORY_KEY);
        if (c == null) {
            throw new IllegalArgumentException("no category");
        }
        Map<String, URL> ret = new HashMap<>(16);
        if (urls.get(c) == null) {
            return ret;
        }
        String application = filter.get(APPLICATION_FILTER_KEY);
        for (Map.Entry<String, Map<String, URL>> entry : urls.get(c).entrySet()) {
            for (Map.Entry<String, URL> entry1 : entry.getValue().entrySet()) {
                URL url = entry1.getValue();
                if (url != null && Objects.equals(application, url.getParameter(Constants.APPLICATION))) {
                    ret.putAll(entry.getValue());
                }
            }
        }
        return ret;
    }

    // Map<category, Map<servicename, Map<Long, URL>>>
    public static <SM extends Map<String, Map<String, URL>>> Map<String, URL> filterFromCategory(Map<String, SM> urls, Map<String, String> filter) {
        String c = filter.get(Constants.CATEGORY_KEY);
        if (c == null) {
            throw new IllegalArgumentException("no category");
        }
        filter.remove(Constants.CATEGORY_KEY);
        return filterFromService(urls.get(c), filter);
    }


    // Map<servicename, Map<Long, URL>>
    public static Map<String, URL> filterFromService(Map<String, Map<String, URL>> urls, Map<String, String> filter) {
        Map<String, URL> ret = new HashMap<>();
        if (urls == null) {
            return ret;
        }
        String s = filter.remove(SERVICE_FILTER_KEY);
        if (s == null) {
            for (Map.Entry<String, Map<String, URL>> entry : urls.entrySet()) {
                filterFromUrls(entry.getValue(), ret, filter);
            }
        } else {
            Map<String, URL> map = urls.get(s);
            filterFromUrls(map, ret, filter);
        }
        return ret;
    }

    // Map<Long, URL>
    static void filterFromUrls(Map<String, URL> from, Map<String, URL> to, Map<String, String> filter) {
        if (from == null || from.isEmpty()) return;

        for (Map.Entry<String, URL> entry : from.entrySet()) {
            URL url = entry.getValue();

            boolean match = true;
            for (Map.Entry<String, String> e : filter.entrySet()) {
                String key = e.getKey();
                String value = e.getValue();

                if (ADDRESS_FILTER_KEY.equals(key)) {
                    if (!value.equals(url.getIp())) {
                        match = false;
                        break;
                    }
                } else {
                    if (!value.equals(url.getParameter(key))) {
                        match = false;
                        break;
                    }
                }
            }

            if (match) {
                to.put(entry.getKey(), url);
            }
        }
    }

    /**
     * @param urls
     * @param filter
     * @param <SM>
     * @return
     */
    public static <SM extends Map<String, Map<String, URL>>> Map<String, URL> filterFromCategoryAndRegistry(Map<String, SM> urls, Map<String, String> filter) {
        String c = filter.get(Constants.CATEGORY_KEY);
        if (c == null) {
            throw new IllegalArgumentException("no category");
        }
        Map<String, URL> ret = new HashMap<>(16);
        if (urls.get(c) == null) {
            return ret;
        }
        //s = group/service:version
        String service = filter.get(SERVICE_FILTER_KEY);
        for (Map.Entry<String, Map<String, URL>> entry : urls.get(c).entrySet()) {
            Map<String, URL> map = urls.get(c).get(service);
            if (map != null) {
                ret.putAll(map);
            }
        }
        return ret;
    }

    /**
     * @param urls   Map<category, Map<group/service:version@registry, Map<Long, URL>>>
     * @param filter
     * @param <SM>
     * @return
     */
    public static <SM extends Map<String, Map<String, URL>>> Map<String, URL> filterFromCategoryWithoutRegistry(Map<String, SM> urls, Map<String, String> filter) {
        String c = filter.get(Constants.CATEGORY_KEY);
        if (c == null) {
            throw new IllegalArgumentException("no category");
        }
        filter.remove(Constants.CATEGORY_KEY);
        return filterFromServiceWithoutRegistry(urls.get(c), filter);
    }


    /**
     * @param urls   Map<group/service:version@registry, Map<String, URL>>
     * @param filter
     * @return
     */
    public static Map<String, URL> filterFromServiceWithoutRegistry(Map<String, Map<String, URL>> urls, Map<String, String> filter) {
        Map<String, URL> ret = new HashMap<>(16);
        if (urls == null) {
            return ret;
        }
        //s = group/service:version
        String s = filter.remove(SERVICE_FILTER_KEY);
        if (s == null) {
            for (Map.Entry<String, Map<String, URL>> entry : urls.entrySet()) {
                filterFromUrlsWithoutRegistry(entry.getValue(), ret, filter);
            }
        } else {
            for (Map.Entry<String, Map<String, URL>> entry : urls.entrySet()) {
                if (entry == null) {
                    continue;
                }
                if (entry.getKey().contains(s)) {
                    filterFromUrlsWithoutRegistry(entry.getValue(), ret, filter);
                }
            }
        }
        return ret;
    }

    /**
     * @param from
     * @param to
     * @param filter
     */
    static void filterFromUrlsWithoutRegistry(Map<String, URL> from, Map<String, URL> to, Map<String, String> filter) {
        if (from == null || from.isEmpty()) {
            return;
        }
        for (Map.Entry<String, URL> entry : from.entrySet()) {
            to.put(entry.getKey(), entry.getValue());
        }
    }


    public static <SM extends Map<String, Map<String, URL>>> Pair<String, URL> filterFromCategory(Map<String, SM> urls, String category, String id) {
        SM services = urls.get(category);
        if (services == null) {
            return null;
        }

        for (Map.Entry<String, Map<String, URL>> e1 : services.entrySet()) {
            Map<String, URL> u = e1.getValue();
            if (u.containsKey(id)) {
                return new Pair<>(id, u.get(id));
            }
        }
        return null;
    }


    /**
     * @param cs
     * @return
     */
    public static List<Override> url2OverrideList(Map<String, URL> cs) {
        List<Override> list = new ArrayList<>();
        if (cs == null) {
            return list;
        }
        for (Map.Entry<String, URL> entry : cs.entrySet()) {
            list.add(url2Override(new Pair<>(entry.getKey(), entry.getValue())));
        }
        return list;
    }

    /**
     * @param pair
     * @return
     */
    public static Override url2Override(Pair<String, URL> pair) {
        if (pair == null) {
            return null;
        }

        String hash = pair.getKey();
        URL url = pair.getValue();

        if (null == url) {
            return null;
        }
        Override o = new Override();
        o.setHash(hash);

        Map<String, String> parameters = new HashMap<>(url.getParameters());

        o.setService(url.getServiceKey());
        parameters.remove(Constants.INTERFACE_KEY);
        parameters.remove(Constants.GROUP_KEY);
        parameters.remove(Constants.VERSION_KEY);
        parameters.remove(Constants.APPLICATION_KEY);
        parameters.remove(Constants.CATEGORY_KEY);
        parameters.remove(Constants.DYNAMIC_KEY);
        parameters.remove(Constants.ENABLED_KEY);

        o.setEnabled(url.getParameter(Constants.ENABLED_KEY, true));

        String host = url.getHost();
        boolean anyhost = url.getParameter(Constants.ANYHOST_VALUE, false);
        if (!anyhost || !"0.0.0.0".equals(host)) {
            o.setAddress(url.getAddress());
        }

        o.setApplication(url.getParameter(Constants.APPLICATION_KEY, url.getUsername()));
        parameters.remove(Constants.VERSION_KEY);
        parameters.remove(Constants.REGISTRY_KEY);

        o.setParams(StringUtils.toQueryString(parameters));

        return o;
    }

    /**
     * @param pair
     * @return
     */
    public static Override url2Override2(Pair<String, URL> pair) {
        if (pair == null) {
            return null;
        }

        String hash = pair.getKey();
        URL url = pair.getValue();

        if (null == url) {
            return null;
        }
        Override o = new Override();
        o.setHash(hash);

        Map<String, String> parameters = new HashMap<>(url.getParameters());

        o.setService(url.getServiceKey());
        parameters.remove(Constants.INTERFACE_KEY);
        parameters.remove(Constants.GROUP_KEY);
        parameters.remove(Constants.VERSION_KEY);
        parameters.remove(Constants.APPLICATION_KEY);
        parameters.remove(Constants.CATEGORY_KEY);
        parameters.remove(Constants.DYNAMIC_KEY);
        parameters.remove(Constants.ENABLED_KEY);

        if (url.getParameter(Constants.ENABLED_KEY) != null && !"".equals(url.getParameter(Constants.ENABLED_KEY))) {
            o.setEnabled(url.getParameter(Constants.ENABLED_KEY, true));
        }

        String host = url.getHost();
        boolean anyhost = url.getParameter(Constants.ANYHOST_VALUE, false);
        if (!anyhost || !"0.0.0.0".equals(host)) {
            o.setAddress(url.getAddress());
        }

        o.setApplication(url.getParameter(Constants.APPLICATION_KEY, url.getUsername()));
        parameters.remove(Constants.VERSION_KEY);
        parameters.remove(Constants.REGISTRY_KEY);

        o.setParams(StringUtils.toQueryString(parameters));

        return o;
    }

    /**
     * 获取状态
     *
     * @param provider
     * @param oList
     * @return
     */
    public static boolean isProviderEnabled(Provider provider, List<Override> oList) {
        for (Override o : oList) {
            if (o.isMatch(provider)) {
                Map<String, String> params = StringUtils.parseQueryString(o.getParams());
                String disabled = params.get(Constants.DISABLED_KEY);
                if (disabled != null && disabled.length() > 0) {
                    return !"true".equals(disabled);
                }
            }
        }
        return provider.isEnabled();
    }

    /**
     * 获取权重
     *
     * @param provider
     * @param oList
     * @return
     */
    public static int getProviderWeight(Provider provider, List<Override> oList) {
        for (Override o : oList) {
            if (o.isMatch(provider)) {
                Map<String, String> params = StringUtils.parseQueryString(o.getParams());
                String weight = params.get(Constants.WEIGHT_KEY);
                if (weight != null && weight.length() > 0) {
                    return Integer.parseInt(weight);
                }
            }
        }
        return provider.getWeight();
    }


    // Map<category, Map<servicename, Map<Long, URL>>>
    public static <SM extends Map<String, Map<String, URL>>> Map<String, URL> filterFromCategory2(Map<String, SM> urls, Map<String, String> filter) {
        String c = filter.get(Constants.CATEGORY_KEY);
        if (c == null) {
            throw new IllegalArgumentException("no category");
        }
        filter.remove(Constants.CATEGORY_KEY);
        return filterFromService2(urls.get(c), filter);
    }


    // Map<servicename, Map<Long, URL>>
    public static Map<String, URL> filterFromService2(Map<String, Map<String, URL>> urls, Map<String, String> filter) {
        Map<String, URL> ret = new HashMap<>();
        if (urls == null) {
            return ret;
        }
        String s = (String) filter.remove(SERVICE_FILTER_KEY);
        if (s == null) {
            for (Map.Entry<String, Map<String, URL>> entry : urls.entrySet()) {
                filterFromUrls2(entry.getValue(), ret, filter);
            }
        } else {
            Map<String, URL> map = urls.get(s);
            filterFromUrls2(map, ret, filter);
        }

        return ret;
    }

    // Map<Long, URL>
    static void filterFromUrls2(Map<String, URL> from, Map<String, URL> to, Map<String, String> filter) {
        if (from == null || from.isEmpty()) {
            return;
        }

        for (Map.Entry<String, URL> entry : from.entrySet()) {
            URL url = entry.getValue();

            boolean match = true;
            for (Map.Entry<String, String> e : filter.entrySet()) {
                String key = e.getKey();
                String value = e.getValue();

                if (ADDRESS_FILTER_KEY.equals(key)) {
                    if (!value.equals(url.getAddress())) {
                        match = false;
                        break;
                    }
                } else {
                    if (!value.equals(url.getParameter(key))) {
                        match = false;
                        break;
                    }
                }
            }

            if (match) {
                to.put(entry.getKey(), url);
            }
        }
    }
}
