package com.Chenkham.Echofy.ui.screens.ai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.ai.AiPresets
import com.Chenkham.Echofy.ai.AiProfile

/**
 * Dialog for managing multi-provider AI profile settings.
 * Restructured with clear preset/custom separation, active provider indication,
 * and secure API key handling.
 */
@Composable
fun MultiProviderSettingsDialog(
    profiles: List<AiProfile>,
    activeProfileId: String,
    onDismiss: () -> Unit,
    onSaveProfiles: (updatedList: List<AiProfile>, activeId: String) -> Unit
) {
    var profilesList by remember { mutableStateOf(profiles.toMutableList()) }
    var currentActiveId by remember { mutableStateOf(activeProfileId) }
    var editingProfile by remember { mutableStateOf<AiProfile?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainerHigh,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                "AI Providers",
                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnSurface)
            )
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth().height(480.dp)) {
                if (editingProfile != null) {
                    ProviderEditForm(
                        profile = editingProfile!!,
                        onCancel = { editingProfile = null },
                        onSave = { updated ->
                            val idx = profilesList.indexOfFirst { it.id == editingProfile!!.id }
                            if (idx >= 0) {
                                profilesList[idx] = updated
                            } else {
                                profilesList.add(updated)
                            }
                            editingProfile = null
                        },
                        onDelete = if (AiPresets.BUILTIN_PROFILES.none { it.id == editingProfile!!.id }) {
                            {
                                profilesList.removeAll { it.id == editingProfile!!.id }
                                if (currentActiveId == editingProfile!!.id) {
                                    currentActiveId = AiPresets.BUILTIN_PROFILES.first().id
                                }
                                editingProfile = null
                            }
                        } else null
                    )
                } else {
                    ProvidersListView(
                        profilesList = profilesList,
                        currentActiveId = currentActiveId,
                        onSelectActive = { currentActiveId = it },
                        onEditProfile = { editingProfile = it },
                        onAddCustom = {
                            editingProfile = AiProfile(
                                name = "Custom Provider",
                                baseUrl = "https://api.openai.com/v1",
                                modelName = "gpt-4o-mini",
                                apiKeysRaw = ""
                            )
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveProfiles(profilesList, currentActiveId) },
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save & Apply", color = OnPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Text("Cancel", color = OnSurfaceVariant, fontSize = 14.sp)
            }
        }
    )
}

@Composable
private fun ProvidersListView(
    profilesList: List<AiProfile>,
    currentActiveId: String,
    onSelectActive: (String) -> Unit,
    onEditProfile: (AiProfile) -> Unit,
    onAddCustom: () -> Unit
) {
    val presetProfiles = profilesList.filter { profile ->
        AiPresets.BUILTIN_PROFILES.any { it.id == profile.id }
    }
    val customProfiles = profilesList.filter { profile ->
        AiPresets.BUILTIN_PROFILES.none { it.id == profile.id }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            "Select your AI provider and configure API keys. All keys are stored securely on your device.",
            style = TextStyle(fontSize = 13.sp, color = OnSurfaceVariant, lineHeight = 18.sp),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Preset Providers Section
        Text(
            "Preset Providers",
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Primary),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        presetProfiles.forEach { profile ->
            ProviderCard(
                profile = profile,
                isActive = profile.id == currentActiveId,
                onClick = { onSelectActive(profile.id) },
                onEdit = { onEditProfile(profile) }
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(16.dp))

        // Custom Providers Section
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Custom Providers",
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Primary)
            )
            IconButton(
                onClick = onAddCustom,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.add),
                    contentDescription = "Add custom provider",
                    tint = Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (customProfiles.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.cloud_lock),
                        contentDescription = null,
                        tint = OnSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No custom providers yet",
                        style = TextStyle(fontSize = 13.sp, color = OnSurfaceVariant, fontWeight = FontWeight.Medium)
                    )
                    Text(
                        "Add any OpenAI-compatible API",
                        style = TextStyle(fontSize = 11.sp, color = OnSurfaceVariant.copy(alpha = 0.7f))
                    )
                }
            }
        } else {
            customProfiles.forEach { profile ->
                ProviderCard(
                    profile = profile,
                    isActive = profile.id == currentActiveId,
                    onClick = { onSelectActive(profile.id) },
                    onEdit = { onEditProfile(profile) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ProviderCard(
    profile: AiProfile,
    isActive: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    val hasApiKey = profile.apiKeysRaw.isNotBlank()
    val apiKeyStatus = if (hasApiKey) "API key set" else "No API key"

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) PrimaryContainer else SurfaceContainerHighest
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        profile.name,
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isActive) Primary else OnSurface
                        )
                    )
                    if (isActive) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(R.drawable.check_circle),
                            contentDescription = "Active",
                            tint = Tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    profile.modelName,
                    style = TextStyle(fontSize = 12.sp, color = OnSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    apiKeyStatus,
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = if (hasApiKey) Tertiary.copy(alpha = 0.8f) else OnSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            }

            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.edit),
                    contentDescription = "Edit provider",
                    tint = if (isActive) Primary else OnSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ProviderEditForm(
    profile: AiProfile,
    onCancel: () -> Unit,
    onSave: (AiProfile) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(profile.name) }
    var baseUrl by remember { mutableStateOf(profile.baseUrl) }
    var modelName by remember { mutableStateOf(profile.modelName) }
    var apiKeysRaw by remember { mutableStateOf(profile.apiKeysRaw) }
    var showApiKey by remember { mutableStateOf(false) }

    val isValid = name.isNotBlank() && baseUrl.isNotBlank() && modelName.isNotBlank()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            if (AiPresets.BUILTIN_PROFILES.any { it.id == profile.id }) "Configure Provider" else "Edit Custom Provider",
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Primary),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Name Field
        Text("Provider Name", style = TextStyle(fontSize = 12.sp, color = OnSurfaceVariant, fontWeight = FontWeight.Medium))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("e.g., My Custom AI", fontSize = 13.sp, color = OnSurfaceVariant.copy(alpha = 0.5f)) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = OnSurface,
                unfocusedTextColor = OnSurface,
                focusedBorderColor = Primary,
                unfocusedBorderColor = Outline,
                cursorColor = Primary
            ),
            textStyle = TextStyle(fontSize = 14.sp),
            modifier = Modifier.fillMaxWidth()
        )

        // Base URL Field
        Text("Base URL", style = TextStyle(fontSize = 12.sp, color = OnSurfaceVariant, fontWeight = FontWeight.Medium))
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            placeholder = { Text("https://api.example.com/v1", fontSize = 13.sp, color = OnSurfaceVariant.copy(alpha = 0.5f)) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = OnSurface,
                unfocusedTextColor = OnSurface,
                focusedBorderColor = Primary,
                unfocusedBorderColor = Outline,
                cursorColor = Primary
            ),
            textStyle = TextStyle(fontSize = 14.sp),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "OpenAI-compatible API endpoint",
            style = TextStyle(fontSize = 11.sp, color = OnSurfaceVariant.copy(alpha = 0.7f)),
            modifier = Modifier.padding(top = 2.dp)
        )

        // Model Name Field
        Text("Model Name", style = TextStyle(fontSize = 12.sp, color = OnSurfaceVariant, fontWeight = FontWeight.Medium))
        OutlinedTextField(
            value = modelName,
            onValueChange = { modelName = it },
            placeholder = { Text("e.g., gpt-4o-mini", fontSize = 13.sp, color = OnSurfaceVariant.copy(alpha = 0.5f)) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = OnSurface,
                unfocusedTextColor = OnSurface,
                focusedBorderColor = Primary,
                unfocusedBorderColor = Outline,
                cursorColor = Primary
            ),
            textStyle = TextStyle(fontSize = 14.sp),
            modifier = Modifier.fillMaxWidth()
        )

        // API Key Field with visibility toggle
        Text("API Key", style = TextStyle(fontSize = 12.sp, color = OnSurfaceVariant, fontWeight = FontWeight.Medium))
        OutlinedTextField(
            value = apiKeysRaw,
            onValueChange = { apiKeysRaw = it },
            placeholder = { Text("Enter your API key", fontSize = 13.sp, color = OnSurfaceVariant.copy(alpha = 0.5f)) },
            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showApiKey = !showApiKey }) {
                    Icon(
                        painter = painterResource(if (showApiKey) R.drawable.visibility_off else R.drawable.visibility),
                        contentDescription = if (showApiKey) "Hide API key" else "Show API key",
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = OnSurface,
                unfocusedTextColor = OnSurface,
                focusedBorderColor = Primary,
                unfocusedBorderColor = Outline,
                cursorColor = Primary
            ),
            textStyle = TextStyle(fontSize = 14.sp),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Multiple keys can be separated by newlines or commas",
            style = TextStyle(fontSize = 11.sp, color = OnSurfaceVariant.copy(alpha = 0.7f)),
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(Modifier.height(8.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onDelete != null) {
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.delete),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Delete", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHighest),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel", color = OnSurfaceVariant, fontSize = 13.sp)
            }
            Button(
                onClick = {
                    val updated = profile.copy(
                        name = name.trim(),
                        baseUrl = baseUrl.trim(),
                        modelName = modelName.trim(),
                        apiKeysRaw = apiKeysRaw
                    )
                    onSave(updated)
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    disabledContainerColor = Primary.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "Save",
                    color = if (isValid) SurfaceDark else OnSurfaceVariant.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
