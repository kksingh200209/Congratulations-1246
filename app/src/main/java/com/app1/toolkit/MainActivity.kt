package com.app1.toolkit

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App1Home() }
    }
}

private data class AppTool(val title: String, val subtitle: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun App1Home() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Choose a tool to get started") }
    var busy by remember { mutableStateOf(false) }
    var action by remember { mutableStateOf("compress") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            busy = true
            scope.launch {
                status = try {
                    withContext(Dispatchers.IO) {
                        if (action == "compress") compressImage(context, uri) else imageToPdf(context, uri)
                    }
                } catch (e: Exception) {
                    "Could not process file: ${e.localizedMessage ?: "unknown error"}"
                }
                busy = false
            }
        }
    }

    val tools = listOf(
        AppTool("Image Compressor", "Reduce image size", Icons.Default.Compress),
        AppTool("Image to PDF", "Convert photo to PDF", Icons.Default.PictureAsPdf),
        AppTool("PDF Merge", "Combine PDF files", Icons.Default.MergeType),
        AppTool("PDF Split", "Extract PDF pages", Icons.Default.CallSplit),
        AppTool("PDF Compressor", "Reduce PDF size", Icons.Default.FolderZip),
        AppTool("OCR Scanner", "Extract text from images", Icons.Default.DocumentScanner)
    )

    Scaffold(topBar = { TopAppBar(title = { Text("App1") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Documents, simplified.", style = MaterialTheme.typography.headlineSmall)
            Text("Private tools that work on your device.")
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(tools) { tool ->
                    Card(onClick = {
                        when (tool.title) {
                            "Image Compressor" -> { action = "compress"; picker.launch("image/*") }
                            "Image to PDF" -> { action = "pdf"; picker.launch("image/*") }
                            else -> status = "${tool.title} is planned for a future update."
                        }
                    }) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(tool.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(tool.title, style = MaterialTheme.typography.titleMedium)
                            Text(tool.subtitle, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(status, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun decodeBitmap(context: Context, uri: Uri): Bitmap =
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        ?: error("Unsupported or unreadable image")

private fun compressImage(context: Context, uri: Uri): String {
    val bitmap = decodeBitmap(context, uri)
    try {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "App1_compressed_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/App1")
        }
        val output = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create output file")
        context.contentResolver.openOutputStream(output)?.use {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 65, it)) error("Compression failed")
        } ?: error("Could not write output")
        return "Compressed image saved in Pictures/App1."
    } finally { bitmap.recycle() }
}

private fun imageToPdf(context: Context, uri: Uri): String {
    val bitmap = decodeBitmap(context, uri)
    val pdf = PdfDocument()
    try {
        val info = PdfDocument.PageInfo.Builder(bitmap.width.coerceAtLeast(1), bitmap.height.coerceAtLeast(1), 1).create()
        val page = pdf.startPage(info)
        page.canvas.drawBitmap(bitmap, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
        pdf.finishPage(page)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "App1_${System.currentTimeMillis()}.pdf")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/App1")
        }
        val output = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), values)
            ?: error("Could not create PDF")
        context.contentResolver.openOutputStream(output)?.use { pdf.writeTo(it) }
            ?: error("Could not write PDF")
        return "PDF saved in Documents/App1."
    } finally {
        pdf.close()
        bitmap.recycle()
    }
}
