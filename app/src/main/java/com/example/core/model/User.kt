package com.example.core.model

enum class KycStatus {
    NOT_STARTED,
    IN_PROGRESS,
    UNDER_REVIEW,
    VERIFIED,
    REJECTED,
    RESUBMISSION_REQUIRED
}

enum class AuthState {
    FIRST_INSTALL,
    UNAUTHENTICATED,
    AUTHENTICATED,
    SESSION_EXPIRED,
    MAINTENANCE,
    BLOCKED
}

data class UserProfile(
    val id: String = "usr_998124",
    val email: String = "trader@apper.io",
    val fullName: String = "Alex Chen",
    val phone: String = "+1 (555) 392-1049",
    val country: String = "United States",
    val isDemoMode: Boolean = true,
    val kycStatus: KycStatus = KycStatus.UNDER_REVIEW,
    val mfaEnabled: Boolean = true,
    val loginAlertsEnabled: Boolean = true,
    val joinedDate: String = "September 2026",
    val activeSessionCount: Int = 2
)
