package com.symplified.order.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class StoreBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = StoreBroadcastReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(new Intent(context, AlertService.class));
                }
                for(int i=0; i<5; i++)
                {
                    Log.d(TAG, "onStartJob: "+i);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Log.d(TAG, "run: Job Finished");
                context.stopService(new Intent(context, AlertService.class));

            }
        }).start();

    }
}
