package com.example.englishtutor

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        val repository = WordRepository(this)

        setContent {
            MaterialTheme(colorScheme = AppColorScheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = DarkBg) {
                    var wordList by remember { mutableStateOf(repository.loadWords()) }
                    var loadError by remember { mutableStateOf(repository.loadError) }

                    val currentError = loadError
                    if (currentError != null) {
                        ErrorScreen(
                            message = currentError,
                            onRetry = {
                                wordList = repository.loadWords()
                                loadError = repository.loadError
                            },
                        )
                    } else {
                        FlashcardScreen(words = wordList, repository = repository)
                    }
                }
            }
        }
    }
}

private val AppColorScheme = darkColorScheme(
    primary = TealNeon,
    onPrimary = DarkBg,
    secondary = PurpleNeon,
    onSecondary = DarkBg,
    tertiary = StarGold,
    onTertiary = DarkBg,
    background = DarkBg,
    onBackground = Color.White,
    surface = CardDark,
    onSurface = Color.White,
    surfaceVariant = CardDark,
    onSurfaceVariant = Color.LightGray,
)

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "⚠️", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            color = Color.White,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onRetry) {
            Text("נסה שוב")
        }
    }
}
