package com.edm.downloadmanager

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.edm.downloadmanager.ui.navigation.EDMDestination
import com.edm.downloadmanager.ui.screens.*
import com.edm.downloadmanager.ui.theme.EDMTheme
import com.edm.downloadmanager.ui.theme.EdmThemeMode
import com.edm.downloadmanager.viewmodel.DownloadViewModel

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        permissions += Manifest.permission.CAMERA
        permissionLauncher.launch(permissions.toTypedArray())

        // Handle a shared/opened direct link (ACTION_VIEW / ACTION_SEND) as an incoming download.
        val incomingUrl = extractIncomingUrl(intent)

        setContent {
            val settings = (application as EDMApplication).settingsStore
            val themeMode = when (settings.themeMode) {
                "LIGHT" -> EdmThemeMode.LIGHT
                "DARK" -> EdmThemeMode.DARK
                else -> EdmThemeMode.SYSTEM
            }
            EDMTheme(themeMode = themeMode) {
                EDMApp(incomingUrl = incomingUrl)
            }
        }
    }

    private fun extractIncomingUrl(intent: Intent?): String? {
        return when (intent?.action) {
            Intent.ACTION_VIEW -> intent.dataString
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.startsWith("http") }
            else -> null
        }
    }
}

@Composable
fun EDMApp(incomingUrl: String?) {
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
        return
    }

    val navController = rememberNavController()
    val downloadViewModel: DownloadViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    LaunchedEffect(incomingUrl) {
        incomingUrl?.let { downloadViewModel.addDownloadFromUrl(it) }
    }

    Scaffold(
        bottomBar = { EDMBottomBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = EDMDestination.Home.route,
            modifier = androidx.compose.ui.Modifier.padding(padding)
        ) {
            composable(EDMDestination.Home.route) {
                HomeScreen(
                    viewModel = downloadViewModel,
                    onOpenQrScanner = { navController.navigate("qr_scanner") },
                    onOpenDownloadDetail = { }
                )
            }
            composable(EDMDestination.Downloads.route) {
                DownloadsScreen(downloadViewModel) { }
            }
            composable(EDMDestination.Queue.route) {
                QueueScreen(downloadViewModel) { }
            }
            composable(EDMDestination.History.route) {
                HistoryScreen(downloadViewModel) { }
            }
            composable(EDMDestination.Favorites.route) {
                FavoritesScreen(downloadViewModel) { }
            }
            composable(EDMDestination.Statistics.route) {
                StatisticsScreen(downloadViewModel)
            }
            composable(EDMDestination.FileManager.route) {
                FileManagerScreen()
            }
            composable(EDMDestination.Settings.route) {
                SettingsScreen()
            }
            composable(EDMDestination.About.route) {
                AboutScreen()
            }
            composable("qr_scanner") {
                QrScannerScreen(onLinkFound = { url ->
                    downloadViewModel.addDownloadFromUrl(url)
                    navController.popBackStack()
                })
            }
        }
    }
}

@Composable
private fun EDMBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        val items = listOf(
            Triple(EDMDestination.Home, Icons.Filled.Home, "Home"),
            Triple(EDMDestination.Downloads, Icons.Filled.Download, "Downloads"),
            Triple(EDMDestination.Queue, Icons.Filled.List, "Queue"),
            Triple(EDMDestination.Statistics, Icons.Filled.BarChart, "Stats"),
            Triple(EDMDestination.Settings, Icons.Filled.Settings, "Settings")
        )
        items.forEach { (destination, icon, label) ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) }
            )
        }
    }
}
