package com.example.englishtutor

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

data class WordCard(
    val english: String, 
    val hebrew: String,
    var isLearned: Boolean = false,
    var isHard: Boolean = false
)

class WordRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("words_prefs", Context.MODE_PRIVATE)

    fun loadWords(): List<WordCard> {
        val wordList = mutableListOf<WordCard>()
        try {
            val jsonString = context.assets.open("words.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val english = item.getString("english")
                
                // קריאת המצב השמור מהזיכרון
                val isLearned = prefs.getBoolean("${english}_learned", false)
                val isHard = prefs.getBoolean("${english}_hard", false)

                wordList.add(
                    WordCard(
                        english = english,
                        hebrew = item.getString("hebrew"),
                        isLearned = isLearned,
                        isHard = isHard
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            wordList.add(WordCard("Error", "שגיאה בטעינת המילים"))
        }
        return wordList.shuffled() // מערבב את המילים
    }

    fun saveWordState(word: WordCard) {
        prefs.edit().apply {
            putBoolean("${word.english}_learned", word.isLearned)
            putBoolean("${word.english}_hard", word.isHard)
            apply()
        }
    }
}
