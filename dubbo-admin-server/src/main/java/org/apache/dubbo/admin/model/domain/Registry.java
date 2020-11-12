package org.apache.dubbo.admin.model.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * @author heyudev
 * @date 2019/06/05
 */
@Data
public class Registry implements Serializable {
    /**
     * 主键
     */
    private Integer id;
    /**
     * 名称
     */
    private String regName;
    /**
     * 地址
     * 10.2.39.29:2181
     * 10.2.39.29:2181,10.2.39.11:2181,10.2.39.12:2181
     */
    private String regAddress;
    /**
     * 组
     */
    private String regGroup;
    /**
     * 环境：1 测试 2 预发 3 生产
     */
    private Integer env;
    /**
     * 是否有效 ：0 无效  1有效
     */
    private Integer state;
    /**
     * 是否自动连接 0 否 1 是
     */
    private Integer auto;
    /**
     * 编码
     */
    private String appCode;
    /**
     * 是否需要监控 0 否 1 是
     */
    private Integer monitor;

    private String username;

    private String password;

}
