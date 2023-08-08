package com.plainhu.study;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.List;
import java.util.Properties;

/**
 * @Author plainhu
 * @Date 2023/8/4 22:54
 * @Description: TODO
 */
public class DiscoverDemo {


    private final static String SERVICE_NAME = DiscoverDemo.class.getSimpleName();
    private final static NamingService namingService;

    static {
        try {
            namingService = getNamingService();
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    static NamingService getNamingService() throws NacosException {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, DemoConstant.NACOS_ADDRESS);
        properties.put(PropertyKeyConst.USERNAME, DemoConstant.NACOS_USERNAME);
        properties.put(PropertyKeyConst.PASSWORD, DemoConstant.NACOS_PASSWORD);
        return NacosFactory.createNamingService(properties);
    }

    public static void main(String[] args) throws NacosException, InterruptedException {
        subscribe();
//        Thread.sleep(60000);
    }


    /**
     * 注册实例
     */
    static void registerInstance() throws NacosException {
        /*
         * 参数 serviceName：服务名
         * 参数 groupName：服务组名
         * 参数 ip：服务实例IP
         * 参数 port：服务实例port
         * 参数 clusterName：集群名
         */
        namingService.registerInstance(SERVICE_NAME, Constants.DEFAULT_GROUP, DemoConstant.SERVICE_IP, DemoConstant.SERVICE_PORT, Constants.DEFAULT_CLUSTER_NAME);
    }

    /**
     * 注销实例
     */
    static void deregisterInstance() throws NacosException {
        /*
         * 参数 serviceName：服务名
         * 参数 groupName：服务组名
         * 参数 ip：服务实例IP
         * 参数 port：服务实例port
         * 参数 clusterName：集群名
         */
        namingService.deregisterInstance(SERVICE_NAME, Constants.DEFAULT_GROUP, DemoConstant.SERVICE_IP, DemoConstant.SERVICE_PORT, Constants.DEFAULT_CLUSTER_NAME);
    }

    /**
     * 获取服务下的所有实例
     */
    static void getAllInstances() throws NacosException {
        /*
         * 参数 serviceName：服务名
         * 参数 clusters：集群列表
         */
        List<Instance> allInstances = namingService.getAllInstances(SERVICE_NAME);
        System.out.println("获取服务下的所有实例结果：" + allInstances);
    }

    /**
     * 根据条件获取过滤后的实例列表
     */
    static void getAllInstancesByCondition() throws NacosException {
        /*
         * 参数 serviceName：服务名
         * 参数 healthy：是否只要健康的实例
         */
        List<Instance> allInstances = namingService.selectInstances(SERVICE_NAME, true);
        System.out.println("获取服务下的健康实例结果：" + allInstances);
    }

    /**
     * 获取一个健康实例
     * 根据【负载均衡算法】随机获取一个健康实例。
     */
    static void selectOneHealthyInstance() throws NacosException {
        /*
         * 参数 serviceName：服务名
         */
        Instance instance = namingService.selectOneHealthyInstance(SERVICE_NAME);
        System.out.println("获取一个健康实例结果：" + instance);
    }

    /**
     * 订阅（监听）服务
     * 监听服务下的实例列表变化。
     */
    static void subscribe() throws NacosException {
        EventListener eventListener = event -> {
            if (event instanceof NamingEvent) {
                NamingEvent namingEvent = (NamingEvent) event;
                System.out.println(namingEvent.getServiceName());
                System.out.println(namingEvent.getInstances());
            }
        };
        /*
         * 参数 serviceName：服务名
         * 参数 listener：事件监听器
         */
        namingService.subscribe(SERVICE_NAME, eventListener);
    }

    /**
     * 取消订阅（监听）服务
     */
    static void unsubscribe() throws NacosException {
        EventListener eventListener = event -> {
            if (event instanceof NamingEvent) {
                NamingEvent namingEvent = (NamingEvent) event;
                System.out.println(namingEvent.getServiceName());
                System.out.println(namingEvent.getInstances());
            }
        };
        namingService.unsubscribe(SERVICE_NAME, eventListener);
    }

}
