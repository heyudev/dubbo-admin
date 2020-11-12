package org.apache.dubbo.admin.controller;

import org.apache.dubbo.admin.BaseTest;
import org.apache.dubbo.admin.model.domain.Registry;
import org.apache.dubbo.admin.service.RegistryService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author heyudev
 * @date 2019/10/08
 */
public class RegistryControllerTest extends BaseTest {

    @Autowired
    private RegistryService registryService;

    @Test
    public void getAllRegistryTest(){
        List<Registry> registryList = registryService.getAllRegistry();
        System.out.println(registryList);

        List<Registry> registryList2 = registryService.getAllRegistryByEnv(1);
        System.out.println(registryList2);
    }
}
