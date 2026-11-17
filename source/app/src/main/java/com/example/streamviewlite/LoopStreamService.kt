package com.example.streamviewlite

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.pedro.common.ConnectChecker

class LoopStreamService : Service(), ConnectChecker {

    companion object {
        private const val CHANNEL_ID = "stream_channel"
        private const val NOTIF_ID = 1
    }

    private var bridge: RtmpBridge? = null
    private var running = true

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(CHANNEL_ID, "StreamView Engine", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        
        val note = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("StreamView Active")
            .setContentText("Streaming fake camera loop...")
            .setSmallIcon(android.R.drawable.presence_video_online).build()
            
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, note, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIF_ID, note)
        }

        Thread {
            val repo = SettingsRepository(this)
            val settings = kotlinx.coroutines.runBlocking { repo.settings.first() }
            val endpoint = "${settings.rtmpUrl}/${settings.streamKey}"

            val file = File(filesDir, "fake_cam.mp4")
            if (!file.exists()) { stopSelf(); return@Thread }

            val ctx = this
            val b = RtmpBridge(object : ConnectChecker {
                override fun onConnectionSuccess(){ }
                override fun onConnectionFailed(reason: String){ android.util.Log.e("Stream", reason) }
                override fun onDisconnect(){ android.util.Log.i("Stream", "Disconnected") }
                // ... other required stubs from ConnectChecker
                override fun onAuthSuccess() {}
                override fun onAuthError() {}
            })

            val (videoFmt, audioFmt) = Media.probe(file.absolutePath)
            if (videoFmt != null) {
                val w = videoFmt.getInteger(android.media.MediaFormat.KEY_WIDTH)
                val h = videoFmt.getInteger(android.media.MediaFormat.KEY_HEIGHT)
                val fps = 30 // simplify
                b.prepareVideo(w, h, fps, 2000000)
                
                VideoLoop(file.absolutePath, b.videoInputSurface()) { running }.run()
                if (audioFmt != null) {
                    b.prepareAudio(44100, true, 128000)
                    AudioLoop(file.absolutePath, { p, t -> b.sendAudioPcm(p, t) }) { running }.run()
                }
                b.start(endpoint)
            }
            if (!running) b.stop()
            stopSelf()
        }.start()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onAuthSuccess() {}
    override fun onAuthError() {}
    override fun onConnectionSuccess() {}
    override fun onConnectionFailed(reason: String) {}
    override fun onDisconnect() {}
}
