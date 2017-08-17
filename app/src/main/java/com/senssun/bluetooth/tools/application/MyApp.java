package com.senssun.bluetooth.tools.application;


import android.app.Application;
import android.content.Context;

import com.tencent.bugly.crashreport.CrashReport;

public class MyApp extends Application {
    private static Context context;
//    private ArrayList<GattObject> JdGattList = new ArrayList<GattObject>();
//    private List<MessObject> JdMessList = new ArrayList<MessObject>();

//    public List<MessObject> getJdMessList() {
//        return JdMessList;
//    }
//
//    public void setJdMessList(List<MessObject> jdMessList) {
//        JdMessList = jdMessList;
//    }
//
//    public ArrayList<GattObject> getJdGattList() {
//        return JdGattList;
//    }
//
//    public void setJdGattList(ArrayList<GattObject> jdGattList) {
//        JdGattList = jdGattList;
//    }

    @Override
    public void onCreate() {
        super.onCreate();

        this.context=getApplicationContext();

        CrashReport.initCrashReport(getApplicationContext(), "70e8669ad4", true);

    }

    public static Context getContext(){
        return context;
    }
}