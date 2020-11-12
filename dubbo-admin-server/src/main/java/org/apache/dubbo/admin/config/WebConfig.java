//package org.apache.dubbo.admin.config;
//
//import org.apache.dubbo.admin.interceptor.LogInterceptor;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
///**
// * @author heyudev
// * @date 2019/05/09
// */
//@Configuration
//public class WebConfig implements WebMvcConfigurer {
//
//    @Autowired
//    private LogInterceptor logInterceptor;
//
//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        // 添加一个拦截器
//        registry.addInterceptor(logInterceptor).addPathPatterns("/**");
//    }
//}
