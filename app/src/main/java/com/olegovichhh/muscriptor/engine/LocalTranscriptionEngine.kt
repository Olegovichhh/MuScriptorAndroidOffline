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
                val modelFile = File(context.filesDir, "models/muscriptor-small-f16.gguf")
                require(modelFile.exists()) { "Model not installed: " + modelFile.absolutePath }
                val pcm = AudioDecoder.decodeMono16k(context, audio)
                val response = NativeBridge.transcribePcm16k(modelFile.absolutePath, pcm)
                check(response.startsWith("OK")) { response }
                TranscriptionResult(midiPath = "", musicXmlPath = null)
            }
        }
