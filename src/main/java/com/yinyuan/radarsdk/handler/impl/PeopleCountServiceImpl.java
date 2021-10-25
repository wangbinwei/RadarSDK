package com.yinyuan.radarsdk.handler.impl;

import com.yinyuan.radarsdk.handler.PeopleCountService;
import com.yinyuan.radarsdk.pojo.PeopleCount;
import com.yinyuan.radarsdk.util.Threads;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author wbw
 * @date 2021/8/16 14:23
 */
@Slf4j
@Service
public class PeopleCountServiceImpl implements PeopleCountService {
    public static int ON_START=0;
    public static Date START_TIME;


    private static List<PeopleCount> list = new ArrayList<>();
    private static Map<String, List<PeopleCount>> RadarMap = new HashMap<>();
    @Override
    public void insertData(PeopleCount peopleCount) {

    }

    @Override
    public void insertData2List(PeopleCount peopleCount) {

        if(peopleCount.getPeopleNum() != 0 && (peopleCount.getCreateTime().getTime() - PeopleCountServiceImpl.START_TIME.getTime()) >= 2000){
            log.info("插入数据.");
            list.add(peopleCount);
        }
    }

    @Override
    public Integer getResultFromList() {
        log.info("获取雷达计数结果.");
        ON_START=1; //启动计时间
        LocalDateTime now = LocalDateTime.now(); //获取当前时间
        START_TIME= Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        Threads.sleep(3000); //时间可以++
        ON_START=0;
        Integer res = 0;
        if(list.isEmpty()){
            return res;
        }
        res = getHighestFrequencyNum(list);
        //从list数据中获取人数,取2-4s出现次数最多的数字
        /*HashMap<Integer,Integer> map = new HashMap<Integer,Integer>();
        HashMap<Integer, Integer> finalOut = new LinkedHashMap<>();
        //更具sn来判断
        for (PeopleCount peopleCount : list) {
            Integer count = map.get(peopleCount.getPeopleNum());
            if(count == null){
                count = 0;
            }
            map.put(peopleCount.getPeopleNum(), count+1);
        }
        //
        map.entrySet().stream().sorted((p1, p2) -> p2.getValue().compareTo(p1.getValue()))
                .collect(Collectors.toList())
                .forEach(ele->finalOut.put(ele.getKey(), ele.getValue()));
        list.clear();
        //获得到排序号的第一个
        for(Map.Entry<Integer, Integer> entry:finalOut.entrySet()){
            res = entry.getKey();
            break;
        }*/

        return res;
    }

    /**
     * 获取list中数字频率最高的数字
     * @param listNum
     * @return
     */
    public Integer getHighestFrequencyNum(List<PeopleCount> listNum){
        Integer res = 0;
        HashMap<Integer,Integer> map = new HashMap<Integer,Integer>();
        HashMap<Integer, Integer> finalOut = new LinkedHashMap<>();
        //根据sn来判断
        for (PeopleCount peopleCount : listNum) {
            map.put(peopleCount.getPeopleNum(), map.getOrDefault(peopleCount.getPeopleNum(),0)+1);
//            Integer count = map.get(peopleCount.getPeopleNum());
//            if(count == null){
//                count = 0;
//            }
//            map.put(peopleCount.getPeopleNum(), count+1);
        }
        //
        map.entrySet().stream().sorted((p1, p2) -> p2.getValue().compareTo(p1.getValue()))
                .collect(Collectors.toList())
                .forEach(ele->finalOut.put(ele.getKey(), ele.getValue()));
        list.clear();
        //获得到排序号的第一个
        for(Map.Entry<Integer, Integer> entry:finalOut.entrySet()){
            res = entry.getKey();
            break;
        }
        return res;
    }
}
