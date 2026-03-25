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

    data object Library : Screens(
        titleId = R.string.filter_library,
        iconIdInactive = R.drawable.library_music_outlined,
        iconIdActive = R.drawable.library_music_filled,
        route = "library"
    )

    data object Premium : Screens(
        titleId = R.string.premium, // Need to add this string
        iconIdInactive = R.drawable.diamond_outlined, // Need to add this drawable
        iconIdActive = R.drawable.diamond_filled, // Need to add this drawable
        route = "premium"
    )

    companion object {
        val MainScreens = listOf(Home, Explore, Library, Premium)
    }
}

