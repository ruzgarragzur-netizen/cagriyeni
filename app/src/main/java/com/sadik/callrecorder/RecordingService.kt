package com.sadik.callrecorder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingService : Service() {

    companion object {
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        private const val CHANNEL_ID = "kayit_kanali"
        private const val NOTIF_ID = 42
        private const val TAG = "CagriKaydedici"
    }

    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Her ihtimale karşı: burada oluşabilecek HERHANGİ bir hata artık
        // uygulamayı çökertmeyecek, sadece log'a yazılıp servis sessizce durdurulacak.
        try {
            when (intent?.action) {
                ACTION_START -> {
                    startForeground(NOTIF_ID, buildNotification())
                    beginRecording()
                }
                ACTION_STOP -> {
                    stopRecordingSafely()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onStartCommand hatası: ${e.message}", e)
            try {
                stopForeground(true)
                stopSelf()
            } catch (ignored: Exception) {
            }
        }
        return START_NOT_STICKY
    }

    private fun beginRecording() {
        try {
            val dir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "CagriKayitlari")
            if (!dir.exists()) dir.mkdirs()

            val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            // Geçici isimle başlat, görüşme bitince numara/isim varsa yeniden adlandıracağız.
            val file = File(dir, "gecici_$stamp.m4a")
            currentFile = file

            val sourcesToTry = listOf(
                MediaRecorder.AudioSource.VOICE_CALL,
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                MediaRecorder.AudioSource.MIC
            )

            for (source in sourcesToTry) {
                try {
                    val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        MediaRecorder(this)
                    } else {
                        @Suppress("DEPRECATION")
                        MediaRecorder()
                    }
                    mr.setAudioSource(source)
                    mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    mr.setAudioEncodingBitRate(96000)
                    mr.setAudioSamplingRate(44100)
                    mr.setOutputFile(file.absolutePath)
                    mr.prepare()
                    mr.start()
                    recorder = mr
                    Log.i(TAG, "Kayit basladi, kaynak: $source, dosya: ${file.name}")
                    return
                } catch (e: Exception) {
                    Log.w(TAG, "Ses kaynagi $source basarisiz: ${e.message}")
                }
            }
            Log.e(TAG, "Hicbir ses kaynagi calismadi, kayit yapilamadi.")
        } catch (e: Exception) {
            Log.e(TAG, "beginRecording hatasi: ${e.message}", e)
        }
    }

    private fun stopRecordingSafely() {
        val fileBeingRecorded = currentFile
        try {
            recorder?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Kayit durdurulurken hata (cok kisa gorusme olabilir): ${e.message}")
            fileBeingRecorded?.delete()
        } finally {
            try {
                recorder?.release()
            } catch (ignored: Exception) {
            }
            recorder = null
        }

        // Kayıt dosyası oluştuysa, arama kaydından numara/isim bilgisini
        // eklemeyi bir saniye gecikmeyle dene (arama kaydı sisteme hemen yazılmayabiliyor).
        if (fileBeingRecorded != null && fileBeingRecorded.exists()) {
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    renameWithCallerInfo(fileBeingRecorded)
                } catch (e: Exception) {
                    Log.w(TAG, "Dosya yeniden adlandirilamadi: ${e.message}")
                } finally {
                    try {
                        stopForeground(true)
                        stopSelf()
                    } catch (ignored: Exception) {
                    }
                }
            }, 1200)
        } else {
            try {
                stopForeground(true)
                stopSelf()
            } catch (ignored: Exception) {
            }
        }
    }

    private fun renameWithCallerInfo(file: File) {
        val hasCallLogPerm = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCallLogPerm) return

        val info = getLastCallInfo() ?: return
        val safeLabel = sanitizeForFileName(info)
        if (safeLabel.isBlank()) return

        val newName = file.name.replace("gecici_", "cagri_${safeLabel}_")
        val newFile = File(file.parentFile, newName)
        if (file.renameTo(newFile)) {
            currentFile = newFile
            Log.i(TAG, "Dosya yeniden adlandirildi: ${newFile.name}")
        }
    }

    /** Arama kaydından en son aramanın numarasını ve (varsa) rehber ismini döndürür. */
    private fun getLastCallInfo(): String? {
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER),
                null, null,
                CallLog.Calls.DATE + " DESC LIMIT 1"
            )
            if (cursor != null && cursor.moveToFirst()) {
                val number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER))
                    ?: return null
                val name = lookupContactName(number)
                return if (name != null) "${name}_${number}" else number
            }
        } catch (e: Exception) {
            Log.w(TAG, "Arama kaydi okunamadi: ${e.message}")
        } finally {
            cursor?.close()
        }
        return null
    }

    private fun lookupContactName(phoneNumber: String): String? {
        val hasContactsPerm = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasContactsPerm) return null

        var cursor: Cursor? = null
        try {
            val uri = android.net.Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                android.net.Uri.encode(phoneNumber)
            )
            cursor = contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Rehber sorgusu basarisiz: ${e.message}")
        } finally {
            cursor?.close()
        }
        return null
    }

    private fun sanitizeForFileName(input: String): String {
        return input.replace(Regex("[^A-Za-z0-9ığüşöçİĞÜŞÖÇ_+]"), "")
            .take(40)
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Cagri Kaydi", NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Gorusme kaydediliyor")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
