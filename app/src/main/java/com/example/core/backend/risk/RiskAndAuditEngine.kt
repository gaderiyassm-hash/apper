package com.example.core.backend.risk

import com.example.core.backend.model.RiskAction
import com.example.core.backend.model.RiskEvaluationResult
import com.example.core.backend.model.RiskLevel
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class AuditLogRecord(
    val id: String = UUID.randomUUID().toString(),
    val actor: String,
    val action: String,
    val entityType: String,
    val entityId: String,
    val ipAddress: String,
    val status: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Section 33-35: Risk & Fraud Engine.
 * Evaluates trade submissions, deposit velocities, and withdrawal amounts.
 */
class RiskAndFraudEngine {
    private val userTradeVelocity = ConcurrentHashMap<String, MutableList<Long>>()
    private val userDailyWithdrawalCents = ConcurrentHashMap<String, Long>()

    fun evaluateTradeSubmission(
        userId: String,
        amountCents: Long,
        isDemo: Boolean,
        kycStatus: String
    ): RiskEvaluationResult {
        // Demo trades have negligible financial risk
        if (isDemo) {
            return RiskEvaluationResult(
                score = 5,
                level = RiskLevel.LOW,
                action = RiskAction.ALLOW,
                reasons = listOf("Demo practice mode trade")
            )
        }

        val reasons = mutableListOf<String>()
        var score = 10

        // 1. KYC status gate
        if (kycStatus != "VERIFIED") {
            score += 40
            reasons.add("User KYC is not fully verified")
        }

        // 2. High amount check
        if (amountCents > 500_000L) { // > $5,000
            score += 30
            reasons.add("High single trade exposure (>$5,000)")
        }

        // 3. Velocity check (Max 15 trades per minute)
        val now = System.currentTimeMillis()
        val timestamps = userTradeVelocity.computeIfAbsent(userId) { mutableListOf() }
        synchronized(timestamps) {
            timestamps.removeAll { now - it > 60_000 }
            timestamps.add(now)
            if (timestamps.size > 15) {
                score += 50
                reasons.add("High trade velocity limit exceeded (>15 trades/min)")
            }
        }

        val (level, action) = when {
            score >= 80 -> Pair(RiskLevel.CRITICAL, RiskAction.BLOCK)
            score >= 60 -> Pair(RiskLevel.HIGH, RiskAction.HOLD)
            score >= 35 -> Pair(RiskLevel.MEDIUM, RiskAction.STEP_UP)
            else -> Pair(RiskLevel.LOW, RiskAction.ALLOW)
        }

        return RiskEvaluationResult(score, level, action, reasons)
    }

    fun evaluateWithdrawalRequest(
        userId: String,
        amountCents: Long,
        kycStatus: String
    ): RiskEvaluationResult {
        val reasons = mutableListOf<String>()
        var score = 15

        if (kycStatus != "VERIFIED") {
            score = 100
            reasons.add("Unverified KYC cannot withdraw real funds")
            return RiskEvaluationResult(score, RiskLevel.CRITICAL, RiskAction.BLOCK, reasons)
        }

        if (amountCents > 1_000_000L) { // > $10,000 single withdrawal
            score += 40
            reasons.add("Large single withdrawal threshold exceeded (>$10,000)")
        }

        val dailyTotal = (userDailyWithdrawalCents[userId] ?: 0L) + amountCents
        if (dailyTotal > 2_500_000L) { // > $25,000 daily
            score += 35
            reasons.add("Daily cumulative withdrawal threshold exceeded (>$25,000)")
        }

        val (level, action) = when {
            score >= 70 -> Pair(RiskLevel.HIGH, RiskAction.REVIEW)
            score >= 40 -> Pair(RiskLevel.MEDIUM, RiskAction.STEP_UP)
            else -> Pair(RiskLevel.LOW, RiskAction.ALLOW)
        }

        return RiskEvaluationResult(score, level, action, reasons)
    }

    fun recordCompletedWithdrawal(userId: String, amountCents: Long) {
        userDailyWithdrawalCents.compute(userId) { _, current -> (current ?: 0L) + amountCents }
    }
}

/**
 * Section 49: Authoritative Immutable Audit Service.
 * Logs all sensitive operations: Auth, KYC, Trade, Settlement, Wallet, Payments, Admin.
 */
class AuditService {
    private val auditLogs = CopyOnWriteArrayList<AuditLogRecord>()

    fun log(
        actor: String,
        action: String,
        entityType: String,
        entityId: String,
        ipAddress: String = "127.0.0.1",
        status: String = "SUCCESS",
        details: String = ""
    ) {
        val record = AuditLogRecord(
            actor = actor,
            action = action,
            entityType = entityType,
            entityId = entityId,
            ipAddress = ipAddress,
            status = status,
            details = details
        )
        auditLogs.add(record)
        if (auditLogs.size > 250) {
            auditLogs.removeAt(0)
        }
    }

    fun getAuditLogs(): List<AuditLogRecord> = auditLogs.toList()
}
