package com.Chenkham.Echofy.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.utils.LyricsTranslationService
import kotlinx.coroutines.launch

/**
 * Translation button for lyrics
 */
@Composable
fun LyricsTranslateButton(
    lyricsText: String,
    onTranslated: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    var isTranslating by remember { mutableStateOf(false) }
    var translationError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    Box(modifier = modifier) {
        IconButton(
            onClick = { showLanguageDialog = true },
            enabled = !isTranslating
        ) {
            if (isTranslating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.translate),
                    contentDescription = "Translate lyrics",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    
    // Language selection dialog
    if (showLanguageDialog) {
        LanguagePickerDialog(
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { langCode ->
                showLanguageDialog = false
                isTranslating = true
                translationError = null
                
                scope.launch {
                    LyricsTranslationService.translate(lyricsText, langCode)
                        .onSuccess { translated ->
                            onTranslated(translated)
                        }
                        .onFailure { e ->
                            translationError = e.message ?: "Translation failed"
                        }
                    isTranslating = false
                }
            }
        )
    }
    
    // Error toast/snackbar could be added here
}

/**
 * Language picker dialog for translation
 */
@Composable
fun LanguagePickerDialog(
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    var selectedLang by remember { mutableStateOf("en") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Translate To",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn {
                items(LyricsTranslationService.supportedLanguages) { (code, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLang = code }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedLang == code,
                            onClick = { selectedLang = code }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onLanguageSelected(selectedLang) }
            ) {
                Text("Translate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Translated lyrics display section
 */
@Composable
fun TranslatedLyricsSection(
    originalLyrics: String,
    translatedLyrics: String?,
    showTranslation: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Toggle button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (translatedLyrics != null) {
                TextButton(onClick = onToggle) {
                    Text(
                        text = if (showTranslation) "Show Original" else "Show Translation"
                    )
                }
            }
        }
        
        // Display lyrics
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Text(
                text = if (showTranslation && translatedLyrics != null) 
                    translatedLyrics 
                else 
                    originalLyrics,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}
