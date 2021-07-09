package com.yinyuan.radarsdk.protocol;

import com.sirtrack.construct.Core;

import java.util.HashMap;
import java.util.Map;

import static com.sirtrack.construct.Macros.*;

/**
 * 24G雷达私有协议
 * @author YD_Tao
 */
public class MmWave24 {

    /**
     * 24G雷达数据的帧长度（协议上是216，李小双说子勇师兄加过sn号，需要把雷达配置为客户模式，客户模式多了三个sn为228长度）
     */
    public static final int DATA_FRAME_LENGTH = 228;

    /**固定帧头*/
    public static final String FRAME_HEAD = "55AA";

    /**单个包中目标数*/
    private static final int DEFAULT_TARGET_COUNT = 16;

    /**常量定义*/
    public static final String SEQUENCE = "seq";

    public static final String TARGET_COUNT = "targetCnt";

    public static final String SN1 = "sn1";

    public static final String SN2 = "sn2";

    public static final String SN3 = "sn3";

    public static final String TARGET = "target";

    public static final String TID = "tid";

    public static final String POWER = "power";

    public static final String DISTANCE = "distance";

    public static final String ANGLE = "angle";

    public static final String POS_X = "posX";

    public static final String POS_Y = "posY";

    public static final String SPEED = "speed";

    public static final Map<String, Double> TARGET_PROPERTIES = new HashMap<>();
    static {
        TARGET_PROPERTIES.put(TID, 1.0);
        TARGET_PROPERTIES.put(POWER, 10.0);
        TARGET_PROPERTIES.put(DISTANCE, 10.0);
        TARGET_PROPERTIES.put(ANGLE, 100.0);
        TARGET_PROPERTIES.put(POS_Y, 10.0);
        TARGET_PROPERTIES.put(POS_X, 10.0);
        TARGET_PROPERTIES.put(SPEED, 100.0);

    }

    /**协议结构体定义*/
    public static Core.Construct frameStruct = Core.Struct(
            "frameStruct",
            Field("head", 2),
            Field("command", 2),
            ULInt16(SEQUENCE),
            ULInt8(TARGET_COUNT),
            /*SN1、SN2、SN3需要配置雷达为客户模式*/
            UBInt32(SN1),
            UBInt32(SN2),
            UBInt32(SN3),
            Array(DEFAULT_TARGET_COUNT,
                    Core.Struct(TARGET,
                            ULInt8(TID),
                            ULInt16(POWER),
                            SLInt16(DISTANCE),
                            SLInt16(ANGLE),
                            ULInt16(POS_Y),
                            SLInt16(POS_X),
                            SLInt16(SPEED))
            ),
            ULInt8("checksum")
    );
}
