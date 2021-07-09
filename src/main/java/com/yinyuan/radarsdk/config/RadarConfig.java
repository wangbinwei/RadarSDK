package com.yinyuan.radarsdk.config;

import com.yinyuan.radarsdk.callback.RadarDataCallback;
import com.yinyuan.radarsdk.handler.impl.Radar24gDataHandlerImpl;
import com.yinyuan.radarsdk.handler.impl.Radar77gDataHandlerImpl;
import com.yinyuan.radarsdk.pojo.RadarServerPort;
import com.yinyuan.radarsdk.server.RadarServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

/**
 * 雷达配置接口
 * @author YD_Tao
 */
public interface RadarConfig {

    /**
     * 24G雷达端口
     * @return
     */
    RadarServerPort radar24gPort();

    /**
     * 77G雷达端口
     * @return
     */
    RadarServerPort radar77gPort();

    /**
     * 雷达消息回调
     * @return
     */
    RadarDataCallback<String> radarDataCallback();

    /**
     * 24雷达Server
     * @return
     */
    @Bean
    @Scope("prototype")
    default RadarServer radarServer24(){
        return new RadarServer();
    }

    /**
     * 24G雷达数据处理器
     * @return
     */
    @Bean
    default Radar24gDataHandlerImpl radar24gDataHandlerImpl(){
        return new Radar24gDataHandlerImpl();
    }

    /**
     * 77G雷达Server
     * @return
     */
    @Bean
    default RadarServer radarServer77(){
        return new RadarServer();
    }

    /**
     * 77G雷达数据处理器
     * @return
     */
    @Bean
    default Radar77gDataHandlerImpl radar77gDataHandlerImpl(){
        return new Radar77gDataHandlerImpl();
    }
}
