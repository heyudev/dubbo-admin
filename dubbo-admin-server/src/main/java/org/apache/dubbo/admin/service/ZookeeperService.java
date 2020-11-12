package org.apache.dubbo.admin.service;

import org.apache.dubbo.admin.model.domain.Node;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * @author heyudev
 * @date 2019/05/31
 */
public interface ZookeeperService {

    /**
     * 获取zookeeper信息
     *
     * @param address 集群地址
     * @return
     */
    ConcurrentMap<String, ConcurrentMap<String, List<String>>> getZookeeperInfo(String address);

    /**
     * 获取zookeeper 子节点
     *
     * @param address
     * @param parent
     * @return
     */
    List<Node> getChildren(String address, String parent);

    /**
     * 获取zookeeper 节点数据
     *
     * @param address
     * @param path
     * @return
     */
    String getData(String address, String path);

    /**
     * 服务依赖关系
     *
     * @param address
     * @return
     */
    Map<String, Object> appsDependencies(String address);
}
