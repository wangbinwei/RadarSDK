package com.yinyuan.radarsdk.configuration;

import com.yinyuan.radarsdk.callback.RadarDataCallback;
import com.yinyuan.radarsdk.enums.RadarEnum;
import com.yinyuan.radarsdk.handler.impl.Radar24gDataHandlerImpl;
import com.yinyuan.radarsdk.pojo.RadarServerPort;
import com.yinyuan.radarsdk.server.RadarServer;
import com.yinyuan.radarsdk.util.ThreadPoolUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.net.InetSocketAddress;

/**
 * @author tyd
 * @date 2021/7/3
 */
@Slf4j
@Configuration
public class Radar24gConfiguration implements InitializingBean {

    /**
     * 24G雷达服务
     */
    @Resource
    private RadarServer radarServer24;

    /**
     * 24G雷达Handler
     */
    @Resource
    private Radar24gDataHandlerImpl radar24gDataHandler;

    /**
     * 24G雷达端口
     */
    @Resource
    private RadarServerPort radar24gPort;

    /**
     * 雷达数据回调函数
     */
    @Resource
    private RadarDataCallback<String> radarDataCallback;

    /**
     * 启动Netty TCP服务器
     * */
    @Override
    public void afterPropertiesSet() throws Exception {

        //radar24gPort || radar77gPort || radarDataCallback为空时不能正常启动Radar Server服务
        if(radar24gPort == null){
            throw new IllegalArgumentException("Radar Server 启动失败, 请先初始化用于连接雷达的服务端口: " + RadarServerPort.class.getName());
        }
        if (radarDataCallback == null){
            throw new IllegalArgumentException("Radar Server 启动失败, 请先实现用于调用雷达数据的接口: " + RadarDataCallback.class.getName());
        }

        //启动24G雷达TCP服务
        ThreadPoolUtil.taskExecutor.execute(() -> {
            try {
                radar24gDataHandler.setCallback(radarDataCallback);
                radarServer24.startup(RadarEnum.Radar_24G.getType(), radar24gDataHandler, new InetSocketAddress(radar24gPort.getPort()));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Tomcat服务器关闭前需要手动关闭Netty TCP服务器相关资源
     *  1.释放Netty TCP相关连接
     *  */
    @PreDestroy
    public void close(){
        radarServer24.destroy();
    }
}
