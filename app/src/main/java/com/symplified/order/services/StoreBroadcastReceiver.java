package com.symplified.order.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StoreBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = StoreBroadcastReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {

        new Thread(() -> {
            //        ComponentName componentName = new ComponentName(this, StoreManagerService.class);
////        PersistableBundle bundle = new PersistableBundle();
////        bundle.putString("storeId", "Testing Store Closure");
//        JobInfo.Builder builder = new JobInfo.Builder(1423, componentName)
//                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
//                .setPersisted(true)
//                .setPeriodic(60 * 1000);
//        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
//        int resultCode = scheduler.schedule(builder.build());
//        if(resultCode == JobScheduler.RESULT_SUCCESS){
//            Log.d("LoginActivity", "initScheduler: Job Scheduled");
//        }else
//            Log.d("LoginActivity", "initScheduler: Job could not Scheduled");
//
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    context.startForegroundService(new Intent(context, AlertService.class));
//                }
//                for(int i=0; i<5; i++)
//                {
//                    Log.d(TAG, "onStartJob: "+i);
//
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//                Log.d(TAG, "run: Job Finished");
//                context.stopService(new Intent(context, AlertService.class));



        }).start();

    }
}
