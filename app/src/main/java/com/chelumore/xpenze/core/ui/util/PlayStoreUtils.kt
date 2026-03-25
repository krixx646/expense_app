package com.chelumore.xpenze.core.ui.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.chelumore.xpenze.core.common.await
import com.chelumore.xpenze.core.common.awaitCompletion
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.CancellationException

fun Context.openPlayStoreForRating(): Boolean {
    val marketIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=$packageName")
    ).apply {
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

suspend fun Context.launchPlayStoreReview(activity: Activity?): Boolean {
    if (activity == null) {
        return openPlayStoreForRating()
    }

    val reviewManager = ReviewManagerFactory.create(applicationContext)
    return try {
        val reviewInfo = reviewManager.requestReviewFlow().await()
        reviewManager.launchReviewFlow(activity, reviewInfo).awaitCompletion()
        true
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        openPlayStoreForRating()
    }
}
