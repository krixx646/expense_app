package com.chelumore.xpenze.core.autocapture

import com.chelumore.xpenze.core.notifications.AutoCaptureDraftNotifier
import com.chelumore.xpenze.domain.repository.UserRepository
import com.chelumore.xpenze.domain.usecase.autocapture.CaptureNotificationToDraftUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationCaptureServiceEntryPoint {
    fun userRepository(): UserRepository
    fun captureNotificationToDraftUseCase(): CaptureNotificationToDraftUseCase
    fun autoCaptureDraftNotifier(): AutoCaptureDraftNotifier
}
