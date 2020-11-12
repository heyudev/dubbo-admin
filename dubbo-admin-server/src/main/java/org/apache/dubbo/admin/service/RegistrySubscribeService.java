package org.apache.dubbo.admin.service;

import org.apache.dubbo.admin.model.domain.Registry;

/**
 * @author heyudev
 * @date 2019/06/06
 */
public interface RegistrySubscribeService {
    /**
     * 插入并订阅注册中心数据
     *
     * @param registry
     * @return
     */
    boolean addAndSubscribe(Registry registry);

    void subscribe(Registry registry);

    void unSubscribe(Registry registry);
}
