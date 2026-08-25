package com.Chenkham.Echofy.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.Chenkham.Echofy.R

@Immutable
sealed class Screens(
    @StringRes val titleId: Int,
    @DrawableRes val iconIdInactive: Int,
    @DrawableRes val iconIdActive: Int,
    val route: String,
) {
    data object Home : Screens(
        titleId = R.string.home,
        iconIdInactive = R.drawable.home_outlined,
        iconIdActive = R.drawable.home_filled,
        route = "home"
    )

    data object Explore : Screens(
        titleId = R.string.explore,
        iconIdInactive = R.drawable.explore_outlined,
        iconIdActive = R.drawable.explore_filled,
        route = "explore"
    )

    data object AiAssistant : Screens(
        titleId = R.string.ai_assistant,
        iconIdInactive = R.drawable.auto_awesome,
        iconIdActive = R.drawable.auto_awesome_filled,
        route = "ai_assistant"
    )

    data object Library : Screens(
        titleId = R.string.filter_library,
        iconIdInactive = R.drawable.library_music_outlined,
        iconIdActive = R.drawable.library_music_filled,
        route = "library"
    )

    data object Premium : Screens(
        titleId = R.string.premium, // Need to add this string
        iconIdInactive = R.drawable.donate, // Need to add this drawable
        iconIdActive = R.drawable.donate, // Need to add this drawable
        route = "premium"
    )

    data object DownloadQueue : Screens(
        titleId = R.string.download_queue,
        iconIdInactive = R.drawable.downloading,
        iconIdActive = R.drawable.downloading,
        route = "download_queue"
    )

    data object MoodAndGenres : Screens(
        titleId = R.string.mood_and_genres,
        iconIdInactive = R.drawable.style,
        iconIdActive = R.drawable.style,
        route = "mood_and_genres"
    )

    data object Search : Screens(
        titleId = R.string.search,
        iconIdInactive = R.drawable.search,
        iconIdActive = R.drawable.search,
        route = "search"
    )

    companion object {
        val MainScreens = listOf(Home, Explore, AiAssistant, Library, Premium)
    }
}

