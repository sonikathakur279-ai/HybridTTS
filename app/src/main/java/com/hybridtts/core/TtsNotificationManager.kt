package com.hybridtts.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.hybridtts.MainActivity

class TtsNotificationManager private constructor(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val CHANNEL_ID = "hybrid_tts_generation_channel"
        private const val NOTIFICATION_ID_COMPLETE = 1001

        @Volatile
        private var instance: TtsNotificationManager? = null

        fun getInstance(context: Context): TtsNotificationManager {
            return instance ?: synchronized(this) {
                instance ?: TtsNotificationManager(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "HybridTTS Audio Generation",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when background speech synthesis completes."
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun notifySynthesisComplete(voiceName: String, durationSec: Double) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val durationFormatted = String.format(java.util.Locale.US, "%.1fs", durationSec)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Audio Master Ready ($durationFormatted)")
            .setContentText("Synthesized successfully via $voiceName. Tap to play.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIFICATION_ID_COMPLETE, notification)
    }
}
