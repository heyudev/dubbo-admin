package org.apache.dubbo.admin.model.domain;

import lombok.Data;

import java.util.Objects;

/**
 * @author heyudev
 * @date 2019/06/04
 */
@Data
public class Application {
    private String application;
    private String owner;
    private String registry;
    private String type;

    @java.lang.Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Application that = (Application) o;
        return application.equals(that.application) &&
                registry.equals(that.registry);
    }

    @java.lang.Override
    public int hashCode() {
        return Objects.hash(application, owner, registry, type);
    }
}
