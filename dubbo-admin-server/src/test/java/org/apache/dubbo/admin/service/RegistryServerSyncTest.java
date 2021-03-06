///*
// * Licensed to the Apache Software Foundation (ASF) under one or more
// * contributor license agreements.  See the NOTICE file distributed with
// * this work for additional information regarding copyright ownership.
// * The ASF licenses this file to You under the Apache License, Version 2.0
// * (the "License"); you may not use this file except in compliance with
// * the License.  You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package org.apache.dubbo.admin.service;
//
//import org.apache.dubbo.admin.common.util.Constants;
//import org.apache.dubbo.common.URL;
//import org.apache.dubbo.registry.Registry;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.InjectMocks;
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import java.util.*;
//import java.util.concurrent.ConcurrentMap;
//
//import static org.junit.Assert.assertTrue;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.*;
//
//@RunWith(SpringJUnit4ClassRunner.class)
//public class RegistryServerSyncTest {
//    @InjectMocks
//    private RegistryServerSync registryServerSync;
//
//    @Test
//    public void testGetRegistryCache() {
//        registryServerSync.getRegistryCache();
//    }
//
//    @Test
//    public void testAfterPropertiesSet() throws Exception {
//        Registry registry1 = mock(Registry.class);
//        when(registry1.getUrl()).thenReturn(new URL("dubbo", "registry1", 1).addParameter(Constants.REGISTRY_ADDRESS, "r1"));
//        Registry registry2 = mock(Registry.class);
//        when(registry2.getUrl()).thenReturn(new URL("dubbo", "registry2", 1).addParameter(Constants.REGISTRY_ADDRESS, "r2"));
//        List<Registry> registries = Arrays.asList(registry1, registry2);
//        ReflectionTestUtils.setField(registryServerSync, "registries", registries);
//        Map<URL, RegistryServerSync.Notifier> notifiers = new HashMap<>();
//        ReflectionTestUtils.setField(registryServerSync, "notifiers", notifiers);
//        registryServerSync.afterPropertiesSet();
//        for (Registry registry : registries) {
//            verify(registry).subscribe(any(URL.class), any(RegistryServerSync.Notifier.class));
//        }
//    }
//
//    @Test
//    public void testDestroy() throws Exception {
//        Registry registry1 = mock(Registry.class);
//        when(registry1.getUrl()).thenReturn(new URL("dubbo", "registry1", 1).addParameter(Constants.REGISTRY_ADDRESS, "r1"));
//        Registry registry2 = mock(Registry.class);
//        when(registry2.getUrl()).thenReturn(new URL("dubbo", "registry2", 1).addParameter(Constants.REGISTRY_ADDRESS, "r2"));
//        List<Registry> registries = Arrays.asList(registry1, registry2);
//        ReflectionTestUtils.setField(registryServerSync, "registries", registries);
//        Map<URL, RegistryServerSync.Notifier> notifiers = new HashMap<>();
//        notifiers.put(registry1.getUrl(), registryServerSync.new Notifier(registry1.getUrl()));
//        notifiers.put(registry2.getUrl(), registryServerSync.new Notifier(registry2.getUrl()));
//        ReflectionTestUtils.setField(registryServerSync, "notifiers", notifiers);
//
//        registryServerSync.destroy();
//        for (Registry registry : registries) {
//            verify(registry).unsubscribe(any(URL.class), any(RegistryServerSync.Notifier.class));
//        }
//    }
//
//    @Test
//    public void testNotify() {
//        registryServerSync.new Notifier(new URL("", "", 0)).notify(null);
//        registryServerSync.new Notifier(new URL("", "", 0)).notify(Collections.emptyList());
//
//        // when url.getProtocol is not empty protocol
//        URL consumerUrl = mock(URL.class);
//        URL providerUrl = mock(URL.class);
//
//        when(consumerUrl.addParameter(any(), anyString())).thenCallRealMethod();
//        when(consumerUrl.getParameter(Constants.CATEGORY_KEY, Constants.PROVIDERS_CATEGORY)).thenReturn(Constants.CONSUMER_PROTOCOL);
//        when(consumerUrl.getServiceInterface()).thenReturn("org.apache.dubbo.consumer");
//        when(consumerUrl.getServiceKey()).thenReturn("org.apache.dubbo.consumer");
//        when(consumerUrl.toFullString()).thenReturn("consumer://192.168.1.10/sunbufu.dubbo.consumer?application=dubbo&category=consumer&check=false&dubbo=2.7.0&interface=sunbufu.dubbo.consumer&loadbalabce=roundrobin&mehods=sayHi,sayGoodBye&owner=sunbufu&pid=18&protocol=dubbo&side=consumer&timeout=3000&timestamp=1548127407769");
//        when(providerUrl.addParameter(any(), anyString())).thenCallRealMethod();
//        when(providerUrl.getParameter(Constants.CATEGORY_KEY, Constants.PROVIDERS_CATEGORY)).thenReturn(Constants.PROVIDER_PROTOCOL);
//        when(providerUrl.getServiceInterface()).thenReturn("org.apache.dubbo.provider");
//        when(providerUrl.getServiceKey()).thenReturn("org.apache.dubbo.provider");
//        when(providerUrl.toFullString()).thenReturn("consumer://192.168.1.10/sunbufu.dubbo.consumer?application=dubbo&category=consumer&check=false&dubbo=2.6.2&interface=sunbufu.dubbo.consumer&loadbalabce=roundrobin&mehods=sayHi,sayGoodBye&owner=sunbufu&pid=18&protocol=dubbo&side=consumer&timeout=3000&timestamp=1548127407769");
//
//        registryServerSync.new Notifier(new URL("", "", 0)).notify(Arrays.asList(consumerUrl, consumerUrl, providerUrl));
//
//        ConcurrentMap<String, Map<String, URL>> consumerMap = registryServerSync.getRegistryCache().get(Constants.CONSUMER_PROTOCOL);
//        assertTrue(consumerMap.keySet().contains("org.apache.dubbo.consumer"));
//        ConcurrentMap<String, Map<String, URL>> providerMap = registryServerSync.getRegistryCache().get(Constants.PROVIDER_PROTOCOL);
//        assertTrue(providerMap.keySet().contains("org.apache.dubbo.provider"));
//
//        // when url.getProtocol is empty protocol
//        when(consumerUrl.getProtocol()).thenReturn(Constants.EMPTY_PROTOCOL);
//        when(consumerUrl.getParameter(Constants.GROUP_KEY)).thenReturn("dubbo");
//        when(consumerUrl.getParameter(Constants.VERSION_KEY)).thenReturn("2.7.0");
//        registryServerSync.new Notifier(new URL("", "", 0)).notify(Collections.singletonList(consumerUrl));
//
//        assertTrue(!consumerMap.keySet().contains("org.apache.dubbo.consumer"));
//
//        // when url's group or version is ANY_VALUE (*)
//        when(providerUrl.getProtocol()).thenReturn(Constants.EMPTY_PROTOCOL);
//        when(providerUrl.getParameter(Constants.GROUP_KEY)).thenReturn(Constants.ANY_VALUE);
//        registryServerSync.new Notifier(new URL("", "", 0)).notify(Collections.singletonList(providerUrl));
//
//        assertTrue(!providerMap.keySet().contains("org.apache.dubbo.provider"));
//    }
//}
