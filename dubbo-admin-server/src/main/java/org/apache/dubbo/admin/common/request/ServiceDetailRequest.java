package org.apache.dubbo.admin.common.request;

import lombok.Data;

/**
 * @author heyudev
 * @date 2019/07/03
 */
@Data
public class ServiceDetailRequest {
    /**
     * 服务名 com.imooc.springboot.dubbo.demo.DemoService
     */
    private String service;
    /**
     * 注册中心地址：10.2.39.11:2181 或 10.2.39.11:2181,10.2.39.12:2181,10.2.39.13:2181
     */
    private String registryAddress;
    /**
     * service(provider或consumer)的id
     */
    private String id;
    /**
     * 权重
     */
    private Integer weight;
}
