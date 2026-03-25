package com.chelumore.xpenze.core.ui.components.growth

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chelumore.xpenze.ui.theme.YellowPrimary

@Composable
fun AnimatedMochiCatHero(
    modifier: Modifier = Modifier,
    headline: String = "Meow! I can help you save more this month."
) {
    val motion = rememberInfiniteTransition(label = "mochiMotion")
    val bobY = motion.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mochiBobY"
    )
    val pawWave = motion.animateFloat(
        initialValue = -8f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pawWave"
    )

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🐾",
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = headline,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "🐱",
                fontSize = 76.sp,
                modifier = Modifier
                    .offset(y = bobY.value.dp)
                    .graphicsLayer {
                        scaleX = 1.02f
                        scaleY = 1.02f
                    }
            )
            Text(
                text = "✨",
                fontSize = 18.sp,
                modifier = Modifier
                    .offset(x = 40.dp, y = (-14).dp)
                    .graphicsLayer { rotationZ = pawWave.value }
            )
            Text(
                text = "🐾",
                fontSize = 20.sp,
                modifier = Modifier
                    .offset(x = 44.dp, y = 10.dp)
                    .graphicsLayer { rotationZ = pawWave.value }
            )
        }
        Box(
            modifier = Modifier
                .size(width = 96.dp, height = 4.dp)
                .background(YellowPrimary.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
        )
    }
}

