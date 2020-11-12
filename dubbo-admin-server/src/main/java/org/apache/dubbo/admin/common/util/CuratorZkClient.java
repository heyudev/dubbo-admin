package org.apache.dubbo.admin.common.util;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.BackgroundCallback;
import org.apache.curator.framework.api.CuratorListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.dubbo.admin.model.domain.Node;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author heyudev
 * @date 2019/06/03
 */
public class CuratorZkClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CuratorZkClient.class);

    private final CuratorFramework curator;
    private final AtomicReference<CuratorListener> listener = new AtomicReference<>(null);

    public static CuratorFramework create(String connectionString) {
        // these are reasonable arguments for the ExponentialBackoffRetry. The first
        // retry will wait 1 second - the second will wait up to 2 seconds - the
        // third will wait up to 4 seconds.
        ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3);

        // The simplest way to get a CuratorFramework instance. This will use default values.
        // The only required arguments are the connection string and the retry policy
        return CuratorFrameworkFactory.newClient(connectionString, retryPolicy);
    }

    public static CuratorFramework createWithOptions(String connectionString, RetryPolicy retryPolicy, int connectionTimeoutMs, int sessionTimeoutMs) {
        // using the CuratorFrameworkFactory.builder() gives fine grained control
        // over creation options. See the CuratorFrameworkFactory.Builder javadoc
        // details
        return CuratorFrameworkFactory.builder()
                .connectString(connectionString)
                .retryPolicy(retryPolicy)
                .connectionTimeoutMs(connectionTimeoutMs)
                .sessionTimeoutMs(sessionTimeoutMs)
                // etc. etc.
                .build();
    }


    /**
     * @param curator Curator instance to bridge
     */
    public CuratorZkClient(CuratorFramework curator) {
        this.curator = curator;
    }

    /**
     * Return the client
     *
     * @return client
     */
    public CuratorFramework getCurator() {
        return curator;
    }

    public void connect(final Watcher watcher) {
        if (watcher != null) {
            CuratorListener localListener = (client, event) -> {
                if (event.getWatchedEvent() != null) {
                    watcher.process(event.getWatchedEvent());
                }
            };
            curator.getCuratorListenable().addListener(localListener);
            listener.set(localListener);

            try {
                BackgroundCallback callback = (client, event) -> {
                    WatchedEvent fakeEvent = new WatchedEvent(Watcher.Event.EventType.None, curator.getZookeeperClient().isConnected() ? Watcher.Event.KeeperState.SyncConnected : Watcher.Event.KeeperState.Disconnected, null);
                    watcher.process(fakeEvent);
                };
                curator.checkExists().inBackground(callback).forPath("/foo");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void close() {
        // NOTE: the curator instance is NOT closed here
        CuratorListener localListener = listener.getAndSet(null);
        if (localListener != null) {
            curator.getCuratorListenable().removeListener(localListener);
        }
    }

    public List<Node> getChildren(String parent) {
        List<Node> children = new ArrayList<>();
        try {
            List<String> list = getChildren(parent, false);
            for (String name : list) {
                String path = parent.equals("/") ? parent + name : parent + "/" + name;
                Node node = new Node();
                String decodeName = null;
                try {
                    decodeName = URLDecoder.decode(name, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if (decodeName != null) {
                    node.setName(decodeName);
                } else {
                    node.setName(name);
                }
                node.setParent(parent);
                node.setNodeStat(getStat(path));
                children.add(node);
            }
            return children;
        } catch (KeeperException e) {
            LOGGER.error("getChildren error", e);
            throw new RuntimeException("parent = " + parent + ";" + e.getMessage());
        } catch (InterruptedException e) {
            LOGGER.error("getChildren error", e);
            throw new RuntimeException("parent = " + parent + ";" + e.getMessage());
        }
    }

    public String create(String path, byte[] data, CreateMode mode) throws KeeperException, InterruptedException {
        try {
            return curator.create().withMode(mode).forPath(path, data);
        } catch (Exception e) {
            adjustException(e);
        }
        // will never execute
        return null;
    }

    public void delete(String path) throws InterruptedException, KeeperException {
        try {
            curator.delete().forPath(path);
        } catch (Exception e) {
            adjustException(e);
        }
    }

    public boolean exists(String path, boolean watch) throws KeeperException, InterruptedException {
        try {
            return watch ? (curator.checkExists().watched().forPath(path) != null) : (curator.checkExists().forPath(path) != null);
        } catch (Exception e) {
            adjustException(e);
        }
        // will never execute
        return false;
    }

    public List<String> getChildren(String path, boolean watch) throws KeeperException, InterruptedException {
        try {
            return watch ? curator.getChildren().watched().forPath(path) : curator.getChildren().forPath(path);
        } catch (Exception e) {
            adjustException(e);
        }
        // will never execute
        return null;
    }

    public byte[] readData(String path, Stat stat, boolean watch) throws KeeperException, InterruptedException {
        try {
            if (stat != null) {
                return watch ? curator.getData().storingStatIn(stat).watched().forPath(path) : curator.getData().storingStatIn(stat).forPath(path);
            } else {
                return watch ? curator.getData().watched().forPath(path) : curator.getData().forPath(path);
            }
        } catch (Exception e) {
            adjustException(e);
        }
        // will never execute
        return null;
    }

    public void writeData(String path, byte[] data, int expectedVersion) throws KeeperException, InterruptedException {
        writeDataReturnStat(path, data, expectedVersion);
    }

    public Stat writeDataReturnStat(String path, byte[] data, int expectedVersion) throws KeeperException, InterruptedException {
        try {
            curator.setData().withVersion(expectedVersion).forPath(path, data);
        } catch (Exception e) {
            adjustException(e);
        }
        // will never execute
        return null;
    }

    public ZooKeeper.States getZookeeperState() {
        try {
            return curator.getZookeeperClient().getZooKeeper().getState();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public long getCreateTime(String path) throws KeeperException, InterruptedException {
        try {
            Stat stat = curator.checkExists().forPath(path);
            return (stat != null) ? stat.getCtime() : 0;
        } catch (Exception e) {
            adjustException(e);
        }
        return 0;
    }

    public Stat getStat(String path) {
        try {
            if (exists(path, false)) {
                Stat stat = curator.checkExists().forPath(path);
                return stat;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getServers() {
        return curator.getZookeeperClient().getCurrentConnectionString();
    }

    private void adjustException(Exception e) throws KeeperException, InterruptedException {
        if (e instanceof KeeperException) {
            throw (KeeperException) e;
        }

        if (e instanceof InterruptedException) {
            throw (InterruptedException) e;
        }
        throw new RuntimeException(e);
    }
}