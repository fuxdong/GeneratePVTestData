package com.visenergy.inverterCollect;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Fuxdong on 2017/7/6.
 */
public class SplitTime {
    public static List<Integer> getTime(String time){
        List<Integer> list = new ArrayList<Integer>();
        String[] times = time.trim().split("-");
        //获取年
        int year = Integer.parseInt(times[0]);
        //获取月
        int month = Integer.parseInt(times[1]);
        String[] time2 = times[2].split(" ");
        //获取日
        int day = Integer.parseInt(time2[0]);
        String[] time3 = time2[1].split(":");
        //获取小时
        int hour = Integer.parseInt(time3[0]);
        list.add(year);
        list.add(month);
        list.add(day);
        list.add(hour);
        return list;
    }
}
