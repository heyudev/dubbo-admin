package org.apache.dubbo.admin.service.impl;

import org.apache.dubbo.admin.common.util.Constants;
import org.apache.dubbo.admin.model.domain.Application;
import org.apache.dubbo.admin.service.ApplicationService;
import org.apache.dubbo.admin.service.MonitorService;
import org.apache.dubbo.common.URL;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * @author heyudev
 * @date 2019/06/04
 */
@Service
public class ApplicationServiceImpl extends AbstractService implements ApplicationService {

    private static final String PROVIDER = "Provider";
    private static final String CONSUMER = "Consumer";
    private static final String PROVIDER_AND_CONSUMER = "Provider&Consumer";

    @Override
    public List<Application> getApplications(String registryAddress) {
        List<Application> applications = new ArrayList<>();
        ConcurrentMap<String, Map<String, URL>> providerUrls = getSingleRegistryCache(registryAddress).get(Constants.PROVIDERS_CATEGORY);
        if (providerUrls != null) {
            for (Map.Entry<String, Map<String, URL>> serviceEntry : providerUrls.entrySet()) {
                for (Map.Entry<String, URL> urlEntry : serviceEntry.getValue().entrySet()) {
                    URL u = urlEntry.getValue();
                    String app = u.getParameter(Constants.APPLICATION);
                    if (app != null) {
                        Application application = new Application();
                        application.setApplication(app);
                        application.setOwner(u.getParameter("owner"));
                        application.setRegistry(registryAddress);
                        application.setType(PROVIDER);
                        if (!applications.contains(application)) {
                            applications.add(application);
                        }
                    }
                }
            }
        }

        ConcurrentMap<String, Map<String, URL>> consumers = getSingleRegistryCache(registryAddress).get(Constants.CONSUMERS_CATEGORY);
        if (consumers != null) {
            for (Map.Entry<String, Map<String, URL>> serviceEntry : consumers.entrySet()) {
                if (serviceEntry.getKey() != null) {
                    //某个服务的所有地址，一个服务可能会被多个应用消费
                    for (Map.Entry<String, URL> urlEntry : serviceEntry.getValue().entrySet()) {
                        URL u = urlEntry.getValue();
                        if (u.getParameter(Constants.INTERFACE_KEY).equals(MonitorService.class.getName())) {
                            continue;
                        }
                        String app = u.getParameter(Constants.APPLICATION);
                        if (app != null) {
                            Application application = new Application();
                            application.setApplication(app);
                            application.setRegistry(registryAddress);
                            application.setOwner(u.getParameter("owner"));
                            if (!applications.contains(application)) {
                                application.setType(CONSUMER);
                                applications.add(application);
                            } else {
                                application = applications.get(applications.indexOf(application));
                                if (PROVIDER.equals(application.getType())) {
                                    application.setType(PROVIDER_AND_CONSUMER);
                                }
                            }
                        }
                    }
                }
            }
        }
        return applications;
    }

    public static void main(String[] args) {
        List<Application> applications = new ArrayList<>();
        Application application1 = new Application();
        application1.setApplication("1");
        application1.setRegistry("1");
        application1.setOwner("1");
        application1.setType("1");
        applications.add(application1);
        Application application2 = new Application();
        application2.setApplication("1");
        application2.setRegistry("2");
        application2.setOwner("2");
        application2.setType("2");
        System.out.println(applications.contains(application2));
    }
}
