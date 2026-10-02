package com.olegovichhh.muscriptor.engine

import android.content.Context
import android.net.Uri

data class TranscriptionResult(
    val midiPath: String,
    val musicXmlPath: String? = null
)

interface TranscriptionEngine {
    suspend fun transcribe(context: Context, audio: Uri): Result<TranscriptionResult>
}
