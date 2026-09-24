package com.example.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeDao {
    @Query("SELECT * FROM trades ORDER BY createdAt DESC")
    fun getAllTrades(): Flow<List<TradeEntity>>

    @Query("SELECT * FROM trades WHERE status = 'ACTIVE' OR status = 'EXPIRING' ORDER BY serverExpiryTimestamp ASC")
    fun getActiveTrades(): Flow<List<TradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: TradeEntity)

    @Update
    suspend fun updateTrade(trade: TradeEntity)

    @Query("SELECT * FROM trades WHERE id = :id LIMIT 1")
    suspend fun getTradeById(id: String): TradeEntity?

    @Query("SELECT * FROM trades WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getTradeByIdempotencyKey(key: String): TradeEntity?
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()
}

@Dao
interface FavoriteMarketDao {
    @Query("SELECT * FROM favorite_markets")
    fun getFavorites(): Flow<List<FavoriteMarketEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setFavorite(fav: FavoriteMarketEntity)

    @Query("DELETE FROM favorite_markets WHERE symbol = :symbol")
    suspend fun removeFavorite(symbol: String)
}
