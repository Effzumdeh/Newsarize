package com.example.newsarize

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.newsarize.ui.screens.BriefingScreen
import com.example.newsarize.ui.screens.DownloadScreen
import com.example.newsarize.ui.screens.SettingsScreen
import com.example.newsarize.ui.viewmodel.NewsViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: NewsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NewsApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun NewsApp(viewModel: NewsViewModel) {
    val navController = rememberNavController()
    val isModelReady by viewModel.isModelReady.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (isModelReady) "briefing" else "download"
    ) {
        composable("briefing") {
            BriefingScreen(
                viewModel = viewModel,
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("download") {
            // Once ready, we should navigate to briefing
            if (isModelReady) {
                // Navigate immediately but avoid doing it during composition if possible,
                // simplified for this example:
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    navController.navigate("briefing") {
                        popUpTo("download") { inclusive = true }
                    }
                }
            }
            DownloadScreen(viewModel = viewModel)
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
