/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.nacos.naming.core.v2.service.impl;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.exception.runtime.NacosRuntimeException;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.utils.NamingUtils;
import com.alibaba.nacos.common.notify.NotifyCenter;
import com.alibaba.nacos.naming.core.v2.ServiceManager;
import com.alibaba.nacos.naming.core.v2.client.Client;
import com.alibaba.nacos.naming.core.v2.client.manager.ClientManager;
import com.alibaba.nacos.naming.core.v2.client.manager.ClientManagerDelegate;
import com.alibaba.nacos.naming.core.v2.event.client.ClientOperationEvent;
import com.alibaba.nacos.naming.core.v2.event.metadata.MetadataEvent;
import com.alibaba.nacos.naming.core.v2.pojo.BatchInstancePublishInfo;
import com.alibaba.nacos.naming.core.v2.pojo.InstancePublishInfo;
import com.alibaba.nacos.naming.core.v2.pojo.Service;
import com.alibaba.nacos.naming.core.v2.service.ClientOperationService;
import com.alibaba.nacos.naming.misc.Loggers;
import com.alibaba.nacos.naming.pojo.Subscriber;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Operation service for ephemeral clients and services.
 *
 * @author xiweng.yy
 */
@Component("ephemeralClientOperationService")
public class EphemeralClientOperationServiceImpl implements ClientOperationService {
    
    private final ClientManager clientManager;
    
    public EphemeralClientOperationServiceImpl(ClientManagerDelegate clientManager) {
        this.clientManager = clientManager;
    }
    
    @Override
    public void registerInstance(Service service, Instance instance, String clientId) throws NacosException {
        //服务实例基本检查
        NamingUtils.checkInstanceIsLegal(instance);

        //确保Service单例存在(从singletonRepository中取出)
        Service singleton = ServiceManager.getInstance().getSingleton(service);
        //如果当前服务是持久性的（Nacos2.x将服务划分为临时和持久），报错：当前服务是持久性的，不允许注册临时实例
        if (!singleton.isEphemeral()) {
            throw new NacosRuntimeException(NacosException.INVALID_PARAM, String.format("Current service %s is persistent service, can't register ephemeral instance.", singleton.getGroupedServiceName()));
        }

        //根据客户端id，找到Client实例
        //这里使用的是GRPC进行通信，所以Client使用的是ConnectionBasedClient这个实现类。
        Client client = clientManager.getClient(clientId);
        if (!clientIsLegal(client, clientId)) {
            return;
        }
        //客户端Instance模型，转换为服务端Instance模型
        InstancePublishInfo instanceInfo = getPublishInfo(instance);
        //将client、service、instance建立起关系以及发布事件
        client.addServiceInstance(singleton, instanceInfo);
        //设置最后更新时间
        client.setLastUpdatedTime();
        //重新计算修正
        client.recalculateRevision();

        //发布服务实例注册事件
        NotifyCenter.publishEvent(new ClientOperationEvent.ClientRegisterServiceEvent(singleton, clientId));
        //发布服务实例元数据事件
        NotifyCenter.publishEvent(new MetadataEvent.InstanceMetadataEvent(singleton, instanceInfo.getMetadataId(), false));
    }
    
    @Override
    public void batchRegisterInstance(Service service, List<Instance> instances, String clientId) {
        Service singleton = ServiceManager.getInstance().getSingleton(service);
        if (!singleton.isEphemeral()) {
            throw new NacosRuntimeException(NacosException.INVALID_PARAM,
                    String.format("Current service %s is persistent service, can't batch register ephemeral instance.",
                            singleton.getGroupedServiceName()));
        }
        Client client = clientManager.getClient(clientId);
        if (!clientIsLegal(client, clientId)) {
            return;
        }
        BatchInstancePublishInfo batchInstancePublishInfo = new BatchInstancePublishInfo();
        List<InstancePublishInfo> resultList = new ArrayList<>();
        for (Instance instance : instances) {
            InstancePublishInfo instanceInfo = getPublishInfo(instance);
            resultList.add(instanceInfo);
        }
        batchInstancePublishInfo.setInstancePublishInfos(resultList);
        client.addServiceInstance(singleton, batchInstancePublishInfo);
        client.setLastUpdatedTime();
        NotifyCenter.publishEvent(new ClientOperationEvent.ClientRegisterServiceEvent(singleton, clientId));
        NotifyCenter.publishEvent(
                new MetadataEvent.InstanceMetadataEvent(singleton, batchInstancePublishInfo.getMetadataId(), false));
    }
    
    @Override
    public void deregisterInstance(Service service, Instance instance, String clientId) {
        if (!ServiceManager.getInstance().containSingleton(service)) {
            Loggers.SRV_LOG.warn("remove instance from non-exist service: {}", service);
            return;
        }
        Service singleton = ServiceManager.getInstance().getSingleton(service);
        Client client = clientManager.getClient(clientId);
        if (!clientIsLegal(client, clientId)) {
            return;
        }
        InstancePublishInfo removedInstance = client.removeServiceInstance(singleton);
        client.setLastUpdatedTime();
        client.recalculateRevision();
        if (null != removedInstance) {
            NotifyCenter.publishEvent(new ClientOperationEvent.ClientDeregisterServiceEvent(singleton, clientId));
            NotifyCenter.publishEvent(
                    new MetadataEvent.InstanceMetadataEvent(singleton, removedInstance.getMetadataId(), true));
        }
    }
    
    @Override
    public void subscribeService(Service service, Subscriber subscriber, String clientId) {
        Service singleton = ServiceManager.getInstance().getSingletonIfExist(service).orElse(service);
        Client client = clientManager.getClient(clientId);
        if (!clientIsLegal(client, clientId)) {
            return;
        }
        client.addServiceSubscriber(singleton, subscriber);
        client.setLastUpdatedTime();
        NotifyCenter.publishEvent(new ClientOperationEvent.ClientSubscribeServiceEvent(singleton, clientId));
    }
    
    @Override
    public void unsubscribeService(Service service, Subscriber subscriber, String clientId) {
        Service singleton = ServiceManager.getInstance().getSingletonIfExist(service).orElse(service);
        Client client = clientManager.getClient(clientId);
        if (!clientIsLegal(client, clientId)) {
            return;
        }
        client.removeServiceSubscriber(singleton);
        client.setLastUpdatedTime();
        NotifyCenter.publishEvent(new ClientOperationEvent.ClientUnsubscribeServiceEvent(singleton, clientId));
    }
    
    private boolean clientIsLegal(Client client, String clientId) {
        if (client == null) {
            Loggers.SRV_LOG.warn("Client connection {} already disconnect", clientId);
            return false;
        }
        if (!client.isEphemeral()) {
            Loggers.SRV_LOG.warn("Client connection {} type is not ephemeral", clientId);
            return false;
        }
        return true;
    }
}
