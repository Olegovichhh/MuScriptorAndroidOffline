package com.olegovichhh.muscriptor

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.olegovichhh.muscriptor.engine.LocalTranscriptionEngine
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { MuScriptorScreen() } }
    }
}

@Composable
private fun MuScriptorScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = remember { LocalTranscriptionEngine() }
    var audio by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf("Установите модель и выберите аудио") }
    var busy by remember { mutableStateOf(false) }
    val modelFile = remember { File(context.filesDir, "models/muscriptor-small-f16.gguf") }
    var modelReady by remember { mutableStateOf(modelFile.exists() && modelFile.length() > 150L * 1024 * 1024) }

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        audio = it
        if (it != null) status = "Аудио выбрано"
    }
    val modelPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                val displayName = context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
                }
                if (!displayName.isNullOrBlank()) {
                    require(displayName.lowercase().endsWith(".gguf")) {
                        "Выбран файл '$displayName'. Нужна модель MuScriptor .gguf"
                    }
                }
                modelFile.parentFile?.mkdirs()
                context.contentResolver.openInputStream(uri)!!.use { input ->
                    modelFile.outputStream().use { output -> input.copyTo(output) }
                }
                require(modelFile.length() > 150L * 1024 * 1024) {
                    "Файл слишком мал для MuScriptor Small GGUF"
                }
            }.onSuccess {
                modelReady = true
                status = "MuScriptor Small установлен: " + (modelFile.length() / 1024 / 1024) + " МБ"
            }.onFailure {
                modelFile.delete()
                modelReady = false
                status = "Ошибка модели: " + (it.message ?: "unknown")
            }
        }
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("MuScriptor Offline", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text("Audio → MIDI / MusicXML", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(28.dp))
            Text(if (modelReady) "✓ MuScriptor Small GGUF: " + (modelFile.length() / 1024 / 1024) + " МБ" else "Нужна MuScriptor Small GGUF (~200 MiB)")
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = { modelPicker.launch(arrayOf("application/octet-stream","*/*")) }, enabled = !busy) {
                Text(if (modelReady) "Заменить модель GGUF" else "Установить модель GGUF")
            }
            Spacer(Modifier.height(14.dp))
            Button(onClick = { audioPicker.launch(arrayOf("audio/*")) }, enabled = !busy) { Text("Выбрать аудио") }
            Spacer(Modifier.height(18.dp))
            Button(
                enabled = audio != null && modelReady && !busy,
                onClick = {
                    busy = true; status = "Транскрипция на устройстве…"
                    scope.launch {
                        val result = engine.transcribe(context, audio!!)
                        status = result.fold(
                            onSuccess = { "Готово. MIDI: " + it.midiPath },
                            onFailure = { "Ошибка: " + (it.message ?: it.javaClass.simpleName) }
                        )
                        busy = false
                    }
                }
            ) { Text(if (busy) "Обработка…" else "Транскрибировать") }
            if (busy) { Spacer(Modifier.height(16.dp)); CircularProgressIndicator() }
            Spacer(Modifier.height(18.dp))
            Text(status)
        }
    }
}
