package org.apache.dubbo.admin.common.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * http util
 *
 * @author heyudev
 */
public class HttpClientUtil {

    private static final String DEFAULT_SHARED_KEY = "DEFAULT_SHARED_KEY";

    /**
     * 链接建立的超时时间 ms
     */
    private static final int DEFAULT_CONNECTION_TIMEOUT = 3000;
    /**
     * 响应超时时间 ms
     */
    private static final int DEFAULT_SOCKET_TIMEOUT = 3000;

    /**
     * 每个路由的最大连接数
     */
    private static final int DEFAULT_DEFAULT_MAX_PER_ROUTE = 50;

    /**
     * 最大连接数
     */
    private static final int DEFAULT_DEFAULT_MAX_TOTAL = 200;

    /**
     * 重试次数，默认0
     */
    private static final int DEFAULT_RETRY_COUNT = 0;

    /**
     * 从connection pool中获得一个connection的超时时间 ms
     */
    private static final int DEFAULT_CONNECTION_WAIT_TIMEOUT = 300;

    private static final Map<String, HttpPoolClient> CREATED_HTTP_CLIENTS = new HashMap<>();

    private static final Lock LOCK = new ReentrantLock();

    private HttpClientUtil() {
    }

    public static HttpPoolClient useDefault() {
        return createCached(DEFAULT_SHARED_KEY);
    }


    public static HttpPoolClient createCached(String cachedKey) {
        HttpPoolClient httpPoolClient = CREATED_HTTP_CLIENTS.get(cachedKey);
        if (httpPoolClient != null) {
            return httpPoolClient;
        }
        LOCK.lock();
        try {
            httpPoolClient = CREATED_HTTP_CLIENTS.get(cachedKey);
            if (httpPoolClient == null) {
                httpPoolClient = create(DEFAULT_CONNECTION_TIMEOUT, DEFAULT_SOCKET_TIMEOUT, DEFAULT_DEFAULT_MAX_PER_ROUTE, DEFAULT_DEFAULT_MAX_TOTAL, DEFAULT_RETRY_COUNT, DEFAULT_CONNECTION_WAIT_TIMEOUT);
                CREATED_HTTP_CLIENTS.put(cachedKey, httpPoolClient);
            }
        } finally {
            LOCK.unlock();
        }
        return httpPoolClient;
    }

    /**
     * 创建httpclient
     *
     * @param connectTimeout        连接超时时间 ms
     * @param socketTimeout         读超时时间（等待数据超时时间）ms
     * @param maxPerRoute           每个路由的最大连接数
     * @param maxTotal              最大连接数
     * @param retryCount            重试次数
     * @param connectionWaitTimeout 连接等待超市时间 ms
     * @return httpclient instance
     */
    public static HttpPoolClient create(int connectTimeout, int socketTimeout, int maxPerRoute, int maxTotal, int retryCount, int connectionWaitTimeout) {
        return HttpPoolClient.create(connectTimeout, socketTimeout, maxPerRoute, maxTotal, retryCount, connectionWaitTimeout);
    }
}
