package com.symplified.order.services;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.symplified.order.App;
import com.symplified.order.R;

import java.util.List;


public class AlertService extends Service {

    private static MediaPlayer mediaPlayer;
    private static boolean hasRepeatedOnce = false;

    @Override
    public void onCreate() {
        super.onCreate();

        mediaPlayer = MediaPlayer.create(this, Settings.System.DEFAULT_ALARM_ALERT_URI);
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true);
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Notification notification = new Notification.Builder(this, App.ORDERS)
                    .setContentTitle("Symplified")
                    .setContentText("Waiting for orders")
                    .setPriority(Notification.PRIORITY_LOW)
                    .build();
            startForeground(1, notification);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mediaPlayer = MediaPlayer.create(this, R.raw.ring);
        String storeType = intent.getStringExtra(String.valueOf(R.string.store_type));
        if (isAppOnForeground(this) || (storeType != null && !storeType.contains("FnB"))) {
            mediaPlayer.setLooping(false);
            hasRepeatedOnce = false;
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    Log.d("AlertService mediaplayer", "onCompletion hasRepeatedOnce " + hasRepeatedOnce);
                    if (!hasRepeatedOnce) {
                        hasRepeatedOnce = true;
                        mp.seekTo(0);
                        mp.start();
                    }
                }
            });
        } else {
            mediaPlayer.setLooping(true);
        }

        AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

        if (!isExternalAudioOutputPluggedIn()) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                    AudioManager.FLAG_PLAY_SOUND);

            mediaPlayer.setVolume(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        }
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.start();
        return START_STICKY;
    }

    public static boolean isPlaying() {
        if (mediaPlayer != null) {
            return mediaPlayer.isPlaying();
        }
        return false;
    }

    public static void start() {
        if (null != mediaPlayer)
            mediaPlayer.start();
    }

    public static void stop() {
        if (null != mediaPlayer) {
            mediaPlayer.stop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaPlayer.stop();
    }

    private boolean isExternalAudioOutputPluggedIn() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        for (AudioDeviceInfo device
                : audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            if (device.getType() != AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    && device.getType() != AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                    && device.getType() != AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE) {
                return true;
            }
        }
        return false;
    }

    private boolean isAppOnForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }
}
