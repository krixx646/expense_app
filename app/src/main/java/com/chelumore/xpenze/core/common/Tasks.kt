package com.chelumore.xpenze.core.common

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Minimal Task.await() without adding kotlinx-coroutines-play-services.
 */
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            cont.resume(task.result)
        } else {
            cont.resumeWithException(task.exception ?: IllegalStateException("Task failed"))
        }
    }
}

suspend fun Task<*>.awaitCompletion() = suspendCancellableCoroutine<Unit> { cont ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            cont.resume(Unit)
        } else {
            cont.resumeWithException(task.exception ?: IllegalStateException("Task failed"))
        }
    }
}


