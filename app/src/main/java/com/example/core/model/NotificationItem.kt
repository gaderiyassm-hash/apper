package com.example.core.model

enum class NotificationCategory {
    ALL,
    SECURITY,
    TRADING,
    PAYMENT,
    ACCOUNT,
    SYSTEM
}

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val category: NotificationCategory,
    val timestamp: Long,
    val isRead: Boolean = false,
    val actionRoute: String? = null
)
