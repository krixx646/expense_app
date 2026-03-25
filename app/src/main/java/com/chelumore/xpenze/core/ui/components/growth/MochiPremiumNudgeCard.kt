package com.chelumore.xpenze.core.ui.components.growth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chelumore.xpenze.core.growth.MochiPremiumCopy
import com.chelumore.xpenze.ui.theme.BlackBackground
import com.chelumore.xpenze.ui.theme.YellowPrimary

@Composable
fun MochiPremiumNudgeCard(
    copy: MochiPremiumCopy,
    modifier: Modifier = Modifier,
    primaryCtaText: String? = null,
    onPrimaryCta: (() -> Unit)? = null,
    secondaryCtaText: String? = null,
    onSecondaryCta: (() -> Unit)? = null
) {
    val mascotMotion = rememberInfiniteTransition(label = "mochiNudgeMascot")
    val bobY = mascotMotion.animateFloat(
        initialValue = 0f,
        targetValue = -3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mochiNudgeBob"
    )
    val sparkleTilt = mascotMotion.animateFloat(
        initialValue = -10f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mochiNudgeSparkleTilt"
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = YellowPrimary.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🐱",
                    fontSize = 18.sp,
                    modifier = Modifier.offset(y = bobY.value.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "✨",
                    fontSize = 12.sp,
                    modifier = Modifier.rotate(sparkleTilt.value)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = copy.badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = copy.headline,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = copy.body,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = copy.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (primaryCtaText != null && onPrimaryCta != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Button(
                    onClick = onPrimaryCta,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YellowPrimary,
                        contentColor = BlackBackground
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(primaryCtaText, fontWeight = FontWeight.Bold)
                }
            }

            if (secondaryCtaText != null && onSecondaryCta != null) {
                OutlinedButton(
                    onClick = onSecondaryCta,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(secondaryCtaText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

