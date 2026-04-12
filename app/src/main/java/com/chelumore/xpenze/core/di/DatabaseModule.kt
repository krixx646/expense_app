package com.chelumore.xpenze.core.di

import android.content.Context
import androidx.room.Room
import com.chelumore.xpenze.core.common.Constants
import com.chelumore.xpenze.core.database.AppDatabase
import com.chelumore.xpenze.core.database.Migrations
import com.chelumore.xpenze.core.database.dao.AccountDao
import com.chelumore.xpenze.core.database.dao.AutoCaptureDraftDao
import com.chelumore.xpenze.core.database.dao.BudgetDao
import com.chelumore.xpenze.core.database.dao.CategoryDao
import com.chelumore.xpenze.core.database.dao.RecurringRuleDao
import com.chelumore.xpenze.core.database.dao.SubscriptionDao
import com.chelumore.xpenze.core.database.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            Constants.DATABASE_NAME
        )
            .addMigrations(
                Migrations.MIGRATION_1_2,
                Migrations.MIGRATION_2_3
            )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideTransactionDao(database: AppDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    @Singleton
    fun provideAccountDao(database: AppDatabase): AccountDao {
        return database.accountDao()
    }

    @Provides
    @Singleton
    fun provideCategoryDao(database: AppDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    @Singleton
    fun provideBudgetDao(database: AppDatabase): BudgetDao {
        return database.budgetDao()
    }

    @Provides
    @Singleton
    fun provideRecurringRuleDao(database: AppDatabase): RecurringRuleDao {
        return database.recurringRuleDao()
    }

    @Provides
    @Singleton
    fun provideSubscriptionDao(database: AppDatabase): SubscriptionDao {
        return database.subscriptionDao()
    }

    @Provides
    @Singleton
    fun provideAutoCaptureDraftDao(database: AppDatabase): AutoCaptureDraftDao {
        return database.autoCaptureDraftDao()
    }
}
