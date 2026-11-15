package com.Chenkham.Echofy.ui.component

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JamInviteBottomSheet(
    roomCode: String,
    inviteLink: String?,
    context: Context,
    clipboardManager: ClipboardManager,
    onDismissRequest: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF121212),
    ) {
        InviteJamSheetContent(
            roomCode = roomCode,
            inviteLink = inviteLink,
            onCopyCode = {
                clipboardManager.setText(AnnotatedString(roomCode))
                Toast.makeText(context, "Room code copied", Toast.LENGTH_SHORT).show()
            },
            onShare = {
                val shareText = buildInviteShareText(roomCode, inviteLink)
                val shareIntent =
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                context.startActivity(Intent.createChooser(shareIntent, "Share Jam invite"))
            },
        )
    }
}
