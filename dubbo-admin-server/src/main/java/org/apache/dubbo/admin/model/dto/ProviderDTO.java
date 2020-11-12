package org.apache.dubbo.admin.model.dto;

import org.apache.dubbo.admin.model.domain.Provider;

/**
 * @author heyudev
 * @date 2019/05/20
 */
public class ProviderDTO extends Provider {

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
     * 重试次数
     */
    private String retries;

    private String revision;

    public String getRetries() {
        return retries;
    }

    public void setRetries(String retries) {
        this.retries = retries;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
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
