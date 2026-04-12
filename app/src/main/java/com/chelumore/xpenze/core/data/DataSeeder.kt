package com.chelumore.xpenze.core.data

import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.model.AccountType
import com.chelumore.xpenze.domain.repository.AccountRepository
import com.chelumore.xpenze.domain.repository.CategoryRepository
import com.chelumore.xpenze.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

@Singleton
class DataSeeder @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val userRepository: UserRepository
) {

    suspend fun seedIfEmpty() {
        seedCategories()
        bootstrapOnboardingIfNeeded()
        seedAccountsIfOnboardingComplete()
    }

    /**
     * Simple migration for older installs / dev data:
     * if accounts already exist, consider onboarding "complete" so users don't get stuck in setup.
     */
    private suspend fun bootstrapOnboardingIfNeeded() {
        val prefs = userRepository.userPreferences.first()
        if (prefs.isOnboardingComplete) return

        val existing = accountRepository.getAll().firstOrNull().orEmpty()
        if (existing.isEmpty()) return

        val defaultAccount = existing.firstOrNull { it.isDefault } ?: existing.first()
        userRepository.setDefaultCurrency(defaultAccount.currency)
        userRepository.setDefaultAccountId(defaultAccount.id)
        userRepository.setOnboardingComplete(true)
    }

    private suspend fun seedCategories() {
        val prefs = userRepository.userPreferences.first()

        // If the user is signed in, don't seed local defaults: cloud sync will pull the canonical set.
        // Seeding here causes duplicate categories on new devices (different IDs).
        if (!prefs.userId.isNullOrBlank()) return

        categoryRepository.seedDefaultCategories()
    }

    private suspend fun seedAccountsIfOnboardingComplete() {
        val prefs = userRepository.userPreferences.first()
        if (!prefs.isOnboardingComplete) return
        if (!prefs.userId.isNullOrBlank()) return

        val existing = accountRepository.getAll().firstOrNull()
        if (existing.isNullOrEmpty()) {
            val now = System.currentTimeMillis()
            val cashAccount = Account(
                id = UUID.randomUUID().toString(),
                name = "Cash",
                type = AccountType.CASH,
                currency = prefs.defaultCurrency,
                initialBalanceCents = 0,
                currentBalanceCents = 0,
                colorHex = "#81C784", // Green
                iconName = "wallet",
                isDefault = true,
                createdAt = now,
                updatedAt = now
            )
            accountRepository.insert(cashAccount)
        }
    }

    // Category seeding is handled in CategoryRepositoryImpl.seedDefaultCategories()
}

