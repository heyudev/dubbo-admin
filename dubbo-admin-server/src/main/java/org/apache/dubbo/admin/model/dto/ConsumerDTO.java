package org.apache.dubbo.admin.model.dto;

import org.apache.dubbo.admin.model.domain.Consumer;

/**
 * @author heyudev
 * @date 2019/05/20
 */
public class ConsumerDTO extends Consumer {
    /**
     * 全路径
     */
    private String fullUrl;
    /**
     * 应用名
     */
    private String appName;
    /**
     * PID
     */
    private String pid;
    /**
     * Dubbo 版本
     */
    private String dubbo;
    /**
     * 方法列表
     */
    private String methods;
    /**
     * 组
     */
    private String group;
    /**
     * 版本
     */
    private String version;
    /**
     * 所属端
     */
    private String side;
    /**
     * 时间戳
     */
    private String timestamp;
    /**
     * 检查
     */
    private boolean check;

    public boolean isCheck() {
        return check;
    }

    public void setCheck(boolean check) {
        this.check = check;
    }

    public String getFullUrl() {
        return fullUrl;
    }

    public void setFullUrl(String fullUrl) {
        this.fullUrl = fullUrl;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getDubbo() {
        return dubbo;
    }

    public void setDubbo(String dubbo) {
        this.dubbo = dubbo;
    }

    public String getMethods() {
        return methods;
    }

    public void setMethods(String methods) {
        this.methods = methods;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
