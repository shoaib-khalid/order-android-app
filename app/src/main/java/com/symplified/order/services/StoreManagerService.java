package com.symplified.order.services;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.PersistableBundle;
import android.util.Log;
import android.widget.Toast;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class StoreManagerService extends JobService {
    public static final String TAG = "StoreManagerService";
    private String BASE_URL;

    public StoreManagerService(){}

    public StoreManagerService(String BASE_URL){
        super();
        this.BASE_URL = BASE_URL;
    }
    @Override
    public boolean onStartJob(JobParameters jobParameters) {

//        PersistableBundle bundle = jobParameters.getExtras();
//        String storeId = bundle.getString("storeId");

        new Thread(() -> {
//                Toast.makeText(getApplicationContext(), "Testing background jobs", Toast.LENGTH_SHORT).show();

//                scheduleStoreOpen();


            Log.d(TAG, "run: Job Finished");
            jobFinished(jobParameters, false);
        }).start();

        return true;
    }

    private void scheduleStoreOpen() {
        Retrofit retrofit = new Retrofit.Builder()
                .client(new OkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("").build();
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG, "onStopJob: Job Cancelled");
        return true;
    }


}
