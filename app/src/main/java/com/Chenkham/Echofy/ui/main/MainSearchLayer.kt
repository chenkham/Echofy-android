package com.Chenkham.Echofy.ui.main

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.MiniPlayerHeight
import com.Chenkham.Echofy.constants.SearchSource
import com.Chenkham.Echofy.ui.component.IconButton
import com.Chenkham.Echofy.ui.component.TopSearch
import com.Chenkham.Echofy.ui.screens.Screens
import com.Chenkham.Echofy.ui.screens.search.LocalSearchScreen
import com.Chenkham.Echofy.ui.screens.search.OnlineSearchScreen
import com.Chenkham.Echofy.ui.utils.backToMain
import java.net.URLEncoder

/**
 * TopSearch bar and overlay for live online/local queries, voice search, and results.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSearchLayer(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onSearch: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    navController: NavHostController,
    navBackStackEntry: NavBackStackEntry?,
    navigationItems: List<Screens>,
    searchSource: SearchSource,
    onSearchSourceChange: (SearchSource) -> Unit,
    focusRequester: FocusRequester,
    pureBlack: Boolean,
    playerBottomSheetDismissed: Boolean,
    modifier: Modifier = Modifier,
) {
    TopSearch(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = onSearch,
        active = active,
        onActiveChange = onActiveChange,
        placeholder = {
            Text(
                text =
                    stringResource(
                        when (searchSource) {
                            SearchSource.LOCAL -> R.string.search_library
                            SearchSource.ONLINE -> R.string.search_yt_music
                        },
                    ),
            )
        },
        leadingIcon = {
            IconButton(
                onClick = {
                    when {
                        active -> onActiveChange(false)
                        !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                            navController.navigateUp()
                        }
                        else -> onActiveChange(true)
                    }
                },
                onLongClick = {
                    when {
                        active -> {}
                        !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                            navController.backToMain()
                        }
                        else -> {}
                    }
                },
            ) {
                Icon(
                    painterResource(
                        if (active || !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                            R.drawable.arrow_back
                        } else {
                            R.drawable.search
                        },
                    ),
                    contentDescription = null,
                )
            }
        },
        trailingIcon = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (active) {
                    if (query.text.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange(TextFieldValue("")) }) {
                            Icon(painter = painterResource(R.drawable.close), contentDescription = null)
                        }
                    }

                    val voiceSearchLauncher =
                        rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartActivityForResult(),
                        ) { result ->
                            if (result.resultCode == Activity.RESULT_OK) {
                                val spokenText =
                                    result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
                                spokenText?.let { text ->
                                    onQueryChange(TextFieldValue(text, TextRange(text.length)))
                                    onSearch(text)
                                }
                            }
                        }

                    IconButton(
                        onClick = {
                            val intent =
                                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Search for music...")
                                }
                            try {
                                voiceSearchLauncher.launch(intent)
                            } catch (_: Exception) {
                            }
                        },
                    ) {
                        Icon(painter = painterResource(R.drawable.mic), contentDescription = "Voice search")
                    }

                    IconButton(
                        onClick = {
                            onSearchSourceChange(
                                if (searchSource == SearchSource.ONLINE) SearchSource.LOCAL else SearchSource.ONLINE,
                            )
                        },
                    ) {
                        Icon(
                            painter =
                                painterResource(
                                    when (searchSource) {
                                        SearchSource.LOCAL -> R.drawable.library_music
                                        SearchSource.ONLINE -> R.drawable.language
                                    },
                                ),
                            contentDescription =
                                stringResource(
                                    when (searchSource) {
                                        SearchSource.LOCAL -> R.string.search_online
                                        SearchSource.ONLINE -> R.string.search_library
                                    },
                                ),
                        )
                    }
                }
            }
        },
        modifier =
            modifier
                .focusRequester(focusRequester)
                .fillMaxWidth(),
        focusRequester = focusRequester,
    ) {
        Crossfade(
            targetState = searchSource,
            label = "search_content_transition",
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(bottom = if (!playerBottomSheetDismissed) MiniPlayerHeight else 0.dp)
                    .navigationBarsPadding(),
        ) { currentSearchSource ->
            when (currentSearchSource) {
                SearchSource.LOCAL ->
                    LocalSearchScreen(
                        query = query.text,
                        navController = navController,
                        onDismiss = { onActiveChange(false) },
                        pureBlack = pureBlack,
                    )
                SearchSource.ONLINE ->
                    OnlineSearchScreen(
                        query = query.text,
                        onQueryChange = onQueryChange,
                        navController = navController,
                        onSearch = { searchQuery ->
                            try {
                                val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
                                navController.navigate("search/$encodedQuery")
                            } catch (e: Exception) {
                                Log.e("SearchNavigation", "Error navigating to search: ${e.message}", e)
                            }
                        },
                        onDismiss = { onActiveChange(false) },
                        pureBlack = pureBlack,
                    )
            }
        }
    }
}
