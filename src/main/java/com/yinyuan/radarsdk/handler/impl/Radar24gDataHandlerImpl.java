package com.yinyuan.radarsdk.handler.impl;

import com.alibaba.fastjson.JSONObject;
import com.sirtrack.construct.lib.Containers;
import com.yinyuan.radarsdk.callback.RadarDataCallback;
import com.yinyuan.radarsdk.enums.RadarEnum;
import com.yinyuan.radarsdk.handler.RadarDataHandler;
import com.yinyuan.radarsdk.protocol.MmWave24;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yinyuan.radarsdk.protocol.MmWave24.TARGET_PROPERTIES;

/**
 * 24G雷达数据处理器
 * @author YD_Tao
 */
@Slf4j
@Setter
@ChannelHandler.Sharable
public class Radar24gDataHandlerImpl extends ChannelInboundHandlerAdapter implements RadarDataHandler {

    /**
     * 回调接口，处理好的数据应该如何使用
     */
    private RadarDataCallback<String> callback;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("--->Radar 24G 客户端[{}] 连接成功.", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("--->Radar 24G 客户端[{}] 断开连接.", ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        log.info("--->Radar 24G 关闭闲置, 客户端连接[{}] ", ctx.channel().remoteAddress());
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //目标点list
        List<Map<String, Double>> targetList = new ArrayList<>();
        //结果集
        Map<String, Object> result = new HashMap<>(16);
        //客户端IP
        String ip = ((InetSocketAddress)ctx.channel().remoteAddress()).getAddress().getHostAddress();
        //数据处理
        try{
            //获取雷达数据包
            ByteBuf contentBuf = (ByteBuf) msg;
            byte[] contentArr = new byte[contentBuf.readableBytes()];
            if (contentArr.length != MmWave24.DATA_FRAME_LENGTH ){
                //24G雷达数据长度异常
                return;
            }
            contentBuf.getBytes(contentBuf.readerIndex(), contentArr);
            //使用私有协议解析数据包格式
            Containers.Container frameContainer =  MmWave24.frameStruct.parse(contentArr);
            //获取sn号
            String sn = frameContainer.get(MmWave24.SN1).toString();
            //获取目标点数量
            int targetCount = frameContainer.get(MmWave24.TARGET_COUNT);
            //处理目标点
            if (targetCount != 0){
                List<Containers.Container> frameTarget =  frameContainer.get(MmWave24.TARGET);
                for (int i = 0; i < targetCount; i++){
                    Map<String, Double> target = new HashMap<>(128);
                    int finalI = i;
                    TARGET_PROPERTIES.keySet().forEach((property)-> target.put(property, ((Integer) frameTarget.get(finalI).get(property)) / TARGET_PROPERTIES.get(property)));
                    targetList.add(target);
                }
                //获取SN号

                //数据封装
                result.put("dataType", RadarEnum.Radar_24G);
                result.put("sn", sn);
                result.put("data", targetList);

                //发送数据
                if (callback != null){
                    JSONObject jsonResult = new JSONObject(result);
                    callback.notify(jsonResult.toJSONString());
                }
            } else{
                //心跳数据，data部分为空
                result.put("dataType", RadarEnum.Radar_24G);
                result.put("sn", sn);
                result.put("data", null);
                //发送数据
                if (callback != null){
                    JSONObject jsonResult = new JSONObject(result);
                    callback.notify(jsonResult.toJSONString());
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }finally {
            ReferenceCountUtil.release(msg);
        }
    }
}
