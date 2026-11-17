package com.example.streamviewlite

import android.view.Surface
import com.pedro.library.rtmp.RtmpStream
import com.pedro.common.ConnectChecker

class RtmpBridge(private val checker: ConnectChecker) {
    private val stream = RtmpStream(checker)

    fun prepareVideo(width: Int, height: Int, fps: Int, bitrate: Int) {
        stream.prepareVideo(width, height, fps, bitrate, 2, 0)
    }
    
    fun prepareAudio(sampleRate: Int, stereo: Boolean, bitrate: Int) {
        stream.prepareAudio(sampleRate, stereo, bitrate)
    }

    // Gets the Surface that the video decodes into
    fun videoInputSurface(): Surface = stream.videoEncoder.inputSurface

    // Sends Audio PCM
    fun sendAudioPcm(pcm: ByteArray, pts: Long) {
        if (pcm.isNotEmpty()) {
            stream.audioEncoder.inputAudioFrame(pcm, pts)
        }
    }

    fun start(endpoint: String) = stream.startStream(endpoint)
    fun stop() = stream.stopStream()
}
