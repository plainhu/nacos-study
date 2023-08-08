package com.plainhu.study;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.ConfigType;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;

import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * @Author: hkt
 * @Date 2023/1/18 17:09
 */
public class ConfigExample {

    private final static String DATA_ID = "common";
    private final static String GROUP = "dev";
    private final static String SERVER_ADDR = "127.0.0.1:8848";
    private final static String USERNAME = "nacos";
    private final static String PASSWORD = "nacos";

    static ConfigService getConfigService() throws NacosException {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, SERVER_ADDR);
        properties.put(PropertyKeyConst.USERNAME, USERNAME);
        properties.put(PropertyKeyConst.PASSWORD, PASSWORD);
        return NacosFactory.createConfigService(properties);
    }

    /**
     * 发布配置
     */
    static void publishConfig(String content) throws NacosException {
        ConfigService configService = getConfigService();
        /**
         * 参数 dataId：配置ID，采用类似 package.class（如com.taobao.tc.refund.log.level）的命名规则保证全局唯一性，class 部分建议是配置的业务含义。
         *                   全部字符小写。只允许英文字符和 4 种特殊字符（"."、":"、"-"、"_"），不超过 256 字节。
         * 参数 group：配置分组，建议填写产品名:模块名（Nacos:Test）保证唯一性，只允许英文字符和4种特殊字符（"."、":"、"-"、"_"），不超过128字节。
         * 参数 content：配置内容，不超过 100K 字节。
         * 参数 type：配置类型，见 com.alibaba.nacos.api.config.ConfigType，默认为TEXT
         */
        boolean result = configService.publishConfig(DATA_ID, GROUP, "123", ConfigType.TEXT.getType());
        System.out.println("发布结果：" + result);
    }

    /**
     * 获取配置
     */
    static void getConfig() throws NacosException {
        ConfigService configService = getConfigService();
        /**
         * dataId：配置 ID，采用类似 package.class（如com.taobao.tc.refund.log.level）的命名规则保证全局唯一性，class 部分建议是配置的业务含义。
         *          全部字符小写。只允许英文字符和 4 种特殊字符（"."、":"、"-"、"_"），不超过 256 字节。
         * group：配置分组，建议填写产品名:模块名（Nacos:Test）保证唯一性，只允许英文字符和4种特殊字符（"."、":"、"-"、"_"），不超过128字节。
         * timeout：读取配置超时时间，单位 ms，推荐值 3000。
         */
        String config = configService.getConfig(DATA_ID, GROUP, 1000);
        System.out.println("查询结果：" + config);
    }

    /**
     * 删除配置
     */
    static void removeConfig() throws NacosException {
        ConfigService configService = getConfigService();
        /**
         * dataId：配置 ID，采用类似 package.class（如com.taobao.tc.refund.log.level）的命名规则保证全局唯一性，class 部分建议是配置的业务含义。
         *          全部字符小写。只允许英文字符和 4 种特殊字符（"."、":"、"-"、"_"），不超过 256 字节。
         * group：配置分组，建议填写产品名:模块名（Nacos:Test）保证唯一性，只允许英文字符和4种特殊字符（"."、":"、"-"、"_"），不超过128字节。
         * timeout：读取配置超时时间，单位 ms，推荐值 3000。
         */
        boolean result = configService.removeConfig(DATA_ID, GROUP);
        System.out.println("删除结果：" + result);
    }

    /**
     * 监听配置
     * 如果希望 Nacos 推送配置变更，可以使用 Nacos 动态监听配置接口来实现。
     */
    static void listenerConfig() throws NacosException {

        //创建监听器
        Listener listener = new Listener() {
            @Override
            public Executor getExecutor() {
                return null;
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                System.out.println("接受变更配置:" + configInfo);
            }
        };

        ConfigService configService = getConfigService();
        /**
         * dataId：配置 ID，采用类似 package.class（如com.taobao.tc.refund.log.level）的命名规则保证全局唯一性，class 部分建议是配置的业务含义。
         *          全部字符小写。只允许英文字符和 4 种特殊字符（"."、":"、"-"、"_"），不超过 256 字节。
         * group：配置分组，建议填写产品名:模块名（Nacos:Test）保证唯一性，只允许英文字符和4种特殊字符（"."、":"、"-"、"_"），不超过128字节。
         * listener：监听器，配置变更进入监听器的回调函数。
         */
        configService.addListener(DATA_ID, GROUP, listener);

        // 暂停线程60s，因为订阅配置是守护线程，主线程退出守护线程就会退出。
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        configService.removeListener(DATA_ID, GROUP, listener);
    }


    public static void main(String[] args) throws NacosException {
        listenerConfig();
    }
}
