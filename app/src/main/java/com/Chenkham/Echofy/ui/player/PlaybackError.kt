package com.Chenkham.Echofy.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.PlaybackException
import com.Chenkham.Echofy.R
import kotlinx.coroutines.delay

@Composable
fun PlaybackError(
    error: PlaybackException,
    retry: () -> Unit,
) {
    var countdown by remember { mutableIntStateOf(3) }
    
    // Auto-retry after countdown
    LaunchedEffect(error) {
        countdown = 3
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        retry()
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.info),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(32.dp)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = stringResource(R.string.error_playback),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = error.cause?.cause?.message 
                ?: error.cause?.message 
                ?: stringResource(R.string.error_unknown),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { 
                countdown = 0
                retry() 
            },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.refresh),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (countdown > 0) 
                        stringResource(R.string.retry_in_seconds, countdown)
                    else 
                        stringResource(R.string.retry)
                )
            }
        }
    }
}
