package org.apache.dubbo.admin.service;

import org.apache.dubbo.admin.model.dto.StatisticsDTO;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.monitor.MonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author heyudev
 * @date 2019/05/07
 */
@Component
public class DubboMonitorService implements MonitorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DubboMonitorService.class);

    public static final String HOST_KEY="host";

    public static final String REMOTE_ADDRESS="remoteAddress";

    public static final String REMOTE_TYPE="remoteType";

    public static final String APPLICATION_TYPE="applicationType";

    public static final String TPS="tps";

    public static final String KBPS="kbps";

    //TODO 存储到ES
//    private StatisticsStorage statisticsStorage;
//
//    public void setStatisticsStorage(StatisticsStorage statisticsStorage) {
//        this.statisticsStorage = statisticsStorage;
//    }


    @Override
    public void collect(URL statisticsURL) {
        LOGGER.info("statisticsURL info = ",statisticsURL);
        System.out.println("statisticsURL info = "+statisticsURL.toString());

        StatisticsDTO statistics = new StatisticsDTO();
        statistics.setTimestamp(System.currentTimeMillis());
        statistics.setApplication(statisticsURL.getParameter(MonitorService.APPLICATION));
        statistics.setConcurrent(
                Long.valueOf(statisticsURL.getParameter(MonitorService.CONCURRENT, 1)));
        if(statistics.getConcurrent()==0){
            statistics.setConcurrent(Long.valueOf(1));
        }
        statistics.setHost(statisticsURL.getHost());
        statistics.setServiceInterface(statisticsURL.getParameter(MonitorService.INTERFACE));
        statistics.setMethod(statisticsURL.getParameter(MonitorService.METHOD));

        int failureCount = statisticsURL.getParameter(MonitorService.FAILURE,0);
        int successCount = statisticsURL.getParameter(MonitorService.SUCCESS,0);
        statistics.setFailureCount(failureCount);
        statistics.setSuccessCount(successCount);
        int totalCount = failureCount+successCount;
        if(totalCount<=0){
            return;
        }
        statistics.setElapsed(
                Long.valueOf(statisticsURL.getParameter(MonitorService.ELAPSED, 0)/totalCount));
        statistics.setInput(
                Long.valueOf(statisticsURL.getParameter(MonitorService.INPUT,0)/totalCount));
        statistics.setOutput(
                Long.valueOf(statisticsURL.getParameter(MonitorService.OUTPUT,0)/totalCount));
        if(statistics.getElapsed()!=0){
            //TPS=并发数/响应时间
            BigDecimal tps = new BigDecimal(statistics.getConcurrent());
            tps=tps.divide(BigDecimal.valueOf(statistics.getElapsed()),2,BigDecimal.ROUND_HALF_DOWN);
            tps=tps.multiply(BigDecimal.valueOf(1000));
            statistics.setTps(tps.doubleValue());//每秒能够处理的请求数量
        }
        BigDecimal kbps = new BigDecimal(statistics.getTps());
        if(statistics.getInput()!=0&&statistics.getElapsed()!=0){
            //kbps=tps*平均每次传输的数据量
            kbps=kbps.multiply(BigDecimal.valueOf(statistics.getInput()).divide(BigDecimal.valueOf(1024),2,BigDecimal.ROUND_HALF_DOWN));
        }else if(statistics.getElapsed()!=0){
            kbps=kbps.multiply(BigDecimal.valueOf(statistics.getOutput()).divide(BigDecimal.valueOf(1024),2,BigDecimal.ROUND_HALF_DOWN));
        }
        statistics.setKbps(kbps.doubleValue());
        if(statisticsURL.hasParameter(MonitorService.PROVIDER)){
            statistics.setType(StatisticsDTO.ApplicationType.CONSUMER);
            statistics.setRemoteType(StatisticsDTO.ApplicationType.PROVIDER);
            statistics.setRemoteAddress(statisticsURL.getParameter(MonitorService.PROVIDER));
        }else{
            statistics.setType(StatisticsDTO.ApplicationType.PROVIDER);
            statistics.setRemoteType(StatisticsDTO.ApplicationType.CONSUMER);
            statistics.setRemoteAddress(statisticsURL.getParameter(MonitorService.CONSUMER));
        }
//        statisticsStorage.storeStatistics(statistics);
        LOGGER.info("statistics info = ",statistics.toString());
        System.out.println("statistics info = "+statistics.toString());
    }

    @Override
    public List<URL> lookup(URL query) {
        return null;
    }
}
