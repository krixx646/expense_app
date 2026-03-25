package com.chelumore.xpenze.core.ui.components.ads

import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.chelumore.xpenze.ui.theme.InfoBlue
import kotlinx.coroutines.delay

/**
 * Free-tier banner slot with safe test-ad behavior in debug builds.
 */
@Composable
fun FreeTierAdSlot(
    hasPremiumAccess: Boolean,
    placement: AdPlacement,
    modifier: Modifier = Modifier
) {
    if (hasPremiumAccess) return

    val adUnitId = remember(placement) { AdMobConfig.bannerUnitId(placement) }
    var adLoadFailed by remember(adUnitId) { mutableStateOf(false) }
    var adRuntimeError by remember(adUnitId) { mutableStateOf(false) }
    var shouldRenderAd by remember(adUnitId) { mutableStateOf(false) }
    var adViewRef by remember { mutableStateOf<AdView?>(null) }

    LaunchedEffect(adUnitId) {
        // Defer SDK view creation slightly to reduce launch jank.
        delay(450)
        shouldRenderAd = true
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { adViewRef?.destroy() }
            adViewRef = null
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Sponsored",
                    color = InfoBlue,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            if (shouldRenderAd) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp)
                        ),
                    factory = { context ->
                        runCatching<View> {
                            AdView(context).apply {
                                adViewRef = this
                                setAdSize(AdSize.BANNER)
                                this.adUnitId = adUnitId
                                adListener = object : AdListener() {
                                    override fun onAdLoaded() {
                                        adRuntimeError = false
                                        adLoadFailed = false
                                    }

                                    override fun onAdFailedToLoad(error: LoadAdError) {
                                        adLoadFailed = true
                                    }
                                }
                                loadAd(AdRequest.Builder().build())
                            }
                        }.getOrElse {
                            adRuntimeError = true
                            adLoadFailed = true
                            FrameLayout(context)
                        }
                    }
                )
            } else {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                )
            }
            if (adRuntimeError) {
                Text(
                    text = "Ad slot unavailable on this device.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (adLoadFailed) {
                Text(
                    text = "Ad unavailable right now. Retrying automatically.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = "Free plan ad slot. Upgrade to Premium to remove ads.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
