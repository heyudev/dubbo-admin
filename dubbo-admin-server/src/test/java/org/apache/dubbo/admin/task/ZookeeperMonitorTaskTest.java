package org.apache.dubbo.admin.task;

import org.apache.dubbo.admin.AbstractSpringIntegrationTest;
import org.apache.dubbo.admin.DubboAdminApplication;
import org.apache.dubbo.admin.model.domain.Registry;
import org.apache.dubbo.admin.service.RegistryService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author heyudev
 * @date 2019/08/22
 */
//@RunWith(SpringJUnit4ClassRunner.class)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = DubboAdminApplication.class)
@TestPropertySource(locations = {
        "classpath:application.yml"})
public class ZookeeperMonitorTaskTest extends AbstractSpringIntegrationTest {

    @Resource
    private RegistryService registryService;

    @Test
    public void getzk() {
        List<Registry> list = registryService.getAllRegistryOfMonitor();
        System.out.println(list);
    }
}
