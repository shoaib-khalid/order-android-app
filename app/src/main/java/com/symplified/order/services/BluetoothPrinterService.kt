package com.symplified.order.services

import android.Manifest.permission
import android.app.*
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.symplified.order.R
import com.symplified.order.utils.ChannelId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.concurrent.thread

class BluetoothPrinterService : Service() {

    companion object {
        const val NOTIFICATION_ID = 32419
        const val MY_UUID = "00001101-0000-1000-8000-00805F9B34FB"
        const val PRINT_DATA = "print_data"
        const val TAG = "btprintservice"
    }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                ChannelId.PRINTING_NEW_ORDER,
                ChannelId.PRINTING_NEW_ORDER,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            chan.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).apply {
                createNotificationChannel(chan)
            }
        }

        startForeground(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, ChannelId.PRINTING_NEW_ORDER)
                .setContentTitle(ChannelId.PRINTING_NEW_ORDER)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                ).build()
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            && ContextCompat.checkSelfPermission(
                applicationContext,
                permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_DENIED
        ) {
            Log.d(TAG, "Permission not granted")
            stopSelf()
            return START_STICKY
        }

        Log.d(TAG, "Permission available. proceeding")

        intent?.getByteArrayExtra(PRINT_DATA).let { printData ->
            val adapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
            val pairedDevices = adapter.bondedDevices

//                App.getBtPrinterRepository().allPrinters.collect { printers ->
//                    printers.forEach { printer ->
//                        if (printer.isEnabled) {
//                            pairedDevices.firstOrNull {
//                                it.name == printer.name
//                            }?.let { pairedDevice ->
            pairedDevices.forEach { pairedDevice ->
                if (pairedDevice.name.lowercase().contains("cloudprint")) {

                    thread {
                        Log.d(TAG, "Creating socket for ${pairedDevice.name}")
                        pairedDevice.createRfcommSocketToServiceRecord(
                            UUID.fromString(MY_UUID)
                        )?.apply {
                            var noOfTries = 0
                            while (noOfTries < 3) {
                                try {
                                    if (!isConnected) {
                                        Log.d(TAG, "Connecting to socket for ${pairedDevice.name}")
                                        connect()
                                    }
                                    Log.d(TAG, "Writing to socket for ${pairedDevice.name}")
                                    thread {
                                        outputStream.write(printData)
                                    }
                                    Log.d(TAG, "Done writing to socket for ${pairedDevice.name}")
//                                    break
                                } catch (e: Throwable) {
                                    Log.e(
                                        TAG,
                                        "Error occurred while connecting to ${pairedDevice.name}. ${e.localizedMessage}"
                                    )
                                }
                                noOfTries++
                            }
                        }
                    }
                }
            }
//                            }
//                        }
//                    }
//                }
        }

        return START_STICKY
    }

    override fun stopService(name: Intent?): Boolean {
        return super.stopService(name)
    }

    override fun onDestroy() {
        super.onDestroy()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).apply {
            cancel(NOTIFICATION_ID)
        }
    }
}