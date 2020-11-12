package org.apache.dubbo.admin.common.util;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.dubbo.admin.common.exception.HttpServiceException;
import org.apache.dubbo.admin.common.exception.TimeoutException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.net.ssl.SSLContext;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * http client
 * @author heyudev
 */
public class HttpPoolClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpPoolClient.class);

    private static final ScheduledExecutorService SCHEDULED_CLOSED_EXECUTOR = new ScheduledThreadPoolExecutor(1,
            new BasicThreadFactory.Builder().namingPattern("http conn-closed-thread-%s").priority(Thread.NORM_PRIORITY).daemon(false).build(), (r, e) -> LOGGER.error(" monitor push reject task error={}", e.toString()));

    private static final List<HttpClientConnectionManager> HTTP_CLIENT_CONNECTION_MANAGERS = Lists.newArrayList();

    static {
        SCHEDULED_CLOSED_EXECUTOR.schedule(() -> HTTP_CLIENT_CONNECTION_MANAGERS.forEach(HttpClientConnectionManager::closeExpiredConnections), 5, TimeUnit.SECONDS);
    }

    private CloseableHttpClient closeableHttpClient;

    private HttpPoolClient(CloseableHttpClient closeableHttpClient) {
        this.closeableHttpClient = closeableHttpClient;
    }

    private static HttpRequestInterceptor getInterceptor() {
        HttpRequestInterceptor requestInterceptor = (request, context) -> {
            try {
                String missSpanId = MDC.get("missSpanId");
                String missTraceId = MDC.get("request-id");
                if (missTraceId != null && !"".equals(missTraceId.trim())) {
                    request.setHeader("request-id", missTraceId);
                }
                if (missSpanId != null && !"".equals(missSpanId.trim())) {
                    request.setHeader("missSpanId", missSpanId);
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        };
        return requestInterceptor;
    }


    public Optional<String> get(String url) {
        HttpGet httpGet = new HttpGet(url);
        return request(httpGet);
    }

    public Optional<String> post(String url) {
        HttpPost httpPost = new HttpPost(url);
        return request(httpPost);
    }


    public Optional<String> postJson(String url, String json) {
        HttpPost httpPost = new HttpPost(url);
        if (StringUtils.isBlank(json)) {
            return request(httpPost);
        }
        StringEntity entity = new StringEntity(json, Charset.forName("UTF-8"));
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        return request(httpPost);
    }

    public Optional<String> request(HttpRequestBase request) {

        if (LOGGER.isDebugEnabled()) {
            String path = request.getURI().toString();
            LOGGER.debug("http request url = {} ", path);
        }
        HttpEntity entity = null;
        try {
            CloseableHttpResponse response = request((HttpUriRequest) request);
            if (response == null) {
                throw new RuntimeException("call api exception no response");
            }
            entity = response.getEntity();
            String content = null;
            if (entity != null) {
                content = EntityUtils.toString(entity, "UTF-8");
            }
            int httpStatus = response.getStatusLine().getStatusCode();
            if (httpStatus == HttpStatus.SC_OK) {
                return Optional.ofNullable(content);
            }
            String path = request.getURI().toString();
            LOGGER.error("http call api {} fail response status {} content {}", path, httpStatus, content);
            throw new HttpServiceException(httpStatus, content);
        } catch (Exception e) {
            if (e instanceof TimeoutException) {
                throw (TimeoutException) e;
            }
            if (e instanceof HttpServiceException) {
                throw (HttpServiceException) e;
            }
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            if (request != null) {
                request.abort();
            }
            EntityUtils.consumeQuietly(entity);
        }
    }


    public CloseableHttpResponse request(HttpUriRequest request) {
        try {
            CloseableHttpResponse execute = closeableHttpClient.execute(request);
            return execute;
        } catch (Exception e) {
            String path = request.getURI().toString();
            if (e instanceof SocketTimeoutException) {
                LOGGER.error(String.format("http timeout request url = %s .", path));
                throw new TimeoutException();
            } else {
            }
            throw new RuntimeException(String.format("http exception request url = %s ", path), e);
        }
    }

    /**
     * @param connectTimeout 连接超时时间 ms
     * @param socketTimeout  读超时时间（等待数据超时时间）ms
     * @param maxPerRoute    每个路由的最大连接数
     * @param maxTotal       最大连接数
     * @param retryCount     重试次数
     * @return httpclient instance
     */
    protected static HttpPoolClient create(int connectTimeout, int socketTimeout, int maxPerRoute, int maxTotal, int retryCount, int connectionWaitTimeout) {
        try {
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(connectTimeout).setSocketTimeout(socketTimeout).setConnectionRequestTimeout(connectionWaitTimeout).build();
            CloseableHttpClient client = HttpClientBuilder.create()
                    .setDefaultRequestConfig(requestConfig)
                    .setConnectionManager(createConnectionManager(maxPerRoute, maxTotal))
                    .setRetryHandler(new DefaultHttpRequestRetryHandler(retryCount, false)).addInterceptorFirst(getInterceptor()).build();
            return new HttpPoolClient(client);
        } catch (Throwable e) {
            LOGGER.error("create HttpPoolClient exception", e);
            throw new RuntimeException("create HttpPoolClient exception");
        }
    }

    private static PoolingHttpClientConnectionManager createConnectionManager(int maxPerRoute, int maxTotal) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        SSLContext sslContext = SSLContexts.custom().loadTrustMaterial((chain, authType) -> true).build();
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE)).build();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
        cm.setDefaultMaxPerRoute(maxPerRoute);
        cm.setMaxTotal(maxTotal);
        HTTP_CLIENT_CONNECTION_MANAGERS.add(cm);
        return cm;
    }

}
