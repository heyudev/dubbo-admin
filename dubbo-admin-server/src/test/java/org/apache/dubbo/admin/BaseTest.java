package org.apache.dubbo.admin;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author heyudev
 * @date 2019/10/08
 */
@ActiveProfiles("dev")
@RunWith(value = SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = DubboAdminApplication.class)
public class BaseTest {
}
