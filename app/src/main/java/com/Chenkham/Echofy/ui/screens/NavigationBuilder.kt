package com.Chenkham.Echofy.ui.screens

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.Chenkham.Echofy.BuildConfig
import com.Chenkham.Echofy.ads.AdManager
import com.Chenkham.Echofy.ui.screens.artist.ArtistItemsScreen
import com.Chenkham.Echofy.ui.screens.artist.ArtistScreen
import com.Chenkham.Echofy.ui.screens.artist.ArtistSongsScreen
import com.Chenkham.Echofy.ui.screens.library.CachePlaylistScreen
import com.Chenkham.Echofy.ui.screens.library.LibraryScreen
import com.Chenkham.Echofy.ui.screens.playlist.AutoPlaylistScreen
import com.Chenkham.Echofy.ui.screens.playlist.LocalPlaylistScreen
import com.Chenkham.Echofy.ui.screens.playlist.OnlinePlaylistScreen
import com.Chenkham.Echofy.ui.screens.playlist.TopPlaylistScreen
import com.Chenkham.Echofy.ui.screens.search.OnlineSearchResult
import com.Chenkham.Echofy.ui.screens.settings.AboutScreen
import com.Chenkham.Echofy.ui.screens.settings.AccountSettings
import com.Chenkham.Echofy.ui.screens.settings.AppearanceSettings
import com.Chenkham.Echofy.ui.screens.settings.BackupAndRestore
import com.Chenkham.Echofy.ui.screens.settings.ContentSettings
import com.Chenkham.Echofy.ui.screens.settings.DiscordLoginScreen
import com.Chenkham.Echofy.ui.screens.settings.DiscordSettings
import com.Chenkham.Echofy.ui.screens.settings.PlayerSettings
import com.Chenkham.Echofy.ui.screens.settings.PrivacySettings
import com.Chenkham.Echofy.ui.screens.settings.SettingsScreen
import com.Chenkham.Echofy.ui.screens.settings.StorageSettings
import com.Chenkham.Echofy.ui.screens.settings.EqualizerScreen
import com.Chenkham.Echofy.ui.screens.settings.BackpaperSettings
import com.Chenkham.Echofy.ui.screens.settings.SaverSettings
import com.Chenkham.Echofy.ui.utils.TouchBlockingWrapper
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
    adManager: AdManager? = null,
    isOnboardingCompleted: Boolean = true,
    onOnboardingComplete: () -> Unit = {},
) {
    composable("splash") {
        TouchBlockingWrapper {
            SplashScreen(
                onNavigateToOnboarding = {
                    navController.navigate("onboarding") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screens.Home.route) {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                isOnboardingCompleted = isOnboardingCompleted
            )
        }
    }
    composable("onboarding") {
        TouchBlockingWrapper {
            OnboardingScreen(
                onLoginClick = {
                    navController.navigate("sign_in?chained=true")
                },
                onSkipClick = {
                    onOnboardingComplete()
                    navController.navigate(Screens.Home.route) {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
    }
    composable(Screens.Home.route) {
        TouchBlockingWrapper {
            HomeScreen(navController, adManager = adManager)
        }
    }
    composable(
        route = "sign_in?chained={chained}",
        arguments = listOf(
            navArgument("chained") {
                type = NavType.BoolType
                defaultValue = false
            }
        )
    ) { backStackEntry ->
        TouchBlockingWrapper {
            val chained = backStackEntry.arguments?.getBoolean("chained") ?: false
            val mainViewModel: com.Chenkham.Echofy.MainViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
            com.Chenkham.Echofy.ui.screens.auth.SignInScreen(
                chained = chained,
                onSignInSuccess = {
                    mainViewModel.disableGuestMode()
                    navController.navigate("login?chained=true") {
                        popUpTo("sign_in?chained={chained}") { inclusive = true }
                    }
                },
                onContinueAsGuest = {
                    coroutineScope.launch {
                        mainViewModel.enableGuestMode()
                        navController.navigate(Screens.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
    }

    composable(route = "account_settings") {
        TouchBlockingWrapper {
            com.Chenkham.Echofy.ui.screens.settings.AccountSettings(
                navController = navController,
                scrollBehavior = scrollBehavior
            )
        }
    }

    composable(
        Screens.Library.route,
    ) {
        TouchBlockingWrapper {
            LibraryScreen(navController, adManager = adManager)
        }
    }

    composable(
        Screens.Premium.route
    ) {
        TouchBlockingWrapper {
            com.Chenkham.Echofy.ui.screens.premium.PremiumScreen()
        }
    }
    composable(Screens.Explore.route) {
        TouchBlockingWrapper {
            ExploreScreen(navController, scrollBehavior, adManager = adManager)
        }
    }

    composable("history") {
        TouchBlockingWrapper {
            HistoryScreen(navController)
        }
    }
    composable("stats") {
        TouchBlockingWrapper {
            StatsScreen(navController)
        }
    }
    composable("account") {
        TouchBlockingWrapper {
            AccountScreen(navController, scrollBehavior)
        }
    }
    composable("new_release") {
        TouchBlockingWrapper {
            NewReleaseScreen(navController, scrollBehavior)
        }
    }
    composable("notifications") {
        TouchBlockingWrapper {
            NotificationsScreen(navController)
        }
    }




    composable(
        route = "search/{query}",
        arguments =
            listOf(
                navArgument("query") {
                    type = NavType.StringType
                },
            ),
        enterTransition = {
            fadeIn(tween(250))
        },
        exitTransition = {
            if (targetState.destination.route?.startsWith("search/") == true) {
                fadeOut(tween(200))
            } else {
                fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
            }
        },
        popEnterTransition = {
            if (initialState.destination.route?.startsWith("search/") == true) {
                fadeIn(tween(250))
            } else {
                fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
            }
        },
        popExitTransition = {
            fadeOut(tween(200))
        },
    ) {
        TouchBlockingWrapper {
            OnlineSearchResult(navController, adManager = adManager)
        }
    }
    composable(
        route = "album/{albumId}",
        arguments =
            listOf(
                navArgument("albumId") {
                    type = NavType.StringType
                },
            ),
    ) {
        TouchBlockingWrapper {
            AlbumScreen(navController, scrollBehavior)
        }
    }
    composable(
        route = "artist/{artistId}",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
            ),
    ) { backStackEntry ->
        TouchBlockingWrapper {
            val artistId = backStackEntry.arguments?.getString("artistId")!!
            if (artistId.startsWith("LA")) {
                ArtistSongsScreen(navController, scrollBehavior)
            } else {
                ArtistScreen(navController, scrollBehavior)
            }
        }
    }
    composable(
        route = "artist/{artistId}/songs",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        TouchBlockingWrapper {
            ArtistSongsScreen(navController, scrollBehavior)
        }
    }
    composable(
        route = "artist/{artistId}/items?browseId={browseId}?params={params}",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
                navArgument("browseId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("params") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) {
        TouchBlockingWrapper {
            ArtistItemsScreen(navController, scrollBehavior)
        }
    }
    composable(
        route = "online_playlist/{playlistId}",
        arguments =
            listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        TouchBlockingWrapper {
            OnlinePlaylistScreen(navController, scrollBehavior)
        }
    }
    composable(
        route = "local_playlist/{playlistId}",
        arguments =
            listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        TouchBlockingWrapper {
            LocalPlaylistScreen(navController, scrollBehavior)
        }
    }
    composable(
        route = "auto_playlist/{playlist}",
        arguments =
            listOf(
                navArgument("playlist") {
                    type = NavType.StringType
                },
            ),
    ) {
        TouchBlockingWrapper {
            AutoPlaylistScreen(navController, scrollBehavior)
        }
    }
    composable(
        route = "cache_playlist/{playlist}",
        arguments =
            listOf(
                navArgument("playlist") {
                    type = NavType.StringType
                },
            ),
    ) {
        TouchBlockingWrapper {
            CachePlaylistScreen(navController, scrollBehavior)
        }
    }



    composable(
        route = "top_playlist/{top}",
        arguments =
            listOf(
                navArgument("top") {
                    type = NavType.StringType
                },
            ),
    ) {
        TouchBlockingWrapper {
            TopPlaylistScreen(navController, scrollBehavior)
        }
    }
    composable(
        route = "youtube_browse/{browseId}?params={params}",
        arguments =
            listOf(
                navArgument("browseId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("params") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) {
        TouchBlockingWrapper {
            YouTubeBrowseScreen(navController)
        }
    }
    composable(
        route = "podcast/{podcastId}",
        arguments = listOf(
            navArgument("podcastId") {
                type = NavType.StringType
            }
        )
    ) {
        TouchBlockingWrapper {
            PodcastScreen(navController, scrollBehavior)
        }
    }


    composable("settings") {
        TouchBlockingWrapper {
            val latestVersion by mutableLongStateOf(BuildConfig.VERSION_CODE.toLong())
            SettingsScreen(latestVersion, navController, scrollBehavior)
        }
    }
    composable("settings/appearance") {
        TouchBlockingWrapper {
            AppearanceSettings(navController, scrollBehavior)
        }
    }
    composable("settings/backpaper") {
        TouchBlockingWrapper {
            BackpaperSettings(navController, scrollBehavior)
        }
    }
    composable("settings/account") {
        TouchBlockingWrapper {
            AccountSettings(navController, scrollBehavior)
        }
    }
    composable("settings/content") {
        TouchBlockingWrapper {
            ContentSettings(navController, scrollBehavior)
        }
    }
    composable("settings/player") {
        TouchBlockingWrapper {
            PlayerSettings(navController, scrollBehavior)
        }
    }
    composable("settings/storage") {
        TouchBlockingWrapper {
            StorageSettings(navController, scrollBehavior)
        }
    }
    composable("settings/privacy") {
        TouchBlockingWrapper {
            PrivacySettings(navController, scrollBehavior)
        }
    }
    composable("settings/backup_restore") {
        TouchBlockingWrapper {
            BackupAndRestore(navController, scrollBehavior)
        }
    }
    composable("settings/discord") {
        TouchBlockingWrapper {
            DiscordSettings(navController, scrollBehavior)
        }
    }
    composable("settings/discord/login") {
        TouchBlockingWrapper {
            DiscordLoginScreen(navController)
        }
    }
    composable("settings/about") {
        TouchBlockingWrapper {
            AboutScreen(navController, scrollBehavior)
        }
    }
    composable("settings/equalizer") {
        TouchBlockingWrapper {
            EqualizerScreen(navController)
        }
    }
    composable("settings/saver") {
        TouchBlockingWrapper {
            SaverSettings(navController, scrollBehavior)
        }
    }
    composable(
        route = "login?chained={chained}",
        arguments = listOf(
            navArgument("chained") {
                type = NavType.BoolType
                defaultValue = false
            }
        )
    ) { backStackEntry ->
        TouchBlockingWrapper {
            val chained = backStackEntry.arguments?.getBoolean("chained") ?: false
            LoginScreen(navController, chained)
        }
    }
}
