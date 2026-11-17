package com.example.streamviewlite

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.SystemClock
import android.view.Surface

object Media {
    fun probe(path: String): Pair<MediaFormat?, MediaFormat?> {
        val ex = MediaExtractor().apply { setDataSource(path) }
        var v: MediaFormat? = null
        var a: MediaFormat? = null
        for (i in 0 until ex.trackCount) {
            val f = ex.getTrackFormat(i)
            val m = f.getString(MediaFormat.KEY_MIME) ?: continue
            if (m.startsWith("video/") && v == null) v = f
            if (m.startsWith("audio/") && a == null) a = f
        }
        ex.release()
        return v to a
    }
}

class VideoLoop(path: String, surface: Surface, isRunning: () -> Boolean) {
    fun run() {
        val ex = MediaExtractor().apply { setDataSource(path) }
        val idx = (0 until ex.trackCount).firstOrNull { ex.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true } ?: return
        ex.selectTrack(idx)

        val decoder = MediaCodec.createDecoderByType("video/avc").apply {
            configure(ex.getTrackFormat(idx), surface, null, 0)
            start()
        }

        val info = MediaCodec.BufferInfo()
        while (isRunning()) {
            val inIdx = decoder.dequeueInputBuffer(10000)
            if (inIdx >= 0) {
                val buf = decoder.getInputBuffer(inIdx)!!
                val size = ex.readSampleData(buf, 0)
                if (size < 0) {
                    decoder.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    ex.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                } else {
                    decoder.queueInputBuffer(inIdx, 0, size, ex.sampleTime, 0)
                    ex.advance()
                }
            }
            var outIdx = decoder.dequeueOutputBuffer(info, 0)
            while (outIdx >= 0) {
                SystemClock.sleep(33) // Approx 30fps loop delay
                decoder.releaseOutputBuffer(outIdx, true)
                outIdx = decoder.dequeueOutputBuffer(info, 0)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            }
        }
        decoder.release(); ex.release()
    }
}

class AudioLoop(path: String, sink: (ByteArray, Long) -> Unit, isRunning: () -> Boolean) {
    fun run() {
        val ex = MediaExtractor().apply { setDataSource(path) }
        val idx = (0 until ex.trackCount).firstOrNull { ex.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true } ?: return
        ex.selectTrack(idx)

        val decoder = MediaCodec.createDecoderByType(ex.getTrackFormat(idx).getString(MediaFormat.KEY_MIME)).apply {
            configure(ex.getTrackFormat(idx), null, null, 0)
            start()
        }
        val info = MediaCodec.BufferInfo()
        while (isRunning()) {
            val inIdx = decoder.dequeueInputBuffer(10000)
            if (inIdx >= 0) {
                val buf = decoder.getInputBuffer(inIdx)!!
                val size = ex.readSampleData(buf, 0)
                if (size < 0) {
                    decoder.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    ex.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                } else {
                    decoder.queueInputBuffer(inIdx, 0, size, ex.sampleTime, 0)
                    ex.advance()
                }
            }
            var outIdx = decoder.dequeueOutputBuffer(info, 0)
            while (outIdx >= 0) {
                val out = decoder.getOutputBuffer(outIdx)!!
                val data = ByteArray(info.size).apply { out.get(this) }
                if (info.size > 0) sink(data, info.presentationTimeUs)
                decoder.releaseOutputBuffer(outIdx, false)
                outIdx = decoder.dequeueOutputBuffer(info, 0)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            }
        }
        decoder.release(); ex.release()
    }
}
