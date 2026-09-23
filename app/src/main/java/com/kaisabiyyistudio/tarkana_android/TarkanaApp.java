package com.kaisabiyyistudio.tarkana_android;

import android.app.Application;

import com.kaisabiyyistudio.tarkana_android.telemetry.CrashReporter;

public class TarkanaApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CrashReporter.init(this);
    }
}
