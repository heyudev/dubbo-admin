package org.apache.dubbo.admin.service;

import org.apache.dubbo.admin.model.domain.Application;

import java.util.List;

/**
 * @author heyudev
 * @date 2019/06/04
 */
public interface ApplicationService {
    /**
     * 获取所有应用列表
     * @param registry
     * @return
     */
    List<Application> getApplications(String registry);
}
