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

data class Country(
    val code: String,
    val name: String,
    val flag: String
)

@Composable
fun CountrySelectionStep(viewModel: OnboardingViewModel) {
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val countries = remember {
        listOf(
            Country("US", "United States", "🇺🇸"),
            Country("GB", "United Kingdom", "🇬🇧"),
            Country("IN", "India", "🇮🇳"),
            Country("CA", "Canada", "🇨🇦"),
            Country("AU", "Australia", "🇦🇺"),
            Country("DE", "Germany", "🇩🇪"),
            Country("FR", "France", "🇫🇷"),
            Country("JP", "Japan", "🇯🇵"),
            Country("BR", "Brazil", "🇧🇷"),
            Country("MX", "Mexico", "🇲🇽"),
            Country("ES", "Spain", "🇪🇸"),
            Country("IT", "Italy", "🇮🇹"),
            Country("KR", "South Korea", "🇰🇷"),
            Country("NL", "Netherlands", "🇳🇱"),
            Country("SE", "Sweden", "🇸🇪"),
            Country("PL", "Poland", "🇵🇱"),
            Country("AR", "Argentina", "🇦🇷"),
            Country("PH", "Philippines", "🇵🇭"),
            Country("TH", "Thailand", "🇹🇭"),
            Country("VN", "Vietnam", "🇻🇳"),
        )
    }

    val filteredCountries = remember(searchQuery) {
        if (searchQuery.isEmpty()) {
            countries
        } else {
            countries.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Select Your Country",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "This helps us personalize your music experience",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search countries") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredCountries, key = { it.code }) { country ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectCountry(country.code) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedCountry == country.code)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = country.flag,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = country.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        if (selectedCountry == country.code) {
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
