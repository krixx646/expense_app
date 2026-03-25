package com.chelumore.xpenze.core.ui.components.display

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.ui.util.iconForName
import com.chelumore.xpenze.core.ui.util.parseHexColorOrFallback
import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.model.AccountType
import com.chelumore.xpenze.domain.repository.AccountCardStyle

@Composable
fun AccountCard(
    account: Account,
    style: AccountCardStyle = AccountCardStyle.MATTE,
    maskAmounts: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val alpha = if (account.isArchived) 0.6f else 1f
    val color = parseHexColorOrFallback(account.colorHex, MaterialTheme.colorScheme.primary)
    val scheme = MaterialTheme.colorScheme
    val typeVisual = accountTypeVisual(account.type, color)
    val styleTint = lerp(color, typeVisual.accent, 0.34f)
    val icon = iconForName(account.iconName)
    val typeLabel = account.type.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    val maskedNumber = "**** **** **** ${account.id.takeLast(4).padStart(4, '0')}"
    val palette = when (style) {
        AccountCardStyle.MATTE -> AccountCardPalette(
            background = Brush.linearGradient(
                colors = listOf(
                    styleTint.copy(alpha = 0.92f),
                    scheme.surfaceVariant.copy(alpha = 0.96f),
                    scheme.surface.copy(alpha = 0.98f)
                )
            ),
            textPrimary = scheme.onSurface,
            textSecondary = scheme.onSurfaceVariant,
            iconBackground = scheme.onSurface.copy(alpha = 0.12f),
            iconTint = scheme.onSurface,
            chipBackground = scheme.onSurface.copy(alpha = 0.14f),
            accentBadgeBackground = scheme.onSurface.copy(alpha = 0.14f),
            accentBadgeText = scheme.onSurface,
            decorativeStrong = lerp(scheme.onSurface.copy(alpha = 0.08f), typeVisual.accent.copy(alpha = 0.20f), 0.45f),
            decorativeSoft = lerp(scheme.onSurface.copy(alpha = 0.06f), typeVisual.accent.copy(alpha = 0.14f), 0.40f),
            borderColor = Color.Transparent,
            typeBadgeBackground = typeVisual.accent.copy(alpha = 0.22f),
            typeBadgeText = scheme.onSurface
        )

        AccountCardStyle.METALLIC -> AccountCardPalette(
            background = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFCED5DE),
                    lerp(Color(0xFF9AA6B6), typeVisual.accent, 0.18f),
                    Color(0xFF6F7C8D)
                )
            ),
            textPrimary = Color(0xFF0D1117),
            textSecondary = Color(0xFF2F3A49),
            iconBackground = Color.Black.copy(alpha = 0.16f),
            iconTint = Color(0xFFEAF0F7),
            chipBackground = Color.Black.copy(alpha = 0.16f),
            accentBadgeBackground = Color.Black.copy(alpha = 0.14f),
            accentBadgeText = Color.White,
            decorativeStrong = lerp(Color.White.copy(alpha = 0.22f), typeVisual.accent.copy(alpha = 0.20f), 0.25f),
            decorativeSoft = lerp(Color.Black.copy(alpha = 0.12f), typeVisual.accent.copy(alpha = 0.16f), 0.24f),
            borderColor = lerp(Color.White.copy(alpha = 0.26f), typeVisual.accent.copy(alpha = 0.34f), 0.35f),
            typeBadgeBackground = Color.Black.copy(alpha = 0.18f),
            typeBadgeText = Color.White
        )

        AccountCardStyle.NEON -> AccountCardPalette(
            background = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF0A1118),
                    Color(0xFF111C2B),
                    styleTint.copy(alpha = 0.84f)
                )
            ),
            textPrimary = Color(0xFFEAF2FF),
            textSecondary = Color(0xFFBED1E8),
            iconBackground = styleTint.copy(alpha = 0.30f),
            iconTint = Color.White,
            chipBackground = styleTint.copy(alpha = 0.38f),
            accentBadgeBackground = styleTint.copy(alpha = 0.30f),
            accentBadgeText = Color(0xFFEAF2FF),
            decorativeStrong = styleTint.copy(alpha = 0.24f),
            decorativeSoft = styleTint.copy(alpha = 0.18f),
            borderColor = styleTint.copy(alpha = 0.56f),
            typeBadgeBackground = typeVisual.accent.copy(alpha = 0.38f),
            typeBadgeText = Color(0xFFEAF2FF)
        )

        AccountCardStyle.SUNSET -> AccountCardPalette(
            background = Brush.linearGradient(
                colors = listOf(
                    lerp(Color(0xFFFFB36B), styleTint, 0.22f),
                    Color(0xFFFF7F7F),
                    Color(0xFFD94D8A)
                )
            ),
            textPrimary = Color(0xFFFFFBF7),
            textSecondary = Color(0xFFFFE2D4),
            iconBackground = Color.White.copy(alpha = 0.20f),
            iconTint = Color.White,
            chipBackground = Color.White.copy(alpha = 0.18f),
            accentBadgeBackground = Color.Black.copy(alpha = 0.16f),
            accentBadgeText = Color.White,
            decorativeStrong = Color.White.copy(alpha = 0.12f),
            decorativeSoft = Color(0xFFFFD7B8).copy(alpha = 0.14f),
            borderColor = Color.White.copy(alpha = 0.22f),
            typeBadgeBackground = Color.Black.copy(alpha = 0.18f),
            typeBadgeText = Color.White
        )

        AccountCardStyle.OCEAN -> AccountCardPalette(
            background = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF062A38),
                    lerp(Color(0xFF0B5E78), styleTint, 0.30f),
                    Color(0xFF2FB8C8)
                )
            ),
            textPrimary = Color(0xFFF2FDFF),
            textSecondary = Color(0xFFC7EEF2),
            iconBackground = Color.White.copy(alpha = 0.16f),
            iconTint = Color.White,
            chipBackground = Color.White.copy(alpha = 0.14f),
            accentBadgeBackground = Color.Black.copy(alpha = 0.18f),
            accentBadgeText = Color.White,
            decorativeStrong = Color(0xFF7CE6F4).copy(alpha = 0.14f),
            decorativeSoft = Color(0xFF031F2E).copy(alpha = 0.16f),
            borderColor = Color(0xFF8CEAF5).copy(alpha = 0.30f),
            typeBadgeBackground = Color(0xFF031F2E).copy(alpha = 0.30f),
            typeBadgeText = Color.White
        )

        AccountCardStyle.ROYAL -> AccountCardPalette(
            background = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF150B2E),
                    lerp(Color(0xFF3B1F6B), styleTint, 0.24f),
                    Color(0xFF6A36D9)
                )
            ),
            textPrimary = Color(0xFFF7F3FF),
            textSecondary = Color(0xFFD9CCFF),
            iconBackground = Color.White.copy(alpha = 0.16f),
            iconTint = Color.White,
            chipBackground = Color.White.copy(alpha = 0.14f),
            accentBadgeBackground = Color.Black.copy(alpha = 0.18f),
            accentBadgeText = Color.White,
            decorativeStrong = Color(0xFFB7A2FF).copy(alpha = 0.14f),
            decorativeSoft = Color(0xFF090413).copy(alpha = 0.18f),
            borderColor = Color(0xFFD7C8FF).copy(alpha = 0.26f),
            typeBadgeBackground = Color(0xFF0D061C).copy(alpha = 0.28f),
            typeBadgeText = Color.White
        )
    }
    val chipShape = when (account.type) {
        AccountType.CASH -> RoundedCornerShape(10.dp)
        AccountType.BANK -> RoundedCornerShape(6.dp)
        AccountType.CREDIT_CARD -> RoundedCornerShape(8.dp)
        AccountType.SAVINGS -> RoundedCornerShape(999.dp)
        AccountType.INVESTMENT -> RoundedCornerShape(4.dp)
        AccountType.OTHER -> RoundedCornerShape(8.dp)
    }
    val chipSize = when (account.type) {
        AccountType.CASH -> 34.dp to 26.dp
        AccountType.BANK -> 40.dp to 24.dp
        AccountType.CREDIT_CARD -> 42.dp to 28.dp
        AccountType.SAVINGS -> 30.dp to 30.dp
        AccountType.INVESTMENT -> 38.dp to 24.dp
        AccountType.OTHER -> 36.dp to 24.dp
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .alpha(alpha),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, palette.borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 170.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(palette.background)
                .padding(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .offset(x = 64.dp, y = (-82).dp)
                    .clip(CircleShape)
                    .background(palette.decorativeStrong)
                    .align(Alignment.TopEnd)
            )
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(x = (-70).dp, y = 70.dp)
                    .clip(CircleShape)
                    .background(palette.decorativeSoft)
                    .align(Alignment.BottomStart)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.size(2.dp))
                        Text(
                            text = "$typeLabel • ${account.currency}",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textSecondary
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(palette.typeBadgeBackground)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = typeVisual.shortLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.typeBadgeText
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(palette.iconBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = palette.iconTint)
                        }
                    }
                }

                Spacer(modifier = Modifier.size(18.dp))
                Box(
                    modifier = Modifier
                        .size(width = chipSize.first, height = chipSize.second)
                        .clip(chipShape)
                        .background(palette.chipBackground)
                        .border(
                            border = BorderStroke(1.dp, palette.textPrimary.copy(alpha = 0.16f)),
                            shape = chipShape
                        )
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = maskedNumber,
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.size(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (maskAmounts) {
                        Text(
                            text = "••••••",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                    } else {
                        AmountText(
                            amountCents = account.currentBalanceCents,
                            currencyCode = account.currency,
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary
                        )
                    }

                    if (account.isDefault) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(palette.accentBadgeBackground)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.account_card_default_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.accentBadgeText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class AccountCardPalette(
    val background: Brush,
    val textPrimary: Color,
    val textSecondary: Color,
    val iconBackground: Color,
    val iconTint: Color,
    val chipBackground: Color,
    val accentBadgeBackground: Color,
    val accentBadgeText: Color,
    val decorativeStrong: Color,
    val decorativeSoft: Color,
    val borderColor: Color,
    val typeBadgeBackground: Color,
    val typeBadgeText: Color
)

private data class AccountTypeVisual(
    val accent: Color,
    val shortLabel: String
)

private fun accountTypeVisual(type: AccountType, baseColor: Color): AccountTypeVisual {
    return when (type) {
        AccountType.CASH -> AccountTypeVisual(
            accent = Color(0xFF22C55E),
            shortLabel = "CASH"
        )

        AccountType.BANK -> AccountTypeVisual(
            accent = Color(0xFF3B82F6),
            shortLabel = "BANK"
        )

        AccountType.CREDIT_CARD -> AccountTypeVisual(
            accent = Color(0xFF8B5CF6),
            shortLabel = "CARD"
        )

        AccountType.SAVINGS -> AccountTypeVisual(
            accent = Color(0xFF10B981),
            shortLabel = "SAVE"
        )

        AccountType.INVESTMENT -> AccountTypeVisual(
            accent = Color(0xFFF59E0B),
            shortLabel = "INV"
        )

        AccountType.OTHER -> AccountTypeVisual(
            accent = baseColor,
            shortLabel = "FLEX"
        )
    }
}


