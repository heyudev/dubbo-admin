package org.apache.dubbo.admin.service.impl;

import org.apache.curator.framework.CuratorFramework;
import org.apache.dubbo.admin.common.exception.ParamValidationException;
import org.apache.dubbo.admin.common.exception.ServiceException;
import org.apache.dubbo.admin.common.util.CuratorZkClient;
import org.apache.dubbo.admin.model.domain.*;
import org.apache.dubbo.admin.service.ApplicationService;
import org.apache.dubbo.admin.service.ConsumerService;
import org.apache.dubbo.admin.service.ProviderService;
import org.apache.dubbo.admin.service.ZookeeperService;
import org.apache.dubbo.admin.task.ZookeeperMonitorTask;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.lang.Override;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author heyudev
 * @date 2019/05/31
 */
@Service
public class ZookeeperServiceImpl implements ZookeeperService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperServiceImpl.class);

    @Autowired
    private ZookeeperMonitorTask task;

    @Autowired
    private ProviderService providerService;

    @Autowired
    private ConsumerService consumerService;

    @Autowired
    private ApplicationService applicationService;

    public ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<String, List<String>>>> getStatusCache() {
        return task.getStatusCache();
    }

    private static final ConcurrentMap<String, CuratorZkClient> ZK_CLIENT_MAP = new ConcurrentHashMap<>();

    @Override
    public ConcurrentMap<String, ConcurrentMap<String, List<String>>> getZookeeperInfo(String address) {
        return getStatusCache().get(address);
    }

    @Override
    public List<Node> getChildren(String address, String parent) {
        if (address == null || "".equals(address) || parent == null) {
            throw new ParamValidationException("zookeeper address and node path can not be null");
        }
        CuratorZkClient client = getCuratorZkClient(address);
        return client.getChildren(parent);
    }

    @Override
    public String getData(String address, String path) {
        if (address == null || "".equals(address) || path == null) {
            throw new ParamValidationException("zookeeper address and node path can not be null");
        }
        CuratorZkClient client = getCuratorZkClient(address);
        try {
            path = getPath(path);
            String data = new String(client.readData(path, null, false));
            if (data != null && data.length() > 1000) {
                return data.substring(0, 1000);
            }
            return data;
        } catch (KeeperException e) {
            LOGGER.error("getData error ", e);
        } catch (InterruptedException e) {
            LOGGER.error("getData error ", e);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("getData error ", e);
        }
        return null;
    }

    private String getPath(String path) throws UnsupportedEncodingException {
        if (path.contains("consumer:")) {
            int index = path.indexOf("consumer:");
            String parent = path.substring(0, index);
            String nodePath = path.substring(index);
            path = parent + URLEncoder.encode(nodePath, "UTF-8");
        }
        if (path.contains("dubbo:")) {
            int index = path.indexOf("dubbo:");
            String parent = path.substring(0, index);
            String nodePath = path.substring(index);
            path = parent + URLEncoder.encode(nodePath, "UTF-8");
        }
        if (path.contains("override:")) {
            int index = path.indexOf("override:");
            String parent = path.substring(0, index);
            String nodePath = path.substring(index);
            path = parent + URLEncoder.encode(nodePath, "UTF-8");
        }
        if (path.contains("route:")) {
            int index = path.indexOf("route:");
            String parent = path.substring(0, index);
            String nodePath = path.substring(index);
            path = parent + URLEncoder.encode(nodePath, "UTF-8");
        }
        return path;
    }

    private CuratorZkClient getCuratorZkClient(String address) {
        CuratorZkClient client = ZK_CLIENT_MAP.get(address);
        if (client == null) {
            CuratorFramework curatorFramework = CuratorZkClient.create(address);
            curatorFramework.start();
            client = new CuratorZkClient(curatorFramework);
            ZK_CLIENT_MAP.put(address, client);
        }
        if (!client.getZookeeperState().isAlive()) {
            throw new ServiceException("zookeeper [" + address + "] is not alive");
        }
        return client;
    }

    @Override
    public Map<String, Object> appsDependencies(String address) {
        Map<String, Object> result = new HashMap<>(2);
        List<Application> applications = applicationService.getApplications(address);
        List<Line> lines = new ArrayList<>();
        result.put("applications", applications);
        for (Application application : applications) {
            List<Consumer> consumers = consumerService.findByApplication(application.getApplication(), address);
            for (Consumer consumer : consumers) {
                List<Provider> providers = providerService.findByServiceAndRegistry(consumer.getService(), address);
                if (providers != null && !providers.isEmpty()) {
                    Line line = new Line();
                    line.setSource(consumer.getApplication());
                    line.setTarget(providers.get(0).getApplication());
                    line.setName(consumer.getApplication() + "依赖" + providers.get(0).getApplication());
                    if (lines.contains(line)) {
                        continue;
                    }
                    lines.add(line);
                }
            }
        }
        result.put("lines", lines);
        return result;
    }

    public static void main(String[] args) {
        ZookeeperServiceImpl zookeeperService = new ZookeeperServiceImpl();
//        List<Node> children = zookeeperService.getChildren("10.2.39.11:2181", "/dubbo");
//        String path = "/matrix-publish-web-esjob/agentCheckOuttimeJob/leader/election/instance";
//        String parent = "/dubbo/brain.api.IJarVersionService/consumers/";
//        String path = "consumer://10.7.8.3/brain.api.IJarVersionService?application=matrix-process&category=consumers&check=false&default.check=false&default.timeout=15000&dubbo=2.6.1&interface=brain.api.IJarVersionService&methods=findAvailableJarVersionRecord,updateJarVersionRecord,produceJarVersionRecord,findLatestDeployJarVersionRecord,onlineJarVersionRecord&pid=19069&side=consumer&timestamp=1562678667780";
//        String path = "/dubbo/brain.api.IJarVersionService/consumers/consumer://10.7.8.3/brain.api.IJarVersionService?application=matrix-process&category=consumers&check=false&default.check=false&default.timeout=15000&dubbo=2.6.1&interface=brain.api.IJarVersionService&methods=findAvailableJarVersionRecord,updateJarVersionRecord,produceJarVersionRecord,findLatestDeployJarVersionRecord,onlineJarVersionRecord&pid=19069&side=consumer&timestamp=1562678667780";
        String path = "/dubbo/com.imooc.springboot.dubbo.demo.DemoService/configurators/override://127.0.0.1/com.imooc.springboot.dubbo.demo.DemoService?category=configurators&dynamic=false&enabled=true&weight=100";
        String data = zookeeperService.getData("10.2.39.11:2181", path);
        System.out.println("------------------------");
        System.out.println(data);
//        System.out.println(children);
    }
}
