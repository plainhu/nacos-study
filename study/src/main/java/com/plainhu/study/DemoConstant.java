package com.plainhu.study;

import cn.hutool.core.text.StrPool;

/**
 * @Author plainhu
 * @Date 2023/8/4 23:04
 * @Description: TODO
 */
public class DemoConstant {

    public final static String NACOS_IP = "127.0.0.1";

    public final static String SERVICE_IP = "127.0.0.1";

    public final static Integer NACOS_PORT = 8848;

    public final static Integer SERVICE_PORT = 9999;

    public final static String NACOS_ADDRESS = NACOS_IP + StrPool.COLON + NACOS_PORT;

    public final static String SERVICE_ADDRESS = SERVICE_IP + StrPool.COLON + SERVICE_PORT;
    public static final String NACOS_USERNAME = "nacos";
    public static final String NACOS_PASSWORD = "nacos";
}
