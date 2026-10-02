package com.olegovichhh.muscriptor.engine

import android.content.Context
import android.net.Uri
import com.olegovichhh.muscriptor.audio.AudioDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalTranscriptionEngine : TranscriptionEngine {
    override suspend fun transcribe(context: Context, audio: Uri): Result<TranscriptionResult> =
        withContext(Dispatchers.Default) {
            runCatching {
                val modelFile=File(context.filesDir,"models/muscriptor-small-f16.gguf")
                require(modelFile.exists()) { "Model not installed" }
                val pcm=AudioDecoder.decodeMono16k(context,audio)
                val midi=NativeBridge.transcribePcm16k(modelFile.absolutePath,pcm)
                    ?: error("Native transcription failed")
                val outDir=File(context.getExternalFilesDir(null),"transcriptions").apply{mkdirs()}
                val out=File(outDir,"transcription_"+System.currentTimeMillis()+".mid")
                out.writeBytes(midi)
                TranscriptionResult(midiPath=out.absolutePath)
            }
        }
}
