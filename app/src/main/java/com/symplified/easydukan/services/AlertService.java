package com.symplified.easydukan.services;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.symplified.easydukan.R;
import com.symplified.easydukan.ui.orders.OrdersActivity;
import com.symplified.easydukan.utils.ChannelId;
import com.symplified.easydukan.utils.Utility;

import java.util.List;

public class AlertService extends Service {

    private static MediaPlayer mediaPlayer;
    private static boolean hasRepeatedOnce = false;

    public static final int NOTIFICATION_ID = 27386;



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Necessary to prevent app from crashing
        startForeground(NOTIFICATION_ID, getNotification("", ""));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String title = "", body = "";

        if (intent != null
                && intent.getExtras() != null
                && intent.hasExtra("title")
                && intent.hasExtra("body")
        ) {
            title = intent.getStringExtra("title");
            body = intent.getStringExtra("body");
        }

        Notification notification = getNotification(title, body);

        startForeground(NOTIFICATION_ID, notification);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(NOTIFICATION_ID, notification);
        } else {
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                    .notify(NOTIFICATION_ID, notification);
        }

        mediaPlayer = MediaPlayer.create(this, R.raw.ring_dine_in);
        mediaPlayer.setLooping(false);

        hasRepeatedOnce = false;
        mediaPlayer.setOnCompletionListener(mp -> {
            if (!hasRepeatedOnce) {
                hasRepeatedOnce = true;
                mp.seekTo(0);
                mp.start();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                stopForeground(STOP_FOREGROUND_DETACH);
            }
        });

//        AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

//        if (!isExternalAudioOutputPluggedIn() &&
//                serviceType != null &&
//                serviceType.contains(ServiceType.DELIVERIN.toString())
//        ) {
//            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
//                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
//                    AudioManager.FLAG_PLAY_SOUND);
//
//            mediaPlayer.setVolume(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
//                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
//        }
        // TODO: setAudioStreamType is deprecated. Look for replacement.
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//        mediaPlayer.setAudioAttributes(AudioAttributes.CONTENT_TYPE_MUSIC);
        mediaPlayer.start();
        return START_STICKY;
    }

    public static boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .build();
    }
}
