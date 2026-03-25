package com.chelumore.xpenze.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.chelumore.xpenze.core.common.Constants
import com.chelumore.xpenze.core.database.dao.AccountDao
import com.chelumore.xpenze.core.database.dao.AutoCaptureDraftDao
import com.chelumore.xpenze.core.database.dao.BudgetDao
import com.chelumore.xpenze.core.database.dao.CategoryDao
import com.chelumore.xpenze.core.database.dao.RecurringRuleDao
import com.chelumore.xpenze.core.database.dao.SubscriptionDao
import com.chelumore.xpenze.core.database.dao.TransactionDao
import com.chelumore.xpenze.core.database.entity.AccountEntity
import com.chelumore.xpenze.core.database.entity.AutoCaptureDraftEntity
import com.chelumore.xpenze.core.database.entity.BudgetEntity
import com.chelumore.xpenze.core.database.entity.CategoryEntity
import com.chelumore.xpenze.core.database.entity.RecurringRuleEntity
import com.chelumore.xpenze.core.database.entity.TransactionEntity
import com.chelumore.xpenze.core.database.entity.UserSubscriptionEntity

@Database(
    entities = [
        TransactionEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        RecurringRuleEntity::class,
        UserSubscriptionEntity::class,
        AutoCaptureDraftEntity::class
    ],
    version = Constants.DATABASE_VERSION,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringRuleDao(): RecurringRuleDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun autoCaptureDraftDao(): AutoCaptureDraftDao
}

