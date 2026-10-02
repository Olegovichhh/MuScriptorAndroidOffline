package com.olegovichhh.muscriptor.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteOrder
import kotlin.math.floor

object AudioDecoder {
    fun decodeMono16k(context: Context, uri: Uri): FloatArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)
        var track = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) { track = i; format = f; break }
        }
        require(track >= 0 && format != null) { "No audio track" }
        extractor.selectTrack(track)
        val mime = format!!.getString(MediaFormat.KEY_MIME)!!
        val sampleRate = format!!.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channels = format!!.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0); codec.start()
        val mono = ArrayList<Float>()
        val info = MediaCodec.BufferInfo()
        var inputDone = false; var outputDone = false
        while (!outputDone) {
            if (!inputDone) {
                val index = codec.dequeueInputBuffer(10_000)
                if (index >= 0) {
                    val buffer = codec.getInputBuffer(index)!!
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) { codec.queueInputBuffer(index,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM); inputDone=true }
                    else { codec.queueInputBuffer(index,0,size,extractor.sampleTime,0); extractor.advance() }
                }
            }
            val outIndex = codec.dequeueOutputBuffer(info, 10_000)
            if (outIndex >= 0) {
                val buffer = codec.getOutputBuffer(outIndex)!!.order(ByteOrder.LITTLE_ENDIAN)
                while (buffer.remaining() >= 2 * channels) {
                    var sum=0f; repeat(channels) { sum += buffer.short / 32768f }; mono.add(sum/channels)
                }
                outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                codec.releaseOutputBuffer(outIndex,false)
            }
        }
        codec.stop(); codec.release(); extractor.release()
        if (sampleRate == 16000) return mono.toFloatArray()
        val ratio=sampleRate/16000.0
        return FloatArray(floor(mono.size/ratio).toInt()) { i ->
            val p=i*ratio; val a=floor(p).toInt().coerceAtMost(mono.lastIndex); val b=(a+1).coerceAtMost(mono.lastIndex)
            val t=(p-a).toFloat(); mono[a]*(1f-t)+mono[b]*t
        }
    }
}
