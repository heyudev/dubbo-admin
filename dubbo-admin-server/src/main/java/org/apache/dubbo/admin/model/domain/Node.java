package org.apache.dubbo.admin.model.domain;

import lombok.Data;
import org.apache.zookeeper.data.Stat;
import java.util.List;

/**
 * @author heyudev
 * @date 2019/06/03
 */
@Data
public class Node {
    /**
     * 父节点
     */
    private String parent;
    /**
     * 节点名称
     */
    private String name;
    /**
     * 子节点
     */
    private List<Node> children;
    /**
     * 状态
     */
    private Stat nodeStat;
}
