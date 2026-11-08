package com.example.englishtutor

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
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
    var isLearned: Boolean = false,
    var isHard: Boolean = false,
    var streak: Int = 0,
    var timesSeen: Int = 0,
    var lastSeenTimestamp: Long = 0L
) {
    // נוסחת העדיפות של האלגוריתם החכם (ציון גבוה יותר = יוצג מוקדם יותר)
    fun calculatePriorityScore(now: Long): Double {
        var score = 100.0

        // מילים שנלמדו יורדות לעדיפות נמוכה מאוד (אלא אם עבר הרבה זמן)
        if (isLearned) score -= 85.0

        // מילים קשות מקבלות הקפצה משמעותית
        if (isHard) score += 65.0

        // מילים חדשות שעדיין לא נראו מקבלות תעדוף
        if (timesSeen == 0) score += 35.0

        // ככל שרצף ההצלחות (streak) גבוה יותר, המילה צריכה להופיע פחות
        score -= (streak * 12.0).coerceAtMost(60.0)

        // אפקט השכחה: ככל שעבר יותר זמן מאז הפעם האחרונה, הציון עולה
        if (lastSeenTimestamp > 0L) {
            val minutesPassed = ((now - lastSeenTimestamp) / 60000.0).coerceAtMost(180.0)
            score += minutesPassed * 0.4
        }

        // תוספת אקראיות קלה כדי שהסדר לא יהיה צפוי ומשעמם
        score += Random.nextDouble(-6.0, 6.0)
        return score
    }
}

class WordRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("smart_words_prefs", Context.MODE_PRIVATE)

    fun loadWords(): List<WordCard> {
        val wordList = mutableListOf<WordCard>()
        try {
            val jsonString = context.assets.open("words.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val english = item.getString("english")

                wordList.add(
                    WordCard(
                        english = english,
                        hebrew = item.getString("hebrew"),
                        isLearned = prefs.getBoolean("${english}_learned", false),
                        isHard = prefs.getBoolean("${english}_hard", false),
                        streak = prefs.getInt("${english}_streak", 0),
                        timesSeen = prefs.getInt("${english}_seen", 0),
                        lastSeenTimestamp = prefs.getLong("${english}_last", 0L)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            wordList.add(WordCard("Resilience", "חוסן נפשי / עמידות"))
        }
        return sortSmartQueue(wordList, StudyFilter.SMART)
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

    // בניית תור הלמידה לפי הסינון והאלגוריתם
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
        return filtered.sortedByDescending { it.calculatePriorityScore(now) }
    }

    // שליפת המילה הכי דחופה הבאה (שאינה המילה הנוכחית)
    fun pickNextSmartWord(
        allWords: List<WordCard>,
        filter: StudyFilter,
        currentEnglish: String?
    ): WordCard? {
        val queue = sortSmartQueue(allWords, filter)
        if (queue.isEmpty()) return null
        if (queue.size == 1) return queue.first()
        return queue.firstOrNull { it.english != currentEnglish } ?: queue.first()
    }
}
