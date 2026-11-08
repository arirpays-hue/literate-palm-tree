package com.example.englishtutor

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// פלטת צבעים כהה וזוהרת
val DarkBg = Color(0xFF0B0E17)
val CardDark = Color(0xFF161B2E)
val TealNeon = Color(0xFF00F0FF)
val TealDark = Color(0xFF004D5A)
val StarGold = Color(0xFFFFC400)
val CheckNeon = Color(0xFF00E676)
val HardCrimson = Color(0xFFFF5252)

@Composable
fun FlashcardScreen(initialWords: List<WordCard>, repository: WordRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var allWords by remember { mutableStateOf(initialWords) }
    var selectedFilter by remember { mutableStateOf(StudyFilter.SMART) }
    var currentWord by remember {
        mutableStateOf(repository.pickNextSmartWord(allWords, selectedFilter, null))
    }

    var isFlipped by remember { mutableStateOf(false) }
    var autoSpeak by remember { mutableStateOf(true) }
    var isSpeaking by remember { mutableStateOf(false) }
    var statsRefreshTrigger by remember { mutableIntStateOf(0) }

    // --- מנוע הקראה קולית (TextToSpeech) ---
    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                instance?.language = Locale.US
                instance?.setSpeechRate(0.92f) // קצב דיבור ברור ונוח ללמידה
                instance?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { isSpeaking = true }
                    override fun onDone(utteranceId: String?) { isSpeaking = false }
                    override fun onError(utteranceId: String?) { isSpeaking = false }
                })
                ttsReady = true
            }
        }
        instance
    }

    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    fun speakWord(wordText: String) {
        if (ttsReady) {
            isSpeaking = true
            tts.speak(wordText, TextToSpeech.QUEUE_FLUSH, null, "WORD_TTS_ID")
        }
    }

    // הקראה אוטומטית בכל פעם שעוברים למילה חדשה
    LaunchedEffect(currentWord?.english, ttsReady) {
        if (ttsReady && autoSpeak && currentWord != null) {
            delay(220)
            speakWord(currentWord!!.english)
        }
    }

    // --- מערך האנימציות של הכרטיס ---
    val dragOffsetX = remember { Animatable(0f) }
    val dragOffsetY = remember { Animatable(0f) }
    val cardEnterScale = remember { Animatable(1f) }

    // 1. אנימציית היפוך תלת-ממדית עם פיזיקת קפיץ
    val flipRotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "flipRotation"
    )

    // 2. אנימציית נשימה וזוהר מתמשכת (Infinite Pulse)
    val infiniteTransition = rememberInfiniteTransition(label = "ambientAnimations")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.99f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )
    val floatingOffsetY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatingY"
    )
    val speakerWaveScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSpeaking) 1.38f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speakerWave"
    )

    // 3. צבע מסגרת דינמי שמשתנה לפי גרירה או מצב המילה
    val dynamicBorderColor by animateColorAsState(
        targetValue = when {
            dragOffsetX.value > 110f -> CheckNeon
            dragOffsetX.value < -110f -> StarGold
            currentWord?.isHard == true -> StarGold
            currentWord?.isLearned == true -> CheckNeon
            else -> TealNeon
        },
        animationSpec = tween(250),
        label = "borderColor"
    )

    // פונקציית מעבר חכם למילה הבאה (כולל איפוס מיידי של ההיפוך)
    fun advanceToNextWord(exitDirection: Float = 0f) {
        scope.launch {
            isFlipped = false // מונע חשיפה של תרגום המילה הבאה
            if (exitDirection != 0f) {
                dragOffsetX.animateTo(exitDirection * 950f, tween(220))
            }
            cardEnterScale.snapTo(0.82f)
            dragOffsetX.snapTo(0f)
            dragOffsetY.snapTo(0f)

            currentWord = repository.pickNextSmartWord(
                allWords = allWords,
                filter = selectedFilter,
                currentEnglish = currentWord?.english
            )
            statsRefreshTrigger++

            cardEnterScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
        }
    }

    // עדכון ציון המילה באלגוריתם החכם
    fun markCurrentWord(learned: Boolean? = null, hard: Boolean? = null) {
        val word = currentWord ?: return
        word.timesSeen++
        word.lastSeenTimestamp = System.currentTimeMillis()

        if (learned != null) {
            word.isLearned = learned
            if (learned) {
                word.streak++
                word.isHard = false
            } else {
                word.streak = 0
            }
        }
        if (hard != null) {
            word.isHard = hard
            if (hard) {
                word.streak = 0
                word.isLearned = false
            }
        }
        repository.saveWordState(word)
        statsRefreshTrigger++
    }

    // חישוב סטטיסטיקות חיות
    val learnedCount = remember(statsRefreshTrigger, allWords) { allWords.count { it.isLearned } }
    val hardCount = remember(statsRefreshTrigger, allWords) { allWords.count { it.isHard } }
    val unlearnedCount = allWords.size - learnedCount
    val progressFraction by animateFloatAsState(
        targetValue = if (allWords.isNotEmpty()) learnedCount.toFloat() / allWords.size else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "progressAnim"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBg, Color(0xFF111827), DarkBg)
                )
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- חלק עליון: כותרת, מתג הקראה אוטומטית ומד התקדמות מונפש ---
        Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // מתג הקראה אוטומטית
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardDark)
                        .border(1.dp, if (autoSpeak) TealNeon else Color.Gray.copy(0.3f), RoundedCornerShape(20.dp))
                        .clickable { autoSpeak = !autoSpeak }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (autoSpeak) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "הקראה אוטומטית",
                        tint = if (autoSpeak) TealNeon else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (autoSpeak) "הקראה אוטומטית: פעיל" else "הקראה: ידנית",
                        fontSize = 12.sp,
                        color = if (autoSpeak) TealNeon else Color.Gray
                    )
                }

                Text(
                    text = "לימוד אנגלית חכם",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // שורת סטטיסטיקות מונפשת
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AnimatedCounterBadge("נלמדו", learnedCount, CheckNeon)
                AnimatedCounterBadge("לא נלמדו", unlearnedCount, TealNeon)
                AnimatedCounterBadge("קשות", hardCount, StarGold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // פס התקדמות מונפש
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = CheckNeon,
                trackColor = CardDark
            )

            Spacer(modifier = Modifier.height(12.dp))

            // סרגל סינון חכם (תור חכם / לא נלמדו / קשות / הכל)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StudyFilter.entries.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    val chipScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 0.96f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "chipScale"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .scale(chipScale)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) TealDark else CardDark)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) TealNeon else Color.Gray.copy(0.25f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                selectedFilter = filter
                                isFlipped = false
                                currentWord = repository.pickNextSmartWord(allWords, filter, null)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) TealNeon else Color.LightGray
                        )
                    }
                }
            }
        }

        // --- חלק מרכזי: הכרטיסייה האינטראקטיבית (תומכת בהיפוך + החלקה לימין/שמאל) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (currentWord == null) {
                // מסך סיום מונפש אם כל המילים בקטגוריה נלמדו
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = StarGold,
                        modifier = Modifier.size(72.dp).scale(breathingScale)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "סיימת את כל המילים בקטגוריה זו!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            selectedFilter = StudyFilter.ALL
                            currentWord = repository.pickNextSmartWord(allWords, StudyFilter.ALL, null)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealDark)
                    ) {
                        Text("חזור לכל המילים", color = TealNeon)
                    }
                }
            } else {
                val word = currentWord!!

                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(340.dp)
                        .offset {
                            IntOffset(
                                x = dragOffsetX.value.roundToInt(),
                                y = (dragOffsetY.value + floatingOffsetY).roundToInt()
                            )
                        }
                        .scale(cardEnterScale.value * breathingScale)
                        .graphicsLayer {
                            rotationY = flipRotation
                            rotationZ = (dragOffsetX.value / 28f).coerceIn(-18f, 18f)
                            cameraDistance = 15f * density
                        }
                        .pointerInput(word.english) {
                            detectDragGestures(
                                onDragEnd = {
                                    when {
                                        // החלקה ימינה -> סמן כ"נלמד" ועבור הלאה
                                        dragOffsetX.value > 220f -> {
                                            markCurrentWord(learned = true)
                                            advanceToNextWord(exitDirection = 1f)
                                        }
                                        // החלקה שמאלה -> סמן כ"קשה" ועבור הלאה
                                        dragOffsetX.value < -220f -> {
                                            markCurrentWord(hard = true)
                                            advanceToNextWord(exitDirection = -1f)
                                        }
                                        else -> {
                                            scope.launch {
                                                dragOffsetX.animateTo(0f, spring())
                                                dragOffsetY.animateTo(0f, spring())
                                            }
                                        }
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    scope.launch {
                                        dragOffsetX.snapTo(dragOffsetX.value + dragAmount.x)
                                        dragOffsetY.snapTo(dragOffsetY.value + dragAmount.y * 0.3f)
                                    }
                                }
                            )
                        }
                        .clickable { isFlipped = !isFlipped }
                        .border(2.5.dp, dynamicBorderColor, RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (flipRotation <= 90f) {
                            // --- צד קדמי: המילה באנגלית + כפתור הקראה קולית ---
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // תגית מצב חכמה בראש הכרטיס
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when {
                                            word.isHard -> "⚡ מילה מאתגרת"
                                            word.isLearned -> "✓ נלמדה (רצף ${word.streak})"
                                            word.timesSeen == 0 -> "✨ מילה חדשה"
                                            else -> "בתהליך למידה"
                                        },
                                        fontSize = 13.sp,
                                        color = dynamicBorderColor,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    // כפתור ההקראה הקולית באנגלית עם גלי קול מונפשים
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isSpeaking) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .scale(speakerWaveScale)
                                                    .clip(CircleShape)
                                                    .background(TealNeon.copy(alpha = 0.22f))
                                            )
                                        }
                                        IconButton(
                                            onClick = { speakWord(word.english) },
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(TealDark)
                                                .border(1.5.dp, TealNeon, CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = "הקרא מילה באנגלית",
                                                tint = TealNeon
                                            )
                                        }
                                    }
                                }

                                // טקסט המילה באנגלית עם מעבר חלק
                                AnimatedContent(
                                    targetState = word.english,
                                    transitionSpec = {
                                        (fadeIn(tween(260)) + scaleIn(initialScale = 0.85f))
                                            .togetherWith(fadeOut(tween(180)))
                                    },
                                    label = "englishWordTransition"
                                ) { targetEnglish ->
                                    Text(
                                        text = targetEnglish,
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TealNeon,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                // הוראות שימוש בתחתית הכרטיס
                                Text(
                                    text = "לחץ להיפוך • החלק ימינה (נלמד) או שמאלה (קשה)",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            // --- צד אחורי: הפירוש בעברית ---
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { rotationY = 180f },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = word.hebrew,
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = word.english,
                                    fontSize = 18.sp,
                                    color = TealNeon.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- חלק תחתון: כפתורי שליטה פונקציונליים עם אנימציית קפיץ ---
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BouncyActionIcon(
                    label = "נלמד",
                    icon = Icons.Default.Check,
                    activeColor = CheckNeon,
                    isActive = currentWord?.isLearned == true,
                    onClick = {
                        val newState = !(currentWord?.isLearned ?: false)
                        markCurrentWord(learned = newState)
                        if (newState) advanceToNextWord(exitDirection = 1f)
                    }
                )

                BouncyActionIcon(
                    label = "הקרא שוב",
                    icon = Icons.Default.RecordVoiceOver,
                    activeColor = TealNeon,
                    isActive = isSpeaking,
                    onClick = { currentWord?.let { speakWord(it.english) } }
                )

                BouncyActionIcon(
                    label = "קשה",
                    icon = Icons.Default.Star,
                    activeColor = StarGold,
                    isActive = currentWord?.isHard == true,
                    onClick = {
                        val newState = !(currentWord?.isHard ?: false)
                        markCurrentWord(hard = newState)
                    }
                )
            }

            // כפתור המילה הבאה החכמה
            Button(
                onClick = {
                    markCurrentWord()
                    advanceToNextWord(exitDirection = 0f)
                },
                modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .height(58.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .border(2.dp, TealNeon, RoundedCornerShape(30.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(TealDark, CardDark, TealDark)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "המילה הבאה בתור החכם",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedCounterBadge(title: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                (slideInVertically { -it } + fadeIn()).togetherWith(slideOutVertically { it } + fadeOut())
            },
            label = "counterAnim"
        ) { targetCount ->
            Text(
                text = "$targetCount",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = title, fontSize = 12.sp, color = Color.LightGray)
    }
}

@Composable
fun BouncyActionIcon(
    label: String,
    icon: ImageVector,
    activeColor: Color,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.84f
            isActive -> 1.1f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "bouncyButtonScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(pressScale)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(if (isActive) activeColor.copy(alpha = 0.22f) else CardDark)
                .border(
                    width = 2.dp,
                    color = if (isActive) activeColor else Color.Gray.copy(alpha = 0.35f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) activeColor else Color.LightGray,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) activeColor else Color.Gray
        )
    }
}
