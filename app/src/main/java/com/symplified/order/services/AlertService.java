package com.symplified.order.services;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.symplified.order.OrdersActivity;
import com.symplified.order.R;
import com.symplified.order.enums.ServiceType;
import com.symplified.order.utils.ChannelId;
import com.symplified.order.utils.Utility;

import java.util.List;

public class AlertService extends Service {

    private static MediaPlayer mediaPlayer;
    private static boolean hasRepeatedOnce = false;
    public static final int notificationId = 27386;

    public void onCreate() {
        super.onCreate();

        mediaPlayer = MediaPlayer.create(this, Settings.System.DEFAULT_ALARM_ALERT_URI);
        startForeground(notificationId, getNotification("", ""));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String storeType = "", serviceType = "";
        if (intent != null && intent.getExtras() != null) {
            Log.d("alert-service", "extras not null");
            if (intent.hasExtra(getString(R.string.store_type))) {
                storeType = intent.getStringExtra(getString(R.string.store_type));
            }

            if (intent.hasExtra(getString(R.string.service_type))) {
                serviceType = intent.getStringExtra(getString(R.string.service_type));
            }

            if (intent.hasExtra("title")
                    && intent.hasExtra("body")) {
                String title = intent.getStringExtra("title");
                String body = intent.getStringExtra("body");

                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(notificationId, getNotification(title, body));
            }
        }

        mediaPlayer = MediaPlayer.create(this,
                serviceType != null
                        && serviceType.contains(ServiceType.DINEIN.toString())
                        ? R.raw.dine_in : R.raw.ring);

        if (isAppOnForeground(this)
                || !storeType.contains("FnB")
                || serviceType.contains(ServiceType.DINEIN.toString())) {
            mediaPlayer.setLooping(false);
            hasRepeatedOnce = false;
            mediaPlayer.setOnCompletionListener(mp -> {
                if (!hasRepeatedOnce) {
                    hasRepeatedOnce = true;
                    mp.seekTo(0);
                    mp.start();
                }
            });
        } else {
            mediaPlayer.setLooping(true);
        }

        AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

        if (!isExternalAudioOutputPluggedIn() && !serviceType.contains(ServiceType.DINEIN.toString())) {
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

    public static void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
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
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private Notification getNotification(String title, String body) {
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, OrdersActivity.class),
                PendingIntent.FLAG_IMMUTABLE
        );

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(
                    ChannelId.NEW_ORDERS,
                    ChannelId.NEW_ORDERS,
                    NotificationManager.IMPORTANCE_HIGH
            );
            chan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(chan);
        }
        return new NotificationCompat.Builder(this, ChannelId.NEW_ORDERS)
                .setContentTitle(!Utility.isBlank(title) ? title : "You have new orders")
                .setContentText(!Utility.isBlank(body) ? body : "Tap to view")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();
    }
}
