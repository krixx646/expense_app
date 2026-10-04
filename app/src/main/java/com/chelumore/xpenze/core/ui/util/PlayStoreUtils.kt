package com.chelumore.xpenze.core.ui.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.chelumore.xpenze.core.common.await
import com.chelumore.xpenze.core.common.awaitCompletion
import com.google.android.play.core.review.ReviewManagerFactory

fun Context.openPlayStoreForRating(): Boolean {
    val marketIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=$packageName")
    ).apply {
        setPackage("com.android.vending")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val webIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return runCatching {
        startActivity(marketIntent)
        true
    }.recoverCatching {
        startActivity(webIntent)
        true
    }.getOrDefault(false)
}

/**
 * Prefers Google's in-app review sheet. Falls back to the public Play Store listing only when
 * the Activity is missing or the Play Core review API fails.
 *
 * The API does not report whether the user actually submitted a rating.
 */
suspend fun Context.launchPlayStoreReview(activity: Activity?): Boolean {
    if (activity == null || activity.isFinishing || activity.isDestroyed) {
        return openPlayStoreForRating()
    }

    return runCatching {
        val manager = ReviewManagerFactory.create(this)
        val reviewInfo = manager.requestReviewFlow().await()
        manager.launchReviewFlow(activity, reviewInfo).awaitCompletion()
        true
    }.getOrElse { error ->
        Log.w(TAG, "In-app review unavailable; opening Play Store listing", error)
        openPlayStoreForRating()
    }
}

private const val TAG = "PlayStoreReview"
