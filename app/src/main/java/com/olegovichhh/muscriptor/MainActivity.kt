package com.olegovichhh.muscriptor

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { MuScriptorScreen() } }
    }
}

@Composable
private fun MuScriptorScreen() {
    var audio by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf("Выберите аудиофайл") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        audio = it
        status = if (it == null) "Файл не выбран" else "Аудио готово к локальной транскрипции"
    }
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("MuScriptor Offline", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(12.dp))
            Text("Audio → MIDI / MusicXML", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(32.dp))
            Button(onClick = { picker.launch(arrayOf("audio/*")) }) { Text("Выбрать аудио") }
            Spacer(Modifier.height(16.dp))
            Text(status)
            Spacer(Modifier.height(24.dp))
            Button(onClick = { status = if (audio == null) "Сначала выберите аудио" else "Подготовка офлайн-движка…" },
                enabled = audio != null) { Text("Транскрибировать") }
        }
    }
}
