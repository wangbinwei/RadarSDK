package com.yinyuan.radarsdk.handler.impl;

import com.alibaba.fastjson.JSONObject;
import com.sirtrack.construct.lib.Containers;
import com.yinyuan.radarsdk.callback.RadarDataCallback;
import com.yinyuan.radarsdk.enums.RadarEnum;
import com.yinyuan.radarsdk.handler.RadarDataHandler;
import com.yinyuan.radarsdk.protocol.MmWave77;
import com.yinyuan.radarsdk.util.ByteConvertUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.ReferenceCountUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sirtrack.construct.lib.Binary.byteArrayToHexString;
import static com.sirtrack.construct.lib.Binary.hexStringToByteArray;
import static com.yinyuan.radarsdk.protocol.MmWave77.*;

/**
 * 77G雷达数据处理器
 * @author YD_Tao
 */
@Slf4j
@Setter
@ChannelHandler.Sharable
public class Radar77gDataHandlerImpl extends ChannelInboundHandlerAdapter implements RadarDataHandler {

    /**
     * 回调接口，处理好的数据应该如何使用
     */
    private RadarDataCallback<String> callback;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("--->Radar 77G 客户端[{}] 连接成功.", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("--->Radar 77G 客户端[{}] 断开连接.", ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        log.info("--->Radar 77G 关闭闲置, 客户端[{}] ", ctx.channel().remoteAddress());
        ctx.close();
    }

    /**
     * 从通道读取数据
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            FullHttpRequest request = (FullHttpRequest) msg;
            if(!request.decoderResult().equals(DecoderResult.SUCCESS)) {
                log.warn("Radar 77G : A decoding error occurred in the received HTTP request");
                return;
            }
            //请求URI
            String uri = request.uri();
            String[] uriParameter = uri.split("/");
            if (uriParameter.length < 3) {
                log.warn("Radar 77G : The fileNames' length less than 3! Uri=" + uri);
                return;
            }
            //77G雷达可以从URI获取设备sn号
            String sn = uriParameter[2];
            //请求体内容
            ByteBuf contentBuf = request.content();
            byte[] contentArr = new byte[contentBuf.readableBytes()];
            contentBuf.getBytes(contentBuf.readerIndex(), contentArr);
            StringBuilder content = new StringBuilder(byteArrayToHexString(contentArr));
            //请求体的内容逐帧解析
            int frameStartIndex = content.indexOf(SYNCPATTERN);
            int frameEndIndex = content.indexOf(SYNCPATTERN, frameStartIndex + SYNC_PATTERN_LENGTH_IN_BYTES >> 1);
            if (frameStartIndex != -1 ) {
                while (frameEndIndex != -1){
                    String frame = content.substring(frameStartIndex, frameEndIndex);
                    content.delete(frameStartIndex, frameEndIndex);
                    frameEndIndex = content.indexOf(SYNCPATTERN, frameStartIndex + SYNC_PATTERN_LENGTH_IN_BYTES >> 1);
                    parseOneFrame(frame, sn);
                }
                String frame = content.toString();
                parseOneFrame(frame, sn);
            }
        }finally {
            ReferenceCountUtil.release(msg);
        }
    }

    /**
     * 解析一帧数据
     * @param frame
     * @param sn
     */
    private void parseOneFrame(String frame, String sn){
        if ((FRAME_HEADER_LENGTH_IN_BYTES << 1) > frame.length()) {
            log.warn("Radar 77G : The length of the data part of the frame is wrong, this data analysis failed and skipped(1)!");
            return;
        }
        //处理帧头
        String frameHeaderContent = frame.substring(0, FRAME_HEADER_LENGTH_IN_BYTES << 1);
        Containers.Container frameHeader = MmWave77.frameHeaderStructType.parse(hexStringToByteArray(frameHeaderContent));
        //数据部分长度(按字节) = 包长 - 帧头长度
        int dataLength = (int) frameHeader.get(FRAME_PACKETLENGTH) - FRAME_HEADER_LENGTH_IN_BYTES;
        //偏移量，左移是因为两位十六进制为一个字节(目前是十六进制数据)
        int frameOffset = FRAME_HEADER_LENGTH_IN_BYTES << 1;
        //一帧的数据部分(十六进制)
        String frameData = frame.substring(frameOffset);
        //数据长度效验
        if (dataLength <= 0 || dataLength << 1 != frameData.length()){
            log.warn("Radar 77G : The length of the data part of the frame is wrong, this data analysis failed and skipped(2)!");
            return;
        }
        //该帧包含的tlv个数
        int tlvCount = frameHeader.get(TLV_NUM);
        //解析数据部分
        parseDataOfFrame(frameData, tlvCount, sn);
    }


    /**
     * @param frameData
     * @param tlvCount
     * @param sn
     */
    private void parseDataOfFrame(String frameData, int tlvCount, String sn){
        //目标点list
        List<Map<String, Object>> targetList = new ArrayList<>();
        //结果集
        Map<String, Object> result = new HashMap<>(16);

        //偏移量
        int offset = 0;
        //处理给一个tlv
        for (int i = 0; i < tlvCount; i++) {
            //获取tlv头
            Containers.Container tlvHeader = MmWave77.tlvHeaderStruct.parse(
                    hexStringToByteArray(frameData.substring(offset, offset + 16))
            );
            //获取tlv信息
            int tlvType = tlvHeader.get(TLV_HEADER_TYPE);
            int tlvLength = tlvHeader.get(TLV_HEADER_LENGHT);
            //长度效验
            if (((tlvLength << 1 )) + offset > frameData.length()) {
                log.warn("Radar 77G : The length of the data part of the frame is wrong, this data analysis failed and skipped(3)!");
                return;
            }
            //处理tlv体
            offset += (TLV_HEADER_LENGTH_IN_BYTES << 1);
            int valueLength = tlvLength - TLV_HEADER_LENGTH_IN_BYTES;
            switch (tlvType) {
                case 6:
                    //数据封装,tlv类型6为心跳数据
                    result.put("dataType", RadarEnum.Radar_77G);
                    result.put("sn", sn);
                    result.put("data", null);
                    //发送数据
                    if (callback != null){
                        JSONObject jsonResult = new JSONObject(result);
                        callback.notify(jsonResult.toJSONString());
                    }
                    offset += (valueLength << 1);
                    break;
                case 7:
                    //tlv类型6为目标点数据
                    try {
                        //处理目标点，一个tlv包含多个目标点
                        int targetCount = valueLength / TARGET_LENGTH_IN_BYTES;
                        for (int j = 0; j < targetCount; j++) {
                            //解析一个目标点
                            Containers.Container targetStruct = MmWave77.targetStruct2D.parse(
                                    hexStringToByteArray(
                                            frameData.substring(offset, offset + (TARGET_LENGTH_IN_BYTES << 1))
                                    )
                            );
                            //目标点
                            Map<String, Object> target = new HashMap<>(16);
                            //封装tid, posX, posY, velX, velY, accX, accY
                            TARGET_PROPERTIES.forEach((key)-> {
                                Object value = targetStruct.get(key);
                                if (value instanceof byte[]){
                                    target.put(key, ByteConvertUtil.byte2float(targetStruct.get(key), 0));
                                } else {
                                    target.put(key, value);
                                }
                            });
                            //添加到目标点list
                            targetList.add(target);
                            offset += (TARGET_LENGTH_IN_BYTES << 1);
                        }
                        //数据封装
                        result.put("dataType", RadarEnum.Radar_77G);
                        result.put("sn", sn);
                        result.put("data", targetList);
                        //发送
                        if (callback != null){
                            JSONObject jsonResult = new JSONObject(result);
                            callback.notify(jsonResult.toJSONString());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                    break;
                default:
                    offset += (valueLength << 1);
                    break;
            }
        }
    }
}
