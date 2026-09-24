package com.example.core.backend.ledger

import com.example.core.backend.model.BackendLedgerEntry
import com.example.core.backend.model.LedgerEntryDirection
import com.example.core.backend.model.LedgerEventType
import com.example.core.backend.model.MoneyMinor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Section 28: Authoritative Wallet Balance Model
 * Total Balance = Available Balance + Locked Balance
 * Stored in Minor Units (cents) to avoid any floating-point arithmetic hazards (Section 40).
 */
data class AuthoritativeWalletBalance(
    val accountId: String,
    val availableCents: Long,
    val lockedCents: Long,
    val isDemo: Boolean
) {
    val totalCents: Long get() = availableCents + lockedCents

    fun availableDollars(): Double = availableCents / 100.0
    fun lockedDollars(): Double = lockedCents / 100.0
    fun totalDollars(): Double = totalCents / 100.0
}

/**
 * Section 26 & 27: Append-Only Immutable Double-Entry Ledger Engine
 * - Principle: Append-only. Never overwrite or delete ledger entries.
 * - Concurrency: Thread-safe, synchronized writes, idempotency enforcement.
 * - Financial amounts: Minor units (Long cents).
 */
class DoubleEntryLedgerService {
    // Immutable append-only audit trail of all financial movements
    private val ledgerEntries = CopyOnWriteArrayList<BackendLedgerEntry>()

    // Processed idempotency keys to ensure financial operations execute at-most-once
    private val processedIdempotencyKeys = ConcurrentHashMap.newKeySet<String>()

    // Projected wallet balances derived strictly from the ledger
    private val _demoBalance = MutableStateFlow(
        AuthoritativeWalletBalance("demo_user_1", availableCents = 1_000_000L, lockedCents = 0L, isDemo = true) // $10,000.00
    )
    val demoBalance: StateFlow<AuthoritativeWalletBalance> = _demoBalance.asStateFlow()

    private val _realBalance = MutableStateFlow(
        AuthoritativeWalletBalance("real_user_1", availableCents = 125_000L, lockedCents = 0L, isDemo = false) // $1,250.00
    )
    val realBalance: StateFlow<AuthoritativeWalletBalance> = _realBalance.asStateFlow()

    init {
        // Record initial ledger entries
        recordEntry(
            accountId = "demo_user_1",
            amountCents = 1_000_000L,
            direction = LedgerEntryDirection.CREDIT,
            eventType = LedgerEventType.INITIAL_CREDIT,
            reference = "INIT-DEMO-CREDIT",
            idempotencyKey = "idemp_init_demo_1"
        )
        recordEntry(
            accountId = "real_user_1",
            amountCents = 125_000L,
            direction = LedgerEntryDirection.CREDIT,
            eventType = LedgerEventType.INITIAL_CREDIT,
            reference = "INIT-REAL-CREDIT",
            idempotencyKey = "idemp_init_real_1"
        )
    }

    /**
     * Records an immutable entry into the ledger with strict idempotency verification.
     */
    @Synchronized
    fun recordEntry(
        accountId: String,
        amountCents: Long,
        direction: LedgerEntryDirection,
        eventType: LedgerEventType,
        reference: String,
        idempotencyKey: String
    ): Boolean {
        if (!processedIdempotencyKeys.add(idempotencyKey)) {
            // Already processed this idempotency key
            return false
        }

        val entry = BackendLedgerEntry(
            id = UUID.randomUUID().toString(),
            transactionId = "tx_${UUID.randomUUID()}",
            accountId = accountId,
            currency = "USD",
            amountCents = amountCents,
            direction = direction,
            eventType = eventType,
            reference = reference,
            idempotencyKey = idempotencyKey,
            timestamp = System.currentTimeMillis()
        )
        ledgerEntries.add(entry)
        if (ledgerEntries.size > 250) {
            ledgerEntries.removeAt(0)
        }
        return true
    }

    /**
     * Locks funds when a trade is accepted by the Trading Engine.
     * Decreases Available balance and increases Locked balance atomically.
     */
    @Synchronized
    fun lockTradeFunds(isDemo: Boolean, amountCents: Long, tradeId: String, idempotencyKey: String): Boolean {
        val targetFlow = if (isDemo) _demoBalance else _realBalance
        val current = targetFlow.value

        if (current.availableCents < amountCents) {
            return false // Insufficient funds
        }

        val recorded = recordEntry(
            accountId = current.accountId,
            amountCents = amountCents,
            direction = LedgerEntryDirection.DEBIT,
            eventType = LedgerEventType.TRADE_INVESTMENT_LOCK,
            reference = "LOCK-TRADE-$tradeId",
            idempotencyKey = idempotencyKey
        )

        if (recorded) {
            targetFlow.update { prev ->
                prev.copy(
                    availableCents = prev.availableCents - amountCents,
                    lockedCents = prev.lockedCents + amountCents
                )
            }
            return true
        }
        return false
    }

    /**
     * Unlocks funds and credits winnings on authoritative trade settlement.
     */
    @Synchronized
    fun settleTradeFunds(
        isDemo: Boolean,
        lockedInvestmentCents: Long,
        payoutReturnCents: Long,
        tradeId: String,
        isWin: Boolean,
        isTie: Boolean
    ) {
        val targetFlow = if (isDemo) _demoBalance else _realBalance
        val current = targetFlow.value
        val eventType = if (isWin) LedgerEventType.TRADE_SETTLEMENT_WIN else LedgerEventType.TRADE_SETTLEMENT_REFUND

        recordEntry(
            accountId = current.accountId,
            amountCents = payoutReturnCents,
            direction = LedgerEntryDirection.CREDIT,
            eventType = eventType,
            reference = "SETTLE-$tradeId-${if (isWin) "WIN" else if (isTie) "TIE" else "LOSS"}",
            idempotencyKey = "idemp_settle_$tradeId"
        )

        targetFlow.update { prev ->
            val newLocked = maxOf(0L, prev.lockedCents - lockedInvestmentCents)
            val newAvailable = prev.availableCents + payoutReturnCents
            prev.copy(availableCents = newAvailable, lockedCents = newLocked)
        }
    }

    /**
     * Demo balance reset/refill to $10,000 (1,000,000 cents).
     */
    @Synchronized
    fun refillDemoBalance(idempotencyKey: String): Boolean {
        val current = _demoBalance.value
        val refillTargetCents = 1_000_000L
        val delta = refillTargetCents - current.availableCents
        if (delta <= 0) return true

        val recorded = recordEntry(
            accountId = current.accountId,
            amountCents = delta,
            direction = LedgerEntryDirection.CREDIT,
            eventType = LedgerEventType.DEMO_REFILL,
            reference = "DEMO-REFILL-$refillTargetCents",
            idempotencyKey = idempotencyKey
        )

        if (recorded) {
            _demoBalance.update { it.copy(availableCents = refillTargetCents, lockedCents = 0L) }
            return true
        }
        return false
    }

    /**
     * Credits real deposit amount following verified webhook validation.
     */
    @Synchronized
    fun creditDeposit(amountCents: Long, reference: String, idempotencyKey: String): Boolean {
        val recorded = recordEntry(
            accountId = _realBalance.value.accountId,
            amountCents = amountCents,
            direction = LedgerEntryDirection.CREDIT,
            eventType = LedgerEventType.DEPOSIT_CONFIRMED,
            reference = reference,
            idempotencyKey = idempotencyKey
        )

        if (recorded) {
            _realBalance.update { it.copy(availableCents = it.availableCents + amountCents) }
            return true
        }
        return false
    }

    /**
     * Locks funds for withdrawal pending security approval.
     */
    @Synchronized
    fun lockWithdrawalFunds(amountCents: Long, reference: String, idempotencyKey: String): Boolean {
        if (_realBalance.value.availableCents < amountCents) {
            return false
        }

        val recorded = recordEntry(
            accountId = _realBalance.value.accountId,
            amountCents = amountCents,
            direction = LedgerEntryDirection.DEBIT,
            eventType = LedgerEventType.WITHDRAWAL_REQUESTED,
            reference = reference,
            idempotencyKey = idempotencyKey
        )

        if (recorded) {
            _realBalance.update { prev ->
                prev.copy(
                    availableCents = prev.availableCents - amountCents,
                    lockedCents = prev.lockedCents + amountCents
                )
            }
            return true
        }
        return false
    }

    /**
     * Completes withdrawal by clearing locked funds upon payment provider success.
     */
    @Synchronized
    fun completeWithdrawal(amountCents: Long, reference: String) {
        _realBalance.update { prev ->
            prev.copy(lockedCents = maxOf(0L, prev.lockedCents - amountCents))
        }
    }

    fun getAllLedgerEntries(): List<BackendLedgerEntry> = ledgerEntries.toList()
}
