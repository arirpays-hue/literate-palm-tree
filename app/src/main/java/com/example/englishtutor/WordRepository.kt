package com.example.englishtutor

import android.content.Context
import org.json.JSONArray

data class WordCard(val english: String, val hebrew: String)

class WordRepository(private val context: Context) {
    fun loadWords(): List<WordCard> {
        val wordList = mutableListOf<WordCard>()
        try {
            val jsonString = context.assets.open("words.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                wordList.add(
                    WordCard(
                        english = item.getString("english"),
                        hebrew = item.getString("hebrew")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            wordList.add(WordCard("Error", "שגיאה בטעינת המילים"))
        }
        return wordList.shuffled()
    }
}
