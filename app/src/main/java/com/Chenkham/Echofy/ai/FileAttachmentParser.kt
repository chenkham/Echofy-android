package com.Chenkham.Echofy.ai

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class AttachedFile(
    val uri: Uri,
    val fileName: String,
    val textContent: String
)

object FileAttachmentParser {

    suspend fun parseFile(context: Context, uri: Uri): AttachedFile = withContext(Dispatchers.IO) {
        var name = "Document"
        var content = ""

        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex("_display_name")
                    if (nameIndex >= 0) {
                        name = it.getString(nameIndex) ?: "Document"
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val reader = BufferedReader(InputStreamReader(inputStream))
                content = reader.readText().take(5000)
                reader.close()
                inputStream.close()
            }
        } catch (_: Exception) {
            content = "[Could not extract raw text from file]"
        }

        AttachedFile(uri = uri, fileName = name, textContent = content)
    }
}
