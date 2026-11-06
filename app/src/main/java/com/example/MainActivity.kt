package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.AtefBottomNav
import com.example.ui.components.AtefHeader
import com.example.ui.components.AtefTvNavRail
import com.example.ui.components.DeviceModeDialog
import com.example.ui.navigation.AtefNavHost
import com.example.ui.navigation.Screen
import com.example.ui.theme.AtefIptvTheme
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.DeviceMode
import com.example.ui.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Keep screen awake while media player is active
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val isDarkTheme by appViewModel.isDarkTheme.collectAsState()
            val deviceMode by appViewModel.deviceMode.collectAsState()
            val showFirstRunDialog by appViewModel.showFirstRunDialog.collectAsState()
            val activeSubscription by appViewModel.activeSubscription.collectAsState()

            AtefIptvTheme(darkTheme = isDarkTheme) {
                // Mandatory Arabic RTL Layout Direction
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

                    // Welcome device selection modal on first launch
                    if (showFirstRunDialog) {
                        DeviceModeDialog(
                            initialMode = deviceMode,
                            onConfirm = { mode ->
                                appViewModel.completeFirstRun(mode)
                            }
                        )
                    }

                    val isPlayerScreen = currentRoute == Screen.Player.route
                    val isTvMode = deviceMode == DeviceMode.TV

                    if (isPlayerScreen) {
                        // 100% Immersive Fullscreen Video Player Mode
                        AtefNavHost(
                            navController = navController,
                            appViewModel = appViewModel,
                            playerViewModel = playerViewModel
                        )
                    } else if (isTvMode) {
                        // Android TV Leanback Layout with D-Pad Nav Rail and Safe Overscan Inset
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = 20.dp, vertical = 12.dp) // Prevents overscan clipping on TV screens
                        ) {
                            AtefTvNavRail(
                                currentRoute = currentRoute,
                                activeSubscription = activeSubscription,
                                onNavigate = { screen ->
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(Screen.Dashboard.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                onRefreshSubscription = {
                                    appViewModel.refreshActiveSubscription()
                                }
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Box(modifier = Modifier.fillMaxSize()) {
                                AtefNavHost(
                                    navController = navController,
                                    appViewModel = appViewModel,
                                    playerViewModel = playerViewModel
                                )
                            }
                        }
                    } else {
                        // Android Phone & Tablet Scaffold with safeDrawingPadding to prevent screen overflow
                        Scaffold(
                            modifier = Modifier
                                .fillMaxSize()
                                .safeDrawingPadding(),
                            containerColor = MaterialTheme.colorScheme.background,
                            topBar = {
                                AtefHeader(
                                    activeSubscription = activeSubscription,
                                    onNavigateToAddSubscription = {
                                        navController.navigate(Screen.AddSubscription.route) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onNavigateToSupport = {
                                        navController.navigate(Screen.Support.route) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onRefreshSubscription = {
                                        appViewModel.refreshActiveSubscription()
                                    }
                                )
                            },
                            bottomBar = {
                                AtefBottomNav(
                                    currentRoute = currentRoute,
                                    onNavigate = { screen ->
                                        if (currentRoute != screen.route) {
                                            navController.navigate(screen.route) {
                                                popUpTo(Screen.Dashboard.route) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                )
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                AtefNavHost(
                                    navController = navController,
                                    appViewModel = appViewModel,
                                    playerViewModel = playerViewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
