package org.apache.dubbo.admin.model.domain;

import lombok.Data;

import java.util.Objects;

/**
 * @author heyudev
 * @date 2019/06/04
 */
@Data
public class Line {
    private String name;
    private String source;
    private String target;

    @java.lang.Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Line line = (Line) o;
        return source.equals(line.source) &&
                target.equals(line.target);
    }

    @java.lang.Override
    public int hashCode() {
        return Objects.hash(name, source, target);
    }
}
