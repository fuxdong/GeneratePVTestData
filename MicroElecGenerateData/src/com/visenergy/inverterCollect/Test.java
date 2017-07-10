package com.visenergy.inverterCollect;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/7/6.
 */
public class Test {
    public static void main(String[] args){
        Timestamp timestamp = new Timestamp((System.currentTimeMillis()-189*24*3600*1000L-14*3600*1000L-35*60*1000L));
        System.out.println("======="+timestamp);
        List list = new ArrayList();
        for (int i=0;i<10;i++){
            list.add(i);

        }
        if (list.size()>4){
            for (int j =0;j<4;j++){
                list.remove(j);
            }
        }
        for (Object o:list){
            System.out.print(o+"\t");
        }
    }
}
