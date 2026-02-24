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
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalContext
import android.os.Build
import com.example.newsarize.ui.screens.BriefingScreen
import com.example.newsarize.ui.screens.DownloadScreen
import com.example.newsarize.ui.screens.SettingsScreen
import com.example.newsarize.ui.screens.WebViewScreen
import com.example.newsarize.ui.viewmodel.NewsViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: NewsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NewsarizeTheme {
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
fun NewsarizeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> MaterialTheme.colorScheme // Fallback or defined scheme
        else -> MaterialTheme.colorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
fun NewsApp(viewModel: NewsViewModel) {
    val navController = rememberNavController()
    val isModelReady by viewModel.isModelReady.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (isModelReady) "briefing" else "download",
        modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars)
    ) {
        composable("briefing") {
            BriefingScreen(
                viewModel = viewModel,
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToBrowser = { url, title -> 
                    val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                    val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
                    navController.navigate("browser/$encodedUrl/$encodedTitle") 
                }
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
        composable("browser/{url}/{title}") { backStackEntry ->
            val url = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", "UTF-8")
            val title = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", "UTF-8")
            WebViewScreen(
                url = url,
                title = title,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
