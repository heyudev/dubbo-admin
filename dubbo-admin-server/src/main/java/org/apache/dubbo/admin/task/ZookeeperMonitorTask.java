package org.apache.dubbo.admin.task;

import com.alibaba.fastjson.JSON;
import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.google.common.base.Splitter;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.dubbo.admin.common.util.FourLetterWords;
import org.apache.dubbo.admin.model.domain.Registry;
import org.apache.dubbo.admin.service.RegistryService;
import org.apache.dubbo.admin.common.util.HttpClientUtil;
import org.apache.dubbo.admin.common.util.HttpPoolClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * @author heyudev
 * @date 2019/05/31
 */
@Component
public class ZookeeperMonitorTask implements SimpleJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperMonitorTask.class);

    private static final String ECHO = "echo";
    private static final String BASH = "/bin/bash";
    private static final String C = "-c";
    private static final String SEPARATE = "|";
    private static final String NC = "nc";
    /**
     * <zk集群,<单个zk,<监控项,每一行的数据>>>
     */
    private final ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<String, List<String>>>> statusCache = new ConcurrentHashMap<>();

//    private static final ThreadFactory name

    private static final HttpPoolClient HTTP_POOL_CLIENT = HttpClientUtil.useDefault();

    public ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<String, List<String>>>> getStatusCache() {
        return statusCache;
    }

    @Value("${monitor.url}")
    private String monitorUrl;
    /**
     * 开关 true 开 false 关
     */
    @Value("${zk.monitor.switch}")
    private boolean zkMonitorSwitch;

    @Autowired
    private RegistryService registryService;

    @PostConstruct
    public void init() {
    }

//    @Scheduled(cron = "0 0/1 * * * ?")
//    public void conf() {
//        executor.execute(() -> getStatusInfo(FourLetterWords.CONF));
//    }
//
//    @Scheduled(cron = "10 0/1 * * * ?")
//    public void cons() {
//        executor.execute(() -> getStatusInfo(FourLetterWords.CONS));
//    }
//
//    @Scheduled(cron = "20 0/1 * * * ?")
//    public void envi() {
//        executor.execute(() -> getStatusInfo(FourLetterWords.ENVI));
//    }
//
//    @Scheduled(cron = "30 0/1 * * * ?")
//    public void ruok() {
//        executor.execute(() -> getStatusInfo(FourLetterWords.RUOK));
//    }
//
//    @Scheduled(cron = "40 0/1 * * * ?")
//    public void stat() {
//        executor.execute(() -> getStatusInfo(FourLetterWords.STAT));
//    }
//
//    @Scheduled(cron = "50 0/1 * * * ?")
//    public void wchs() {
//        executor.execute(() -> getStatusInfo(FourLetterWords.WCHS));
//    }
//
//    @Scheduled(cron = "50 0/1 * * * ?")
//    public void mntr() {
//        executor.execute(() -> getStatusInfo(FourLetterWords.MNTR));
//    }

//    @Scheduled(cron = "0 0/1 * * * ?")
//    public void mntr() {
//        if (zkMonitorSwitch) {
//            executor.execute(() -> getMntr(FourLetterWords.MNTR));
//        }
//    }

    /**
     * 根据四字命令获取zk信息
     *
     * @param fourLetterWords
     */
    private void getStatusInfo(String fourLetterWords) {
//        List<Registry> registryList = registryService.getAllRegistryByEnv(EnvEnum.RELEASE.getValue());
        List<Registry> registryList = registryService.getAllRegistryOfMonitor();
        for (Registry registry : registryList) {
            String registryAddress = registry.getRegAddress();
            if (statusCache.get(registryAddress) == null) {
                ConcurrentMap<String, ConcurrentMap<String, List<String>>> concurrentMap = new ConcurrentHashMap();
                statusCache.put(registryAddress, concurrentMap);
            }
            List<String> zkList = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(registryAddress);
            if (zkList.isEmpty()) {
                continue;
            }
            for (String zk : zkList) {
                if (statusCache.get(registryAddress).get(zk) == null) {
                    ConcurrentMap<String, List<String>> map = new ConcurrentHashMap<>();
                    statusCache.get(registryAddress).put(zk, map);
                }
                String[] zkInfo = zk.split(":");
                List<String> cmd = assemblyCmd(fourLetterWords, zkInfo);
                ProcessBuilder builder = new ProcessBuilder(cmd);
                try {
                    //将输出流与错误流合并
                    builder.redirectErrorStream(true);
                    Process process = builder.start();
                    BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String output;
                    List<String> result = new ArrayList<>();
                    while (null != (output = br.readLine())) {
                        result.add(output);
                    }
                    process.waitFor();
                    statusCache.get(registryAddress).get(zk).put(fourLetterWords, result);
                } catch (IOException e) {
                    LOGGER.error("assemblyCmd reader error :", e);
                } catch (InterruptedException e) {
                    LOGGER.error("assemblyCmd interrupted error :", e);
                }
            }
        }
    }

    /**
     * 获取监控数据推送到monitor
     *
     * @param fourLetterWords
     */
    private void getMntr(String fourLetterWords) {
        long start = System.currentTimeMillis();
        LOGGER.info("zookeeper monitor getMntr");
//        List<Registry> registryList = registryService.getAllRegistryByEnv(EnvEnum.RELEASE.getValue());
        List<Registry> registryList = registryService.getAllRegistryOfMonitor();
        long timestamp = System.currentTimeMillis();
        for (Registry registry : registryList) {
            if (registry == null || Objects.equals(registry.getRegAddress(), null) || Objects.equals(registry.getRegAddress(), "") ||
                    Objects.equals(registry.getAppCode(), null) || Objects.equals(registry.getAppCode(), "")) {
                LOGGER.info("registry continue = {}", registry);
                continue;
            }
            String registryAddress = registry.getRegAddress();
            List<String> zkList = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(registryAddress);
            if (zkList.isEmpty()) {
                continue;
            }
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                LOGGER.error("sleep error ", e);
            }
            for (String zk : zkList) {
                String[] zkInfo = zk.split(":");
                List<String> cmd = assemblyCmd(fourLetterWords, zkInfo);
                ProcessBuilder builder = new ProcessBuilder(cmd);
                Map<String, Object> params = new HashMap<>();
                params.put("appName", registry.getAppCode());
                params.put("host", zkInfo[0]);
                List<Map<String, Object>> metrics = new ArrayList<>();
                //将输出流与错误流合并
                builder.redirectErrorStream(true);
                Process process;
                try {
                    process = builder.start();
                } catch (IOException e) {
                    LOGGER.error("builder.start error ", e);
                    continue;
                }
                //process阻塞 输入流和错误流，没有处理，就会发生阻塞，此处已经合并
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                try {
                    String output;
                    while (null != (output = inputReader.readLine())) {
                        //followers数
                        if (output.trim().contains("zk_followers")) {
                            String zkFollowers = output.trim().replace("zk_followers", "").trim();
                            if (NumberUtils.isDigits(zkFollowers)) {
                                Map<String, Object> metric = new HashMap<>();
                                metric.put("name", "zk_followers");
                                metric.put("timestamp", timestamp);
                                metric.put("value", Long.valueOf(zkFollowers));
                                metrics.add(metric);
                            }
                        }
                        //同步的follower数
                        if (output.trim().contains("zk_synced_followers")) {
                            String zkSyncedFollowers = output.trim().replace("zk_synced_followers", "").trim();
                            if (NumberUtils.isDigits(zkSyncedFollowers)) {
                                Map<String, Object> metric = new HashMap<>();
                                metric.put("name", "zk_synced_followers");
                                metric.put("timestamp", timestamp);
                                metric.put("value", Long.valueOf(zkSyncedFollowers));
                                metrics.add(metric);
                            }
                        }
                        //等待同步的follower数
                        if (output.trim().contains("zk_pending_syncs")) {
                            String zkPendingSyncs = output.trim().replace("zk_pending_syncs", "").trim();
                            if (NumberUtils.isDigits(zkPendingSyncs)) {
                                Map<String, Object> metric = new HashMap<>();
                                metric.put("name", "zk_pending_syncs");
                                metric.put("timestamp", timestamp);
                                metric.put("value", Long.valueOf(zkPendingSyncs));
                                metrics.add(metric);
                            }
                        }
                        //节点数
                        if (output.trim().contains("zk_znode_count")) {
                            String zkZnodeCount = output.trim().replace("zk_znode_count", "").trim();
                            if (NumberUtils.isDigits(zkZnodeCount)) {
                                Map<String, Object> metric = new HashMap<>();
                                metric.put("name", "zk_znode_count");
                                metric.put("timestamp", timestamp);
                                metric.put("value", Long.valueOf(zkZnodeCount));
                                metrics.add(metric);
                            }
                        }
                        //watches数
                        if (output.trim().contains("zk_watch_count")) {
                            String zkWatchCount = output.trim().replace("zk_watch_count", "").trim();
                            if (NumberUtils.isDigits(zkWatchCount)) {
                                Map<String, Object> metric = new HashMap<>();
                                metric.put("name", "zk_watch_count");
                                metric.put("timestamp", timestamp);
                                metric.put("value", Long.valueOf(zkWatchCount));
                                metrics.add(metric);
                            }
                        }
                        //响应客户端请求的平均时间
                        if (output.trim().contains("zk_avg_latency")) {
                            String zkAvgLatency = output.trim().replace("zk_avg_latency", "").trim();
                            if (NumberUtils.isDigits(zkAvgLatency)) {
                                Map<String, Object> metric = new HashMap<>();
                                metric.put("name", "zk_avg_latency");
                                metric.put("timestamp", timestamp);
                                metric.put("value", Long.valueOf(zkAvgLatency));
                                metrics.add(metric);
                            }
                        }
                        //响应客户端请求的最小时间
                        if (output.trim().contains("zk_min_latency")) {
                            String zkMinLatency = output.trim().replace("zk_min_latency", "").trim();
                            if (NumberUtils.isDigits(zkMinLatency)) {
                                Map<String, Object> metric = new HashMap<>();
                                metric.put("name", "zk_min_latency");
                                metric.put("timestamp", timestamp);
                                metric.put("value", Long.valueOf(zkMinLatency));
                                metrics.add(metric);
                            }
                        }
                        //响应客户端请求的最大时间
                        if (output.trim().contains("zk_max_latency")) {
                            String zkMaxLatency = output.trim().replace("zk_max_latency", "").trim();
                            if (NumberUtils.isDigits(zkMaxLatency)) {
                                Map<String, Object> metric = new HashMap<>();
                                metric.put("name", "zk_max_latency");
                                metric.put("timestamp", timestamp);
                                metric.put("value", Long.valueOf(zkMaxLatency));
                                metrics.add(metric);
                            }
                        }
                        //接收的数据包数量
                        if (output.trim().contains("zk_packets_received")) {
                            String zkPacketsReceived = output.trim().replace("zk_packets_received", "").trim();
                            if (NumberUtils.isDigits(zkPacketsReceived)) {
                                Map<String, Object> metric = new HashMap<>();
                                metric.put("name", "zk_packets_received");
                                metric.put("timestamp", timestamp);
                                metric.put("value", Long.valueOf(zkPacketsReceived));
                                metrics.add(metric);
                            }
                        }
                        //发送的数据包数量
                        if (output.trim().contains("zk_packets_sent")) {
                            String zkPacketsSent = output.trim().replace("zk_packets_sent", "").trim();
                            if (NumberUtils.isDigits(zkPacketsSent)) {
                                Map<String, Object> metric = new HashMap<>();
                                metric.put("name", "zk_packets_sent");
                                metric.put("timestamp", timestamp);
                                metric.put("value", Long.valueOf(zkPacketsSent));
                                metrics.add(metric);
                            }
                        }
                        //排队请求数
                        if (output.trim().contains("zk_outstanding_requests")) {
                            String zkOutstandingRequests = output.trim().replace("zk_outstanding_requests", "").trim();
                            if (NumberUtils.isDigits(zkOutstandingRequests)) {
                                Map<String, Object> metric = new HashMap<>();
                                metric.put("name", "zk_outstanding_requests");
                                metric.put("timestamp", timestamp);
                                metric.put("value", Long.valueOf(zkOutstandingRequests));
                                metrics.add(metric);
                            }
                        }
                        //客户端连接总数
                        if (output.trim().contains("zk_num_alive_connections")) {
                            String zkNumAliveConnections = output.trim().replace("zk_num_alive_connections", "").trim();
                            if (NumberUtils.isDigits(zkNumAliveConnections)) {
                                Map<String, Object> metric = new HashMap<>();
                                metric.put("name", "zk_num_alive_connections");
                                metric.put("timestamp", timestamp);
                                metric.put("value", Long.valueOf(zkNumAliveConnections));
                                metrics.add(metric);
                            }
                        }
                        //文件句柄数
                        if (output.trim().contains("zk_open_file_descriptor_count")) {
                            String zkOpenFileDescriptorCount = output.trim().replace("zk_open_file_descriptor_count", "").trim();
                            if (NumberUtils.isDigits(zkOpenFileDescriptorCount)) {
                                Map<String, Object> metric = new HashMap<>();
                                metric.put("name", "zk_open_file_descriptor_count");
                                metric.put("timestamp", timestamp);
                                metric.put("value", Long.valueOf(zkOpenFileDescriptorCount));
                                metrics.add(metric);
                            }
                        }
                        //文件句柄上限
                        if (output.trim().contains("zk_max_file_descriptor_count")) {
                            String zkMaxFileDescriptorCount = output.trim().replace("zk_max_file_descriptor_count", "").trim();
                            if (NumberUtils.isDigits(zkMaxFileDescriptorCount)) {
                                Map<String, Object> metric = new HashMap<>();
                                metric.put("name", "zk_max_file_descriptor_count");
                                metric.put("timestamp", timestamp);
                                metric.put("value", Long.valueOf(zkMaxFileDescriptorCount));
                                metrics.add(metric);
                            }
                        }
                    }
                    params.put("metrics", metrics);
                    //如果5秒未处理
                    boolean waitFor = process.waitFor(5, TimeUnit.SECONDS);
                    if (!waitFor) {
                        LOGGER.info("waitFor = {}", waitFor);
                    }
                    HTTP_POOL_CLIENT.postJson(monitorUrl, JSON.toJSONString(params));
                } catch (IOException e) {
                    LOGGER.error("getMntr read error :", e);
                } catch (InterruptedException e) {
                    LOGGER.error("getMntr interrupted error :", e);
                } catch (RuntimeException e) {
                    LOGGER.error("getMntr runtime error :", e);
                } finally {
                    try {
                        inputReader.close();
                    } catch (IOException e) {
                        LOGGER.error("br close error ", e);
                    }
                }
            }
        }
        LOGGER.info("get mntr execute time = {}", System.currentTimeMillis() - start);
    }

    /**
     * 组装参数
     *
     * @param fourLetterWords
     * @param zkInfo
     * @return
     */
    private List<String> assemblyCmd(String fourLetterWords, String[] zkInfo) {
        List<String> cmd = new ArrayList<>();
        cmd.add(BASH);
        cmd.add(C);
        cmd.add(ECHO + " " + fourLetterWords + SEPARATE + NC + " " + zkInfo[0] + " " + zkInfo[1]);
        return cmd;
    }

    @Override
    public void execute(ShardingContext shardingContext) {
        if (zkMonitorSwitch) {
            getMntr(FourLetterWords.MNTR);
        }
    }

//    @Scheduled(cron = "0 0/1 * * * ?")
//    public void execute() {
//        if (zkMonitorSwitch && isExecute) {
//            //TODO 非分布式
//            getMntr(FourLetterWords.MNTR);
//        }
//    }
}
