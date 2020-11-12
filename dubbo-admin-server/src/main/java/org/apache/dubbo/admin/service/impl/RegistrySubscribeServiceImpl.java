package org.apache.dubbo.admin.service.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.dubbo.admin.common.util.Constants;
import org.apache.dubbo.admin.config.ConfigCenter;
import org.apache.dubbo.admin.dao.RegistryDao;
import org.apache.dubbo.admin.model.domain.Registry;
import org.apache.dubbo.admin.service.RegistryServerSync;
import org.apache.dubbo.admin.service.RegistrySubscribeService;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.registry.RegistryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.*;

/**
 * @author heyudev
 * @date 2019/06/06
 */
@Service
public class RegistrySubscribeServiceImpl implements RegistrySubscribeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrySubscribeServiceImpl.class);

    @Resource
    private RegistryDao registryDao;

    @Resource
    private RegistryServerSync sync;

    private static final RegistryFactory REGISTRY_FACTORY = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();

    ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("registry-pool-%d").build();
    ExecutorService executor = new ThreadPoolExecutor(10, 10,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());

    @Override
    public boolean addAndSubscribe(Registry registry) {
        int flag = registryDao.insert(registry);
        if (flag > 0) {
            subscribe(registry);
            return true;
        }
        return false;
    }

    @Override
    public void subscribe(Registry registry) {
        executor.execute(() -> {
            URL registryUrl = ConfigCenter.formUrl(Constants.REGISTRY_ZOOKEEPER_PREFIX + registry.getRegAddress(), registry.getRegGroup(), registry.getUsername(), registry.getPassword());
            try {
                sync.subscribe(REGISTRY_FACTORY.getRegistry(registryUrl));
            } catch (Exception e) {
                LOGGER.error("subscribe error ", e);
            }
        });
    }

    @Override
    public void unSubscribe(Registry registry) {
        URL registryUrl = ConfigCenter.formUrl(Constants.REGISTRY_ZOOKEEPER_PREFIX + registry.getRegAddress(), registry.getRegGroup(), registry.getUsername(), registry.getPassword());
        try {
            sync.unSubscribe(REGISTRY_FACTORY.getRegistry(registryUrl));
        } catch (Exception e) {
            LOGGER.error("subscribe error ", e);
        }
    }
}
