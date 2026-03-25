package com.Chenkham.Echofy.ui.screens.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import com.Chenkham.Echofy.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Chenkham.Echofy.viewmodels.OnboardingViewModel

data class Language(
    val code: String,
    val name: String,
    val nativeName: String
)

@Composable
fun LanguageSelectionStep(viewModel: OnboardingViewModel) {
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()

    val languages = remember {
        listOf(
            Language("en", "English", "English"),
            Language("es", "Spanish", "Español"),
            Language("fr", "French", "Français"),
            Language("de", "German", "Deutsch"),
            Language("it", "Italian", "Italiano"),
            Language("pt", "Portuguese", "Português"),
            Language("hi", "Hindi", "हिन्दी"),
            Language("ja", "Japanese", "日本語"),
            Language("ko", "Korean", "한국어"),
            Language("zh", "Chinese", "中文"),
            Language("ar", "Arabic", "العربية"),
            Language("ru", "Russian", "Русский"),
            Language("tr", "Turkish", "Türkçe"),
            Language("pl", "Polish", "Polski"),
            Language("nl", "Dutch", "Nederlands"),
            Language("sv", "Swedish", "Svenska"),
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Choose Your Language",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Select your preferred language for music content",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(languages, key = { it.code }) { language ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectLanguage(language.code) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedLanguage == language.code)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = language.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = language.nativeName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (selectedLanguage == language.code) {
                            Icon(
                                painter = painterResource(R.drawable.check),
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
