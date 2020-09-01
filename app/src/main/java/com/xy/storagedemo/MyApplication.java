package com.xy.storagedemo;

import android.app.Application;
import android.content.Context;

/**
 * Describe:
 * Created by xieying on 2020/9/1
 */
public class MyApplication extends Application {

    private Context context;

    private static MyApplication INSTANCE;

    public static MyApplication getInstance(){
        return INSTANCE;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
        context = getApplicationContext();
    }

    public Context getContext(){
        return context;
    }


}
