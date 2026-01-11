package com.chamundi.templete

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chamundi.templete.ui.screens.BreakingNewsScreen
import com.chamundi.templete.ui.screens.HomeScreen
import com.chamundi.templete.ui.screens.YouTubeLinkScreen
import com.chamundi.templete.ui.theme.TempleteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Handle incoming share intent
        val sharedLink = handleIncomingShare()
        
        setContent {
            TempleteTheme {
                AppNavigation(initialSharedLink = sharedLink)
            }
        }
    }
    
    private fun handleIncomingShare(): String? {
        return if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (sharedText != null) {
                Toast.makeText(this, "Link received!", Toast.LENGTH_SHORT).show()
                sharedText
            } else {
                null
            }
        } else if (intent?.action == Intent.ACTION_SEND_MULTIPLE && intent.type == "text/plain") {
            val sharedTexts = intent.getStringArrayListExtra(Intent.EXTRA_TEXT)
            if (sharedTexts != null && sharedTexts.isNotEmpty()) {
                val firstText = sharedTexts[0]
                Toast.makeText(this, "Link received!", Toast.LENGTH_SHORT).show()
                firstText
            } else {
                null
            }
        } else {
            null
        }
    }
}

@Composable
fun AppNavigation(initialSharedLink: String? = null) {
    val navController = rememberNavController()
    // Determine start destination
    val startDestination = if (initialSharedLink != null) "link_share?link=$initialSharedLink" else "home"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("home") {
            HomeScreen(
                onNavigateToBreakingNews = { navController.navigate("breaking_news") }
            )
        }
        
        composable("breaking_news") {
            BreakingNewsScreen(
                onNavigateToLinkShare = { navController.navigate("link_share") }
            )
        }
        
        composable("link_share") {
            YouTubeLinkScreen(
                onNavigateBack = { navController.popBackStack() },
                initialLink = initialSharedLink
            )
        }
        
        composable(
            route = "link_share?link={link}"
        ) { backStackEntry ->
            val link = backStackEntry.arguments?.getString("link")
            YouTubeLinkScreen(
                onNavigateBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                },
                initialLink = link
            )
        }
    }
}
