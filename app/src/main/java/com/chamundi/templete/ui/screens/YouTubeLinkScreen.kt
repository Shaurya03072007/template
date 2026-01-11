package com.chamundi.templete.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chamundi.templete.utils.ImageGenerator.shareToSocialMedia

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeLinkScreen(
    onNavigateBack: () -> Unit,
    initialLink: String? = null
) {
    var youtubeLink by remember(initialLink) {
        mutableStateOf(initialLink ?: "https://www.youtube.com/watch?v=B5MijkEMJfg")
    }
    var newsDescription by remember { mutableStateOf("   నుండి ప్రత్యక్ష ప్రసారం...") }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share YouTube Link") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = youtubeLink,
                onValueChange = { youtubeLink = it },
                label = { Text("YouTube Link") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )

            OutlinedTextField(
                value = newsDescription,
                onValueChange = { newsDescription = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 5,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            if (youtubeLink.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                         Text("Preview:", style = MaterialTheme.typography.labelLarge)
                         Text(
                             text = formatShareText(youtubeLink, newsDescription),
                             style = MaterialTheme.typography.bodySmall
                         )
                    }
                }
            }

            Button(
                onClick = {
                    shareYouTubeLink(context, youtubeLink, newsDescription)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = youtubeLink.isNotEmpty()
            ) {
                Text("Share Link")
            }
        }
    }
}

fun formatShareText(youtubeLink: String, newsDescription: String): String {
    return buildString {
        append(newsDescription)
        append("\n")
        append("బ్లూలింకు కొట్టి వీడియో చూడగలరు\n")
        append(youtubeLink)
        append("\n")
        append("క్రింది బ్లూలింకును నొక్కి LIKE/FOLLOW/SUBSCRIBE బటన్ నొక్కండి\n")
        append("Facebook:- ")
        append("https://www.facebook.com/CHAMUNDINEWS24X7\n")
        append("Youtube:- ")
        append("https://www.youtube.com/@CHAMUNDITV?sub_confirmation=1\n")
        append("Instagram:- ")
        append("https://www.instagram.com/chamunditvtelugu\n")
        append("VIEW CHANNEL నొక్కి,FOLLOW బటన్ నొక్కండి")
    }
}

fun shareYouTubeLink(context: android.content.Context, link: String, newsDescription: String = "") {
    try {
        val shareText = formatShareText(link, newsDescription)

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "YouTube Link")
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Share YouTube Link")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
