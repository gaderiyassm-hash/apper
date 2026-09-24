package com.example.core.storage

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core.model.NotificationCategory
import com.example.core.model.PaymentMethod
import com.example.core.model.Trade
import com.example.core.model.TradeDirection
import com.example.core.model.TradeOutcome
import com.example.core.model.TradeStatus
import com.example.core.model.TransactionItem
import com.example.core.model.TransactionStatus
import com.example.core.model.TransactionType

@Entity(tableName = "trades")
data class TradeEntity(
    @PrimaryKey val id: String,
    val idempotencyKey: String,
    val assetSymbol: String,
    val assetName: String,
    val direction: String,
    val amount: Double,
    val entryPrice: Double,
    val durationSeconds: Long,
    val createdAt: Long,
    val serverExpiryTimestamp: Long,
    val status: String,
    val settlementPrice: Double?,
    val payoutRate: Double,
    val payoutAmount: Double,
    val outcome: String,
    val isDemo: Boolean
) {
    fun toDomain(): Trade = Trade(
        id = id,
        idempotencyKey = idempotencyKey,
        assetSymbol = assetSymbol,
        assetName = assetName,
        direction = TradeDirection.valueOf(direction),
        amount = amount,
        entryPrice = entryPrice,
        durationSeconds = durationSeconds,
        createdAt = createdAt,
        serverExpiryTimestamp = serverExpiryTimestamp,
        status = TradeStatus.valueOf(status),
        settlementPrice = settlementPrice,
        payoutRate = payoutRate,
        payoutAmount = payoutAmount,
        outcome = TradeOutcome.valueOf(outcome),
        isDemo = isDemo
    )

    companion object {
        fun fromDomain(trade: Trade): TradeEntity = TradeEntity(
            id = trade.id,
            idempotencyKey = trade.idempotencyKey,
            assetSymbol = trade.assetSymbol,
            assetName = trade.assetName,
            direction = trade.direction.name,
            amount = trade.amount,
            entryPrice = trade.entryPrice,
            durationSeconds = trade.durationSeconds,
            createdAt = trade.createdAt,
            serverExpiryTimestamp = trade.serverExpiryTimestamp,
            status = trade.status.name,
            settlementPrice = trade.settlementPrice,
            payoutRate = trade.payoutRate,
            payoutAmount = trade.payoutAmount,
            outcome = trade.outcome.name,
            isDemo = trade.isDemo
        )
    }
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amount: Double,
    val currency: String,
    val status: String,
    val reference: String,
    val timestamp: Long,
    val paymentMethod: String?,
    val note: String
) {
    fun toDomain(): TransactionItem = TransactionItem(
        id = id,
        type = TransactionType.valueOf(type),
        amount = amount,
        currency = currency,
        status = TransactionStatus.valueOf(status),
        reference = reference,
        timestamp = timestamp,
        paymentMethod = paymentMethod?.let { runCatching { PaymentMethod.valueOf(it) }.getOrNull() },
        note = note
    )

    companion object {
        fun fromDomain(item: TransactionItem): TransactionEntity = TransactionEntity(
            id = item.id,
            type = item.type.name,
            amount = item.amount,
            currency = item.currency,
            status = item.status.name,
            reference = item.reference,
            timestamp = item.timestamp,
            paymentMethod = item.paymentMethod?.name,
            note = item.note
        )
    }
}

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val category: String,
    val timestamp: Long,
    val isRead: Boolean,
    val actionRoute: String?
)

@Entity(tableName = "favorite_markets")
data class FavoriteMarketEntity(
    @PrimaryKey val symbol: String,
    val isFavorite: Boolean
)
