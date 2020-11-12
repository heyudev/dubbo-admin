package org.apache.dubbo.admin.common;

/**
 * @author heyudev
 * @date 2019/07/15
 */
public enum OperationEnum {
    ENABLE(1, "启用"),
    DISABLE(2, "禁用");

    int value;
    String desc;

    public int getValue() {
        return value;
    }

    public String getDesc() {
        return desc;
    }

    OperationEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
