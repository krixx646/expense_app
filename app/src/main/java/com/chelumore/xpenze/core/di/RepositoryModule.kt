package com.chelumore.xpenze.core.di

import com.chelumore.xpenze.data.repository.AccountRepositoryImpl
import com.chelumore.xpenze.data.repository.AutoCaptureDraftRepositoryImpl
import com.chelumore.xpenze.data.repository.AuthRepositoryImpl
import com.chelumore.xpenze.data.repository.BudgetRepositoryImpl
import com.chelumore.xpenze.data.repository.CategoryRepositoryImpl
import com.chelumore.xpenze.data.repository.RecurringRuleRepositoryImpl
import com.chelumore.xpenze.data.repository.SubscriptionRepositoryImpl
import com.chelumore.xpenze.data.repository.TransactionRepositoryImpl
import com.chelumore.xpenze.data.repository.UserRepositoryImpl
import com.chelumore.xpenze.data.repository.FirestoreSyncRepositoryImpl
import com.chelumore.xpenze.domain.repository.AccountRepository
import com.chelumore.xpenze.domain.repository.AutoCaptureDraftRepository
import com.chelumore.xpenze.domain.repository.AuthRepository
import com.chelumore.xpenze.domain.repository.BudgetRepository
import com.chelumore.xpenze.domain.repository.CategoryRepository
import com.chelumore.xpenze.domain.repository.RecurringRuleRepository
import com.chelumore.xpenze.domain.repository.SubscriptionRepository
import com.chelumore.xpenze.domain.repository.TransactionRepository
import com.chelumore.xpenze.domain.repository.UserRepository
import com.chelumore.xpenze.domain.repository.SyncRepository
import com.chelumore.xpenze.core.billing.BillingService
import com.chelumore.xpenze.core.billing.PlayBillingService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(
        impl: AccountRepositoryImpl
    ): AccountRepository

    @Binds
    @Singleton
    abstract fun bindAutoCaptureDraftRepository(
        impl: AutoCaptureDraftRepositoryImpl
    ): AutoCaptureDraftRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        impl: CategoryRepositoryImpl
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(
        impl: BudgetRepositoryImpl
    ): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindRecurringRuleRepository(
        impl: RecurringRuleRepositoryImpl
    ): RecurringRuleRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(
        impl: SubscriptionRepositoryImpl
    ): SubscriptionRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(
        impl: FirestoreSyncRepositoryImpl
    ): SyncRepository

    @Binds
    @Singleton
    abstract fun bindBillingService(
        impl: PlayBillingService
    ): BillingService
}

