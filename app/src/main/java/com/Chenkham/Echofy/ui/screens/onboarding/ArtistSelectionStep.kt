package com.Chenkham.Echofy.ui.screens.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import com.Chenkham.Echofy.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.Chenkham.Echofy.viewmodels.OnboardingViewModel

data class Artist(
    val id: String,
    val name: String,
    val genre: String
)

@Composable
fun ArtistSelectionStep(viewModel: OnboardingViewModel) {
    val selectedArtists by viewModel.selectedArtists.collectAsState()

    val popularArtists = remember {
        listOf(
            Artist("taylorswift", "Taylor Swift", "Pop"),
            Artist("theweeknd", "The Weeknd", "R&B"),
            Artist("drake", "Drake", "Hip-Hop"),
            Artist("arianagrande", "Ariana Grande", "Pop"),
            Artist("edsheeran", "Ed Sheeran", "Pop"),
            Artist("billieeilish", "Billie Eilish", "Alternative"),
            Artist("postmalone", "Post Malone", "Hip-Hop"),
            Artist("imaginedragons", "Imagine Dragons", "Rock"),
            Artist("coldplay", "Coldplay", "Rock"),
            Artist("dualipa", "Dua Lipa", "Pop"),
            Artist("juicewrld", "Juice WRLD", "Hip-Hop"),
            Artist("travisscott", "Travis Scott", "Hip-Hop"),
            Artist("badbunny", "Bad Bunny", "Latin"),
            Artist("bts", "BTS", "K-Pop"),
            Artist("adele", "Adele", "Pop"),
            Artist("justinbieber", "Justin Bieber", "Pop"),
            Artist("shawnmendes", "Shawn Mendes", "Pop"),
            Artist("oliviarodrigo", "Olivia Rodrigo", "Pop"),
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Pick Your Favorite Artists",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Select at least 1 artist you love",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${selectedArtists.size} selected",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(popularArtists, key = { it.id }) { artist ->
                val isSelected = selectedArtists.contains(artist.id)

                Card(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { viewModel.toggleArtist(artist.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isSelected) 8.dp else 2.dp
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = artist.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = artist.genre,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        if (isSelected) {
                            Icon(
                                painter = painterResource(R.drawable.check),
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
