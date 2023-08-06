package com.plainhu.study;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;

import java.util.Properties;

/**
 * @Author plainhu
 * @Date 2023/8/4 22:54
 * @Description: TODO
 */
public class DiscoverDemo {


    public static void main(String[] args) throws NacosException, InterruptedException {
        register();
//        Thread.sleep(60000);
    }


    static void register() throws NacosException {
        Properties properties = new Properties();
        properties.put("serverAddr", DemoConstant.SERVER_ADDRESS);
        //创建NamingService对象
        NamingService namingService = NamingFactory.createNamingService(properties);
        //调用registerInstance注册服务实例
        namingService.registerInstance(DiscoverDemo.class.getTypeName(), DemoConstant.SERVER_IP, DemoConstant.SERVICE_PORT);
    }

}
