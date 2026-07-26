package com.sadik.callrecorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log

class CallReceiver : BroadcastReceiver() {

    companion object {
        private var lastState = TelephonyManager.EXTRA_STATE_IDLE
        private const val TAG = "CagriKaydedici"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Bu receiver icinde olusabilecek her turlu hata, uygulamanin
        // tamamen cokmesine yol acmasin diye tek bir try-catch icine alindi.
        try {
            if (intent.action != "android.intent.action.PHONE_STATE") return

            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return

            when (state) {
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    if (lastState != TelephonyManager.EXTRA_STATE_OFFHOOK) {
                        startRecording(context)
                    }
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    stopRecording(context)
                }
            }
            lastState = state
        } catch (e: Exception) {
            Log.e(TAG, "CallReceiver hatasi: ${e.message}", e)
        }
    }

    private fun startRecording(context: Context) {
        try {
            val serviceIntent = Intent(context, RecordingService::class.java)
            serviceIntent.action = RecordingService.ACTION_START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Kayit baslatilamadi: ${e.message}", e)
        }
    }

    private fun stopRecording(context: Context) {
        try {
            val serviceIntent = Intent(context, RecordingService::class.java)
            serviceIntent.action = RecordingService.ACTION_STOP
            context.startService(serviceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Kayit durdurulamadi: ${e.message}", e)
        }
    }
}
