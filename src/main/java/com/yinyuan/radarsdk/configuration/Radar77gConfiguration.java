package com.yinyuan.radarsdk.configuration;

import com.yinyuan.radarsdk.callback.RadarDataCallback;
import com.yinyuan.radarsdk.enums.RadarEnum;
import com.yinyuan.radarsdk.handler.impl.Radar77gDataHandlerImpl;
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
public class Radar77gConfiguration implements InitializingBean {

    /**
     * 77G雷达服务
     */
    private RadarServer radarServer77 = new RadarServer();

    /**
     * 77G雷达Handler
     */
    @Resource
    private Radar77gDataHandlerImpl radar77gDataHandler;

    /**
     * 24G雷达端口
     */
    @Resource
    private RadarServerPort radar77gPort;

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

        //radar77gPort || radarDataCallback为空时不能正常启动Radar Server服务
        if(radar77gPort == null){
            throw new IllegalArgumentException("Radar Server 启动失败, 请先初始化用于连接雷达的服务端口: " + RadarServerPort.class.getName());
        }
        if (radarDataCallback == null){
            throw new IllegalArgumentException("Radar Server 启动失败, 请先实现用于调用雷达数据的接口: " + RadarDataCallback.class.getName());
        }

        //启动24G雷达TCP服务
        ThreadPoolUtil.taskExecutor.execute(() -> {
            try {
                radar77gDataHandler.setCallback(radarDataCallback);
                radarServer77.startup(RadarEnum.Radar_77G.getType(), radar77gDataHandler, new InetSocketAddress(radar77gPort.getPort()));
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
        radarServer77.destroy();
    }
}
