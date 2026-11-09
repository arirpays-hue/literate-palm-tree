package com.example.englishtutor

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException
import kotlin.random.Random

enum class StudyFilter(val label: String) {
    SMART("תור חכם"),
    UNLEARNED("לא נלמדו"),
    HARD("קשות בלבד"),
    ALL("הכל")
}

data class WordCard(
    val english: String,
    val hebrew: String,
    val isLearned: Boolean = false,
    val isHard: Boolean = false,
    val streak: Int = 0,
    val timesSeen: Int = 0,
    val lastSeenTimestamp: Long = 0L,
) {
    fun calculatePriorityScore(now: Long, random: Random = Random.Default): Double {
        var score = 100.0
        if (isLearned) score -= 85.0
        if (isHard) score += 65.0
        if (timesSeen == 0) score += 35.0
        score -= (streak * 12.0).coerceAtMost(60.0)

        if (lastSeenTimestamp > 0L) {
            val minutesPassed = ((now - lastSeenTimestamp) / 60000.0).coerceAtMost(180.0)
            score += minutesPassed * 0.4
        }

        score += random.nextDouble(-6.0, 6.0)
        return score
    }
}

class WordRepository(
    private val context: Context,
    private val random: Random = Random.Default,
) {
    companion object {
        private const val TAG = "WordRepository"
        private const val PREFS_NAME = "smart_words_prefs"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var loadError: String? = null
        private set

    fun loadWords(): List<WordCard> {
        loadError = null
        return try {
            val jsonString = context.assets.open("words.json")
                .bufferedReader()
                .use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            buildList {
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val english = item.getString("english")
                    add(
                        WordCard(
                            english = english,
                            hebrew = item.getString("hebrew"),
                            isLearned = prefs.getBoolean("${english}_learned", false),
                            isHard = prefs.getBoolean("${english}_hard", false),
                            streak = prefs.getInt("${english}_streak", 0),
                            timesSeen = prefs.getInt("${english}_seen", 0),
                            lastSeenTimestamp = prefs.getLong("${english}_last", 0L),
                        )
                    )
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read assets/words.json", e)
            loadError = "לא ניתן לקרוא את קובץ המילים. ודא שהקובץ קיים ב־assets."
            emptyList()
        } catch (e: JSONException) {
            Log.e(TAG, "Malformed words.json", e)
            loadError = "קובץ המילים פגום ולא ניתן לקרוא אותו."
            emptyList()
        }
    }

    fun saveWordState(word: WordCard) {
        prefs.edit().apply {
            putBoolean("${word.english}_learned", word.isLearned)
            putBoolean("${word.english}_hard", word.isHard)
            putInt("${word.english}_streak", word.streak)
            putInt("${word.english}_seen", word.timesSeen)
            putLong("${word.english}_last", word.lastSeenTimestamp)
            apply()
        }
    }

    fun sortSmartQueue(allWords: List<WordCard>, filter: StudyFilter): List<WordCard> {
        val now = System.currentTimeMillis()
        val filtered = when (filter) {
            StudyFilter.UNLEARNED -> allWords.filter { !it.isLearned }
            StudyFilter.HARD -> allWords.filter { it.isHard }
            StudyFilter.ALL -> allWords
            StudyFilter.SMART -> {
                val active = allWords.filter { !it.isLearned || it.isHard }
                active.ifEmpty { allWords }
            }
        }
        return filtered.sortedByDescending { it.calculatePriorityScore(now, random) }
    }

    fun pickNextSmartWord(
        allWords: List<WordCard>,
        filter: StudyFilter,
        currentEnglish: String?,
    ): WordCard? {
        val queue = sortSmartQueue(allWords, filter)
        if (queue.isEmpty()) return null
        if (queue.size == 1) return queue.first()
        return queue.firstOrNull { it.english != currentEnglish } ?: queue.first()
    }
}
