package org.apache.dubbo.admin.service.impl;

import org.apache.dubbo.admin.dao.RegistryDao;
import org.apache.dubbo.admin.model.domain.Registry;
import org.apache.dubbo.admin.service.RegistryService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author heyudev
 * @date 2019/06/06
 */
@Service
public class RegistryServiceImpl implements RegistryService {

    @Resource
    private RegistryDao registryDao;

    @Override
    public boolean add(Registry registry) {
        int flag = registryDao.insert(registry);
        if (flag > 0) {
            return true;
        }
        return false;
    }

    @Override
    public List<Registry> getAllRegistry() {
        return registryDao.getAllRegistry();
    }

    @Override
    public List<Registry> getRegistryList() {
        return registryDao.getRegistryList();
    }

    @Override
    public List<Registry> getAllRegistryOfAuto() {
        return registryDao.getAllRegistryOfAuto();
    }

    @Override
    public List<Registry> getAllRegistryByEnv(Integer env) {
        return registryDao.getAllRegistryByEnv(env);
    }

    @Override
    public List<Registry> getAllRegistryOfMonitor() {
        return registryDao.getAllRegistryOfMonitor();
    }

    @Override
    public List<Registry> getAllRegistryOfAutoByEnv(Integer env) {
        return registryDao.getAllRegistryOfAutoByEnv(env);
    }

    @Override
    public Registry getRegistry(Registry registry) {
        return registryDao.getRegistry(registry);
    }

    @Override
    public Registry getRegistryByAddress(String regAddress) {
        return registryDao.getRegistryByAddress(regAddress);
    }

    @Override
    public Integer update(Registry registry) {
        return registryDao.update(registry);
    }

    @Override
    public Integer delete(Registry registry) {
        return registryDao.delete(registry);
    }

    @Override
    public Integer updateByAppCode(Registry registry) {
        return registryDao.updateByAppCode(registry);
    }

}
