package com.symplified.order.services;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.PersistableBundle;
import android.util.Log;
import android.widget.Toast;

public class StoreManagerService extends JobService {
    public static final String TAG = "StoreManagerService";
    @Override
    public boolean onStartJob(JobParameters jobParameters) {

//        PersistableBundle bundle = jobParameters.getExtras();
//        String storeId = bundle.getString("storeId");
        Log.e(TAG, "onStartJob: check exception", new Error() );

        new Thread(new Runnable() {
            @Override
            public void run() {
//                Toast.makeText(getApplicationContext(), "Testing background jobs", Toast.LENGTH_SHORT).show();
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
                jobFinished(jobParameters, false);
            }
        }).start();

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG, "onStopJob: Job Cancelled");
        return true;
    }
}
