package com.chelumore.xpenze.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chelumore.xpenze.core.database.entity.UserSubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscription: UserSubscriptionEntity)

    @Update
    suspend fun update(subscription: UserSubscriptionEntity)

    @Query("SELECT * FROM user_subscriptions WHERE isActive = 1 ORDER BY expiryTime DESC LIMIT 1")
    fun getActive(): Flow<UserSubscriptionEntity?>

    @Query("SELECT * FROM user_subscriptions WHERE isActive = 1 ORDER BY expiryTime DESC LIMIT 1")
    suspend fun getActiveOnce(): UserSubscriptionEntity?

    @Query("SELECT * FROM user_subscriptions WHERE productId = :productId LIMIT 1")
    suspend fun getByProductId(productId: String): UserSubscriptionEntity?

    @Query("SELECT * FROM user_subscriptions ORDER BY expiryTime DESC")
    fun getAll(): Flow<List<UserSubscriptionEntity>>

    @Query("UPDATE user_subscriptions SET isActive = 0 WHERE expiryTime < :currentTime")
    suspend fun deactivateExpired(currentTime: Long)

    @Query("UPDATE user_subscriptions SET acknowledgedAt = :acknowledgedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markAcknowledged(id: String, acknowledgedAt: Long, updatedAt: Long)

    @Query("DELETE FROM user_subscriptions")
    suspend fun deleteAll()
}

