package com.Chenkham.Echofy.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.playback.WifiJamManager
import com.Chenkham.Echofy.utils.IpCodeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiJamSheet(onDismiss: () -> Unit) {
    val playerConnection = LocalPlayerConnection.current
    val wifiJamManager = playerConnection?.service?.wifiJamManager ?: return
    
    val context = LocalContext.current
    val isHost by wifiJamManager.isHost.collectAsState()
    val isGuest by wifiJamManager.isGuest.collectAsState()
    val connectedClientsCount by wifiJamManager.connectedClientsCount.collectAsState()

    var joinCodeText by remember { mutableStateOf("") }
    
    val localIp = remember { IpCodeUtils.getLocalIpAddress(context) }
    val hostCode = remember(localIp) { localIp?.let { IpCodeUtils.ipToCode(it) } ?: "ERROR" }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp, start = 24.dp, end = 24.dp), // padding adjusted for modal
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.music_note),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Listen Together",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                "Sync playback with friends on the same WiFi network.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // ACTIVE SESSION:
            if (isHost || isGuest) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isHost) "YOU ARE HOSTING" else "YOU ARE LISTENING",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        if (isHost) {
                            Text(
                                "Ask your friends to join using this code:",
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = hostCode,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                            Text(
                                text = "$connectedClientsCount friend(s) connected",
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Text(
                                "Syncing perfectly with host.",
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        wifiJamManager.stopAll()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Disconnect & Leave Jam")
                }
                return@Column
            }

            // NOT IN A SESSION YET:
            
            // Host Section
            Button(
                onClick = { wifiJamManager.startHosting() },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(painterResource(R.drawable.add), contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Host a Together Session")
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(" OR ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(modifier = Modifier.weight(1f))
            }
            
            // Join Section
            OutlinedTextField(
                value = joinCodeText,
                onValueChange = { joinCodeText = it.uppercase() },
                label = { Text("Enter Join Code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = {
                    val ip = IpCodeUtils.codeToIp(joinCodeText)
                    if (ip != null) {
                        wifiJamManager.joinJam(ip)
                    }
                },
                enabled = joinCodeText.length >= 4,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Join Friend's Jam")
            }
        }
    }
}
