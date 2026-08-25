package com.Chenkham.Echofy.ui.screens.ai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.ai.AttachedFile

/**
 * Bottom input bar for the AI Assistant screen.
 * Contains file attachment button, text input, mic button, and send button.
 * Extracted from AiAssistantScreen for modularity.
 */
@Composable
fun AiInputBar(
    textInput: String,
    onTextChange: (String) -> Unit,
    attachedFile: AttachedFile?,
    onAttachClick: () -> Unit,
    onRemoveAttachment: () -> Unit,
    onSend: () -> Unit,
    isRecording: Boolean,
    onMicClick: () -> Unit,
) {
    Surface(
        color = SurfaceContainer,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Attachment preview
            if (attachedFile != null) {
                Surface(
                    color = SurfaceContainerHighest,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "📄 ${attachedFile.fileName}",
                            style = TextStyle(fontSize = 12.sp, color = Tertiary),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = "Remove File",
                            tint = OnSurfaceVariant,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onRemoveAttachment() }
                        )
                    }
                }
            }

            // Input row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attach file
                IconButton(
                    onClick = onAttachClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add),
                        contentDescription = "Attach File",
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(4.dp))

                // Text input field
                Surface(
                    color = SurfaceContainerHighest,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    BasicTextField(
                        value = textInput,
                        onValueChange = onTextChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        textStyle = TextStyle(color = OnSurface, fontSize = 15.sp),
                        cursorBrush = SolidColor(Primary),
                        singleLine = false,
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSend() }),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (textInput.isEmpty()) {
                                    Text(
                                        "Message Echofy...",
                                        color = OnSurfaceVariant.copy(alpha = 0.5f),
                                        fontSize = 14.sp
                                    )
                                }
                                inner()
                            }
                        }
                    )
                }

                Spacer(Modifier.width(6.dp))

                // Mic button (push-to-talk)
                Surface(
                    shape = CircleShape,
                    color = if (isRecording) Color(0xFFEF4444) else SurfaceContainerHighest,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onMicClick() }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            painter = painterResource(R.drawable.mic),
                            contentDescription = if (isRecording) "Recording..." else "Voice Input",
                            tint = if (isRecording) Color.White else OnSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.width(4.dp))

                // Send button
                val canSend = textInput.isNotBlank() || attachedFile != null
                Surface(
                    shape = CircleShape,
                    color = if (canSend) Primary else SurfaceContainerHighest,
                    modifier = Modifier
                        .size(44.dp)
                        .clickable(enabled = canSend) { onSend() }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_upward),
                            contentDescription = "Send",
                            tint = if (canSend) OnPrimary else OnSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
