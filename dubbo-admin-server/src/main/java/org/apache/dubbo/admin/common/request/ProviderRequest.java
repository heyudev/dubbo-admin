package org.apache.dubbo.admin.common.request;

import lombok.Data;

import java.util.List;

/**
 * @author heyudev
 * @date 2019/07/03
 */
@Data
public class ProviderRequest {
    /**
     * 服务名 demo*com.imooc.springboot.dubbo.demo.DemoService:1.0.0
     */
    private String service;
    /**
     * 注册中心地址：10.2.39.11:2181 或 10.2.39.11:2181,10.2.39.12:2181,10.2.39.13:2181
     */
    private String registryAddress;
    /**
     * service(provider或consumer)的id
     */
    private List<String> ids;
    /**
     * 操作类型 1 启用 2 禁用
     */
    private Integer operation;
}
