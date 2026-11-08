package com.example.englishtutor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val repository = WordRepository(this)
        val wordList = repository.loadWords()

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = DarkBg // שימוש ברקע הכהה שהגדרנו
            ) {
                FlashcardScreen(words = wordList, repository = repository)
            }
        }
    }
}
