package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class PlanTier(val label: String, val monthlyPriceInr: Double, val yearlyPriceInr: Double) {
    FREE("Free", 0.0, 0.0),
    PREMIUM("Premium", 100.0, 1000.0),
    ENTERPRISE("Enterprise", 200.0, 2000.0);

    companion object {
        fun fromRaw(raw: String): PlanTier = entries.firstOrNull { it.name == raw } ?: FREE
    }
}

enum class BillingCycle(val label: String) {
    MONTHLY("Monthly"),
    YEARLY("Yearly");

    companion object {
        fun fromRaw(raw: String): BillingCycle = entries.firstOrNull { it.name == raw } ?: MONTHLY
    }
}

enum class SubscriptionStatus(val label: String) {
    ACTIVE("Active"),
    TRIAL("Trial"),
    EXPIRED("Expired"),
    CANCELLED("Cancelled");

    companion object {
        fun fromRaw(raw: String): SubscriptionStatus = entries.firstOrNull { it.name == raw } ?: ACTIVE
    }
}

enum class AccountStatus(val label: String) {
    ACTIVE("Active"),
    BLOCKED("Blocked");

    companion object {
        fun fromRaw(raw: String): AccountStatus = entries.firstOrNull { it.name == raw } ?: ACTIVE
    }
}

enum class PaymentStatus(val label: String) {
    SUCCESS("Successful"),
    FAILED("Failed"),
    PENDING("Pending"),
    REFUNDED("Refunded");

    companion object {
        fun fromRaw(raw: String): PaymentStatus = entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

enum class NotificationAudience(val label: String) {
    ALL("All Restaurants"),
    SELECTED("Selected Restaurants"),
    FREE("Free Plan Users"),
    PREMIUM("Premium Plan Users"),
    ENTERPRISE("Enterprise Plan Users");

    companion object {
        fun fromRaw(raw: String): NotificationAudience = entries.firstOrNull { it.name == raw } ?: ALL
    }
}

enum class NotificationCategory(val label: String) {
    PAYMENT_REMINDER("Payment Reminder"),
    EXPIRY_REMINDER("Subscription Expiry Reminder"),
    NEW_FEATURE("New Feature Announcement"),
    APP_UPDATE("App Update Notification"),
    PROMOTIONAL("Promotional Notification");

    companion object {
        fun fromRaw(raw: String): NotificationCategory = entries.firstOrNull { it.name == raw } ?: PROMOTIONAL
    }
}

enum class EmployeeRole(val label: String) {
    SUPER_ADMIN("Super Admin"),
    BILLING_MANAGER("Billing Manager"),
    SUPPORT_AGENT("Support Agent"),
    OPS_ANALYST("Ops Analyst");

    companion object {
        fun fromRaw(raw: String): EmployeeRole = entries.firstOrNull { it.name == raw } ?: SUPPORT_AGENT
    }
}

enum class EmployeeStatus(val label: String) {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    SUSPENDED("Suspended");

    companion object {
        fun fromRaw(raw: String): EmployeeStatus = entries.firstOrNull { it.name == raw } ?: ACTIVE
    }
}

enum class TicketCategory(val label: String) {
    COMPLAINT("Complaint"),
    TICKET("Support Ticket"),
    FEEDBACK("User Feedback"),
    BUG_REPORT("Bug Report"),
    FEATURE_REQUEST("Feature Request");

    companion object {
        fun fromRaw(raw: String): TicketCategory = entries.firstOrNull { it.name == raw } ?: TICKET
    }
}

enum class TicketPriority(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    CRITICAL("Critical");

    companion object {
        fun fromRaw(raw: String): TicketPriority = entries.firstOrNull { it.name == raw } ?: MEDIUM
    }
}

enum class TicketStatus(val label: String) {
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    RESOLVED("Resolved");

    companion object {
        fun fromRaw(raw: String): TicketStatus = entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

enum class LogSeverity(val label: String) {
    INFO("Info"),
    WARNING("Warning"),
    CRITICAL("Critical");

    companion object {
        fun fromRaw(raw: String): LogSeverity = entries.firstOrNull { it.name == raw } ?: INFO
    }
}

@Keep
@IgnoreExtraProperties
data class RestaurantTenant(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userCode: String = "",
    val restaurantName: String = "",
    val ownerName: String = "",
    val mobileNumber: String = "",
    val email: String = "",
    val address: String = "",
    val plan: String = PlanTier.FREE.name,
    val billingCycle: String = BillingCycle.MONTHLY.name,
    val subscriptionStatus: String = SubscriptionStatus.ACTIVE.name,
    val accountStatus: String = AccountStatus.ACTIVE.name,
    val trialDaysRemaining: Long = 0L,
    val downloadsCount: Long = 1L,
    val activeUsersToday: Long = 1L,
    val monthlyActiveUsers: Long = 1L,
    val onlineNow: Boolean = false,
    val demoAccount: Boolean = false,
    val expiryEpochMillis: Long = 0L,
    val lastPasswordResetNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "userCode" to userCode.take(30),
        "restaurantName" to restaurantName.take(80),
        "ownerName" to ownerName.take(80),
        "mobileNumber" to mobileNumber.take(20),
        "email" to email.take(120),
        "address" to address.take(250),
        "plan" to PlanTier.fromRaw(plan).name,
        "billingCycle" to BillingCycle.fromRaw(billingCycle).name,
        "subscriptionStatus" to SubscriptionStatus.fromRaw(subscriptionStatus).name,
        "accountStatus" to AccountStatus.fromRaw(accountStatus).name,
        "trialDaysRemaining" to trialDaysRemaining.coerceIn(0L, 365L),
        "downloadsCount" to downloadsCount.coerceAtLeast(0L),
        "activeUsersToday" to activeUsersToday.coerceAtLeast(0L),
        "monthlyActiveUsers" to monthlyActiveUsers.coerceAtLeast(0L),
        "onlineNow" to onlineNow,
        "demoAccount" to demoAccount,
        "expiryEpochMillis" to expiryEpochMillis.coerceAtLeast(0L),
        "lastPasswordResetNote" to lastPasswordResetNote.take(200),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class PaymentTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val transactionId: String = "",
    val invoiceNumber: String = "",
    val restaurantId: String = "",
    val restaurantName: String = "",
    val plan: String = PlanTier.PREMIUM.name,
    val billingCycle: String = BillingCycle.MONTHLY.name,
    val baseAmount: Double = 0.0,
    val gstAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val status: String = PaymentStatus.SUCCESS.name,
    val paymentMethod: String = "UPI",
    val refundReason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "transactionId" to transactionId.take(64),
        "invoiceNumber" to invoiceNumber.take(40),
        "restaurantId" to restaurantId.take(128),
        "restaurantName" to restaurantName.take(80),
        "plan" to PlanTier.fromRaw(plan).name,
        "billingCycle" to BillingCycle.fromRaw(billingCycle).name,
        "baseAmount" to baseAmount.coerceAtLeast(0.0),
        "gstAmount" to gstAmount.coerceAtLeast(0.0),
        "totalAmount" to totalAmount.coerceAtLeast(0.0),
        "status" to PaymentStatus.fromRaw(status).name,
        "paymentMethod" to paymentMethod.take(40),
        "refundReason" to refundReason.take(200),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class AppConfigRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val appName: String = "RestroPOS",
    val currentVersion: String = "2.4.0",
    val latestVersion: String = "2.5.0",
    val minRequiredVersion: String = "2.1.0",
    val forceUpdate: Boolean = false,
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "Scheduled platform upgrade in progress.",
    val featureOnlineOrdering: Boolean = true,
    val featureQrMenu: Boolean = true,
    val featureKitchenDisplay: Boolean = true,
    val featureGstBilling: Boolean = true,
    val featureLoyalty: Boolean = true,
    val featureMultiBranch: Boolean = true,
    val privacyPolicyUrl: String = "https://restroadmin.app/privacy",
    val termsUrl: String = "https://restroadmin.app/terms",
    val androidDownloadUrl: String = "https://play.google.com/store/apps",
    val websiteUrl: String = "https://restroadmin.app",
    val announcementTitle: String = "GST Export v2 Ready",
    val announcementBody: String = "Download monthly CGST/SGST tax statements directly from Finance.",
    val announcementActive: Boolean = true,
    val totalDownloads: Long = 1480L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "appName" to appName.take(60),
        "currentVersion" to currentVersion.take(20),
        "latestVersion" to latestVersion.take(20),
        "minRequiredVersion" to minRequiredVersion.take(20),
        "forceUpdate" to forceUpdate,
        "maintenanceMode" to maintenanceMode,
        "maintenanceMessage" to maintenanceMessage.take(250),
        "featureOnlineOrdering" to featureOnlineOrdering,
        "featureQrMenu" to featureQrMenu,
        "featureKitchenDisplay" to featureKitchenDisplay,
        "featureGstBilling" to featureGstBilling,
        "featureLoyalty" to featureLoyalty,
        "featureMultiBranch" to featureMultiBranch,
        "privacyPolicyUrl" to privacyPolicyUrl.take(300),
        "termsUrl" to termsUrl.take(300),
        "androidDownloadUrl" to androidDownloadUrl.take(300),
        "websiteUrl" to websiteUrl.take(300),
        "announcementTitle" to announcementTitle.take(100),
        "announcementBody" to announcementBody.take(400),
        "announcementActive" to announcementActive,
        "totalDownloads" to totalDownloads.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class AdminNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val audience: String = NotificationAudience.ALL.name,
    val category: String = NotificationCategory.PROMOTIONAL.name,
    val targetRestaurantNames: String = "",
    val deliveryStatus: String = "DELIVERED",
    val scheduledForLabel: String = "Immediate",
    val sentCount: Long = 0L,
    val readCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "title" to title.take(100),
        "message" to message.take(500),
        "audience" to NotificationAudience.fromRaw(audience).name,
        "category" to NotificationCategory.fromRaw(category).name,
        "targetRestaurantNames" to targetRestaurantNames.take(300),
        "deliveryStatus" to deliveryStatus,
        "scheduledForLabel" to scheduledForLabel.take(60),
        "sentCount" to sentCount.coerceAtLeast(0L),
        "readCount" to readCount.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class StaffEmployee(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val mobile: String = "",
    val email: String = "",
    val role: String = EmployeeRole.SUPPORT_AGENT.name,
    val status: String = EmployeeStatus.ACTIVE.name,
    val canManageRestaurants: Boolean = true,
    val canManageBilling: Boolean = false,
    val canManageAppConfig: Boolean = false,
    val canManageSupport: Boolean = true,
    val canViewAnalytics: Boolean = true,
    val canManageSecurity: Boolean = false,
    val lastLoginLabel: String = "Today",
    val lastActionNote: String = "Onboarded",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "name" to name.take(80),
        "mobile" to mobile.take(20),
        "email" to email.take(120),
        "role" to EmployeeRole.fromRaw(role).name,
        "status" to EmployeeStatus.fromRaw(status).name,
        "canManageRestaurants" to canManageRestaurants,
        "canManageBilling" to canManageBilling,
        "canManageAppConfig" to canManageAppConfig,
        "canManageSupport" to canManageSupport,
        "canViewAnalytics" to canViewAnalytics,
        "canManageSecurity" to canManageSecurity,
        "lastLoginLabel" to lastLoginLabel.take(60),
        "lastActionNote" to lastActionNote.take(200),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class SupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val restaurantName: String = "",
    val category: String = TicketCategory.TICKET.name,
    val subject: String = "",
    val description: String = "",
    val priority: String = TicketPriority.MEDIUM.name,
    val status: String = TicketStatus.PENDING.name,
    val adminReply: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "restaurantName" to restaurantName.take(80),
        "category" to TicketCategory.fromRaw(category).name,
        "subject" to subject.take(120),
        "description" to description.take(600),
        "priority" to TicketPriority.fromRaw(priority).name,
        "status" to TicketStatus.fromRaw(status).name,
        "adminReply" to adminReply.take(600),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class MarketingCampaign(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val promoCode: String = "",
    val discountPercent: Long = 20L,
    val targetPlan: String = "PREMIUM",
    val redemptions: Long = 0L,
    val budgetInr: Double = 5000.0,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "name" to name.take(80),
        "promoCode" to promoCode.take(30),
        "discountPercent" to discountPercent.coerceIn(0L, 100L),
        "targetPlan" to targetPlan,
        "redemptions" to redemptions.coerceAtLeast(0L),
        "budgetInr" to budgetInr.coerceAtLeast(0.0),
        "active" to active,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class AdminAuditLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val actorEmail: String = "",
    val action: String = "",
    val module: String = "",
    val details: String = "",
    val deviceModel: String = "",
    val severity: String = LogSeverity.INFO.name,
    val suspicious: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "actorEmail" to actorEmail.take(120),
        "action" to action.take(100),
        "module" to module.take(50),
        "details" to details.take(400),
        "deviceModel" to deviceModel.take(100),
        "severity" to LogSeverity.fromRaw(severity).name,
        "suspicious" to suspicious,
        "createdAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class CrashReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val appVersion: String = "2.4.0",
    val deviceName: String = "",
    val errorTitle: String = "",
    val stackSummary: String = "",
    val affectedUsers: Long = 1L,
    val resolved: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "appVersion" to appVersion.take(20),
        "deviceName" to deviceName.take(80),
        "errorTitle" to errorTitle.take(120),
        "stackSummary" to stackSummary.take(400),
        "affectedUsers" to affectedUsers.coerceAtLeast(0L),
        "resolved" to resolved,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class SecuritySettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val twoFactorEnabled: Boolean = false,
    val unauthorizedProtection: Boolean = true,
    val sessionTimeoutMinutes: Long = 30L,
    val securityPinHash: String = "1234",
    val lastBackupLabel: String = "Not yet backed up",
    val lastRestoreLabel: String = "Never",
    val failedAttemptsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "twoFactorEnabled" to twoFactorEnabled,
        "unauthorizedProtection" to unauthorizedProtection,
        "sessionTimeoutMinutes" to sessionTimeoutMinutes.coerceIn(5L, 1440L),
        "securityPinHash" to securityPinHash.take(64),
        "lastBackupLabel" to lastBackupLabel.take(80),
        "lastRestoreLabel" to lastRestoreLabel.take(80),
        "failedAttemptsCount" to failedAttemptsCount.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
