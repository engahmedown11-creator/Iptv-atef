package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ui.screens.AddSubscriptionScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.LiveTvScreen
import com.example.ui.screens.MoviesScreen
import com.example.ui.screens.SeriesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StorageManagerScreen
import com.example.ui.screens.SupportScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.PlayerViewModel

@Composable
fun AtefNavHost(
    navController: NavHostController,
    appViewModel: AppViewModel,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                appViewModel = appViewModel,
                playerViewModel = playerViewModel,
                onNavigate = { screen ->
                    navController.navigate(screen.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.LiveTv.route) {
            LiveTvScreen(
                appViewModel = appViewModel,
                playerViewModel = playerViewModel,
                onNavigate = { screen ->
                    navController.navigate(screen.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Movies.route) {
            MoviesScreen(
                appViewModel = appViewModel,
                playerViewModel = playerViewModel,
                onNavigate = { screen ->
                    navController.navigate(screen.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Series.route) {
            SeriesScreen(
                appViewModel = appViewModel,
                playerViewModel = playerViewModel,
                onNavigate = { screen ->
                    navController.navigate(screen.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Player.route) {
            VideoPlayerScreen(
                playerViewModel = playerViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Downloads.route) {
            DownloadsScreen(
                playerViewModel = playerViewModel,
                onNavigate = { screen ->
                    navController.navigate(screen.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Storage.route) {
            StorageManagerScreen(
                appViewModel = appViewModel
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                appViewModel = appViewModel,
                onNavigate = { screen ->
                    navController.navigate(screen.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.AddSubscription.route) {
            AddSubscriptionScreen(
                appViewModel = appViewModel,
                onSubscriptionAdded = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Support.route) {
            SupportScreen()
        }
    }
}
