package com.example.englishtutor

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// צבעי הממשק הכהה / ניאון בהשראת העיצוב
val DarkBg = Color(0xFF0F111A)
val TealNeon = Color(0xFF00F0FF)
val TealDark = Color(0xFF003B46)
val GrayNeon = Color(0xFF1E2235)
val StarYellow = Color(0xFFFFD700)
val CheckGreen = Color(0xFF00E676)

@Composable
fun FlashcardScreen(words: List<WordCard>, repository: WordRepository) {
    var currentIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    
    // מעקב אחר שינויים במצב המילה הנוכחית כדי לעדכן את הכפתורים
    var forceUpdate by remember { mutableStateOf(0) }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "flipAnimation"
    )

    if (words.isEmpty()) return

    val currentWord = words[currentIndex]

    // פונקציה שעוברת למילה הבאה ומאפסת את מצב ההיפוך
    fun nextWord() {
        isFlipped = false // **פתרון הבאג:** קודם כל מאפסים את ההיפוך
        if (currentIndex < words.size - 1) {
            currentIndex++
        } else {
            currentIndex = 0 // חזרה להתחלה
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // חלק עליון: כותרת ומספר כרטיס
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "לימוד אנגלית",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "כרטיס ${currentIndex + 1} מתוך ${words.size}",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        // חלק מרכזי: כרטיסיית המילה
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(350.dp)
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 14f * density
                    }
                    .clickable { isFlipped = !isFlipped }
                    .border(2.dp, TealNeon, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = GrayNeon),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (rotation <= 90f) {
                        // צד קדמי: אנגלית
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = currentWord.english,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealNeon
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "לחץ להצגת התרגום",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    } else {
                        // צד אחורי: עברית
                        Column(
                            modifier = Modifier.graphicsLayer { rotationY = 180f },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = currentWord.hebrew,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "תרגום",
                                fontSize = 14.sp,
                                color = TealNeon.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // חלק תחתון: כפתורי פעולה ומעבר
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // כפתורי ה"סימון" (נלמד וקשה) המעוצבים אישית
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButtonCustom(
                    label = "נלמד",
                    icon = Icons.Default.Check,
                    activeColor = CheckGreen,
                    isActive = currentWord.isLearned,
                    onClick = {
                        currentWord.isLearned = !currentWord.isLearned
                        repository.saveWordState(currentWord)
                        forceUpdate++ // רענון מסך
                        nextWord() // עובר אוטומטית למילה הבאה
                    }
                )

                IconButtonCustom(
                    label = "קשה",
                    icon = Icons.Default.Star,
                    activeColor = StarYellow,
                    isActive = currentWord.isHard,
                    onClick = {
                        currentWord.isHard = !currentWord.isHard
                        repository.saveWordState(currentWord)
                        forceUpdate++
                    }
                )
            }

            // כפתור המילה הבאה - מעוצב מטאלי וניאון
            Button(
                onClick = { nextWord() },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(60.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .border(2.dp, TealNeon, RoundedCornerShape(30.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(TealDark, GrayNeon)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "המילה הבאה",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// קומפוננטת כפתור אייקון אינטראקטיבי מעוצב
@Composable
fun IconButtonCustom(
    label: String,
    icon: ImageVector,
    activeColor: Color,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(if (isActive) activeColor.copy(alpha = 0.2f) else GrayNeon)
                .border(2.dp, if (isActive) activeColor else Color.Gray.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) activeColor else Color.Gray,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isActive) activeColor else Color.Gray
        )
    }
}
