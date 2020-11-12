package org.apache.dubbo.admin.common;

/**
 * 环境
 *
 * @author heyudev
 * @date 2019/07/01
 */
public enum EnvEnum {
//    DEV(0, "开发环境"),
    BETA(1, "测试环境"),
    PRE(2, "预发环境"),
    RELEASE(3, "生产环境");

    int value;
    String desc;

    EnvEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public static int getValueByName(String name) {
        for (EnvEnum envEnum : EnvEnum.values()) {
            if (envEnum.name().equalsIgnoreCase(name)) {
                return envEnum.getValue();
            }
        }
        return -1;
    }

}
