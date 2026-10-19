package com.bhanuagrawal.kmplab.ui.upload

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bhanuagrawal.kmplab.uploader.ChunkState
import com.bhanuagrawal.kmplab.uploader.UploadStatus

@Composable
fun UploadScreen(viewModel: UploadViewModel = viewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Upload test", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = ui.uploadUrl,
            onValueChange = viewModel::setUrl,
            label = { Text("Upload URL") },
            modifier = Modifier.fillMaxWidth(),
        )

        val filePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri -> if (uri != null) viewModel.onFilePicked(uri) }

        val imagePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
        ) { uri -> if (uri != null) viewModel.onFilePicked(uri) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { filePicker.launch("*/*") }) { Text("Pick file") }
            OutlinedButton(onClick = {
                imagePicker.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageAndVideo
                    )
                )
            }) { Text("Pick image/video") }
        }

        HorizontalDivider()
        Text("…or generate a test file:")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { viewModel.generateTestFile(1 * 1024 * 1024) }) { Text("1 MB") }
            OutlinedButton(onClick = { viewModel.generateTestFile(5 * 1024 * 1024) }) { Text("5 MB") }
            OutlinedButton(onClick = { viewModel.generateTestFile(20 * 1024 * 1024) }) { Text("20 MB") }
        }

        ui.filePath?.let { path ->
            ui.fileName?.let { Text("Name: $it") }
            Text(
                "Path: $path",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
            Text("Size: ${formatBytes(ui.fileSizeBytes)}")
        }

        val isInFlight = ui.state?.uploadStatus == UploadStatus.INPROGRESS ||
            ui.state?.uploadStatus == UploadStatus.PENDING

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::upload) { Text("Upload") }
            Button(
                onClick = viewModel::cancel,
                enabled = ui.uploadId != null && isInFlight,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Cancel")
            }
        }

        ui.message?.let { Text("→ $it") }

        ui.state?.let { state ->
            Spacer(Modifier.height(8.dp))
            val statusColor = when (state.uploadStatus) {
                UploadStatus.SUCCESS -> MaterialTheme.colorScheme.primary
                UploadStatus.CANCELLED, UploadStatus.FAILED -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            }
            Text(
                "Status: ${state.uploadStatus}",
                style = MaterialTheme.typography.titleMedium,
                color = statusColor,
            )
            Text("Bytes: ${state.uploadedSize} / ${state.totalSize}")
            val pct = if (state.totalSize > 0) state.uploadedSize.toFloat() / state.totalSize else 0f
            LinearProgressIndicator(progress = { pct.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            Text("${(pct * 100).toInt()}%")

            if (state.totalChunks > 0) {
                Spacer(Modifier.height(8.dp))
                Text("Chunks: ${state.completedChunks} / ${state.totalChunks} done")
                state.chunksState.values
                    .sortedBy { it.chunkOffset }
                    .forEachIndexed { index, chunk ->
                        ChunkRow(index, chunk)
                    }
            }
        }
    }
}

@Composable
private fun ChunkRow(index: Int, chunk: ChunkState) {
    val p = if (chunk.size > 0) chunk.uploadedSize.toFloat() / chunk.size else 0f
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            "Chunk #$index  offset=${chunk.chunkOffset}  size=${chunk.size}  ${chunk.status}",
            style = MaterialTheme.typography.bodySmall,
        )
        LinearProgressIndicator(
            progress = { p.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun formatBytes(b: Long): String = when {
    b < 1024 -> "$b B"
    b < 1024 * 1024 -> "${b / 1024} KB"
    else -> "${b / (1024 * 1024)} MB"
}
