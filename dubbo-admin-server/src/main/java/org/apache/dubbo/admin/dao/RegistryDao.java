package org.apache.dubbo.admin.dao;

import org.apache.dubbo.admin.model.domain.Registry;

import java.util.List;

/**
 * @author heyudev
 * @date 2019/06/05
 */
public interface RegistryDao {
    /**
     * 获取所有registry
     *
     * @return
     */
    List<Registry> getAllRegistry();

    /**
     * 
     * @return
     */
    List<Registry> getRegistryList();

    /**
     * 获取所有自动注册的registry
     *
     * @return
     */
    List<Registry> getAllRegistryOfAuto();

    /**
     * 根据环境获取所有registry
     *
     * @return
     */
    List<Registry> getAllRegistryByEnv(Integer env);

    /**
     * 查询所有需要监控的注册中心
     *
     * @return
     */
    List<Registry> getAllRegistryOfMonitor();

    /**
     * 根据环境获取所有自动注册的registry
     *
     * @return
     */
    List<Registry> getAllRegistryOfAutoByEnv(Integer env);

    /**
     * 查询
     *
     * @param registry
     * @return
     */
    Registry getRegistry(Registry registry);

    /**
     * 根据注册中心查询配置
     *
     * @param regAddress
     * @return
     */
    Registry getRegistryByAddress(String regAddress);

    /**
     * @param registry
     * @return
     */
    Integer insert(Registry registry);

    /**
     * @param registry
     * @return
     */
    Integer update(Registry registry);

    /**
     * 删除
     *
     * @param registry
     * @return
     */
    Integer delete(Registry registry);

    /**
     * 根据appCode更新
     *
     * @param registry
     * @return
     */
    Integer updateByAppCode(Registry registry);
}
