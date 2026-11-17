package com.example.data.firebase

import android.content.Context
import android.os.Build
import com.example.R
import com.example.data.model.AdminAuditLog
import com.example.data.model.AdminNotification
import com.example.data.model.AppConfigRecord
import com.example.data.model.BillingCycle
import com.example.data.model.CrashReport
import com.example.data.model.LogSeverity
import com.example.data.model.MarketingCampaign
import com.example.data.model.NotificationAudience
import com.example.data.model.NotificationCategory
import com.example.data.model.PaymentStatus
import com.example.data.model.PaymentTransaction
import com.example.data.model.PlanTier
import com.example.data.model.RestaurantTenant
import com.example.data.model.SecuritySettings
import com.example.data.model.StaffEmployee
import com.example.data.model.SubscriptionStatus
import com.example.data.model.SupportTicket
import com.example.data.model.TicketCategory
import com.example.data.model.TicketPriority
import com.example.data.model.TicketStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToLong

class AdminRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Admin must be signed in with Google before accessing Firestore.")
    }

    private fun currentActorEmail(): String = auth.currentUser?.email ?: "superadmin@restroadmin.app"

    private fun currentDeviceLabel(): String =
        "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    private inline fun <reified T> observeCollection(
        uid: String,
        collectionName: String
    ): Flow<List<T>> = flow {
        require(uid.isNotBlank()) { "Authenticated userId is required." }
        val path = "users/$uid/$collectionName"
        userDoc(uid).collection(collectionName)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(T::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    fun observeRestaurants(uid: String): Flow<List<RestaurantTenant>> =
        observeCollection(uid, "restaurants")

    fun observePayments(uid: String): Flow<List<PaymentTransaction>> =
        observeCollection(uid, "payments")

    fun observeAppConfigs(uid: String): Flow<List<AppConfigRecord>> =
        observeCollection(uid, "appConfig")

    fun observeNotifications(uid: String): Flow<List<AdminNotification>> =
        observeCollection(uid, "notifications")

    fun observeEmployees(uid: String): Flow<List<StaffEmployee>> =
        observeCollection(uid, "employees")

    fun observeTickets(uid: String): Flow<List<SupportTicket>> =
        observeCollection(uid, "tickets")

    fun observeCampaigns(uid: String): Flow<List<MarketingCampaign>> =
        observeCollection(uid, "campaigns")

    fun observeAuditLogs(uid: String): Flow<List<AdminAuditLog>> =
        observeCollection(uid, "auditLogs")

    fun observeCrashes(uid: String): Flow<List<CrashReport>> =
        observeCollection(uid, "crashes")

    fun observeSecuritySettings(uid: String): Flow<List<SecuritySettings>> =
        observeCollection(uid, "securityConfig")

    private suspend fun writeAuditLogInternal(
        uid: String,
        action: String,
        module: String,
        details: String,
        severity: LogSeverity = LogSeverity.INFO,
        suspicious: Boolean = false
    ) {
        val log = AdminAuditLog(
            actorEmail = currentActorEmail(),
            action = action.take(100),
            module = module.take(50),
            details = details.take(400),
            deviceModel = currentDeviceLabel(),
            severity = severity.name,
            suspicious = suspicious
        )
        userDoc(uid).collection("auditLogs").document().set(log.toCreateMap(uid)).await()
    }

    suspend fun ensureDefaultsAndLogSignIn(uid: String): Result<Unit> = runSafe(
        OperationType.WRITE,
        "users/$uid/appConfig/main"
    ) {
        require(auth.currentUser?.uid == uid) { "Auth session mismatch." }
        val appConfigRef = userDoc(uid).collection("appConfig").document("main")
        val secConfigRef = userDoc(uid).collection("securityConfig").document("main")

        if (!appConfigRef.get().await().exists()) {
            appConfigRef.set(AppConfigRecord().toCreateMap(uid)).await()
        }
        if (!secConfigRef.get().await().exists()) {
            secConfigRef.set(SecuritySettings().toCreateMap(uid)).await()
        }
        writeAuditLogInternal(
            uid = uid,
            action = "Super Admin Login",
            module = "SECURITY",
            details = "Authenticated via Google Credential Manager",
            severity = LogSeverity.INFO,
            suspicious = false
        )
    }

    suspend fun saveRestaurant(tenant: RestaurantTenant): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = if (tenant.id.isBlank()) {
            userDoc(uid).collection("restaurants").document()
        } else {
            userDoc(uid).collection("restaurants").document(tenant.id)
        }
        return runSafe(OperationType.WRITE, docRef.path) {
            requireUserId()
            require(tenant.restaurantName.trim().length in 2..80) { "Restaurant name must be 2–80 characters." }
            require(tenant.ownerName.trim().length in 2..80) { "Owner name must be 2–80 characters." }
            require(tenant.mobileNumber.trim().length in 6..20) { "Valid mobile number required." }
            require(tenant.email.trim().length in 3..120) { "Valid email address required." }

            if (tenant.id.isBlank()) {
                val code = tenant.userCode.ifBlank { "RST-${(1000..9999).random()}" }
                val clean = tenant.copy(
                    userCode = code,
                    restaurantName = tenant.restaurantName.trim(),
                    ownerName = tenant.ownerName.trim(),
                    mobileNumber = tenant.mobileNumber.trim(),
                    email = tenant.email.trim(),
                    address = tenant.address.trim()
                )
                docRef.set(clean.toCreateMap(uid)).await()
                writeAuditLogInternal(
                    uid,
                    "Registered Restaurant",
                    "RESTAURANTS",
                    "Added ${clean.restaurantName} (${clean.plan})",
                    LogSeverity.INFO
                )
            } else {
                docRef.update(
                    mapOf(
                        "restaurantName" to tenant.restaurantName.trim().take(80),
                        "ownerName" to tenant.ownerName.trim().take(80),
                        "mobileNumber" to tenant.mobileNumber.trim().take(20),
                        "email" to tenant.email.trim().take(120),
                        "address" to tenant.address.trim().take(250),
                        "plan" to PlanTier.fromRaw(tenant.plan).name,
                        "billingCycle" to BillingCycle.fromRaw(tenant.billingCycle).name,
                        "subscriptionStatus" to SubscriptionStatus.fromRaw(tenant.subscriptionStatus).name,
                        "accountStatus" to tenant.accountStatus,
                        "demoAccount" to tenant.demoAccount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                writeAuditLogInternal(
                    uid,
                    "Updated Restaurant Details",
                    "RESTAURANTS",
                    "Edited ${tenant.restaurantName}",
                    LogSeverity.INFO
                )
            }
        }
    }

    suspend fun toggleRestaurantBlock(tenant: RestaurantTenant): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/restaurants/${tenant.id}"
        return runSafe(OperationType.UPDATE, path) {
            requireUserId()
            require(tenant.id.isNotBlank()) { "Invalid restaurant ID." }
            val nextStatus = if (tenant.accountStatus == "BLOCKED") "ACTIVE" else "BLOCKED"
            userDoc(uid).collection("restaurants").document(tenant.id).update(
                mapOf(
                    "accountStatus" to nextStatus,
                    "onlineNow" to if (nextStatus == "BLOCKED") false else tenant.onlineNow,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLogInternal(
                uid,
                if (nextStatus == "BLOCKED") "Blocked Restaurant" else "Activated Restaurant",
                "RESTAURANTS",
                "${tenant.restaurantName} (${tenant.userCode}) -> $nextStatus",
                if (nextStatus == "BLOCKED") LogSeverity.WARNING else LogSeverity.INFO
            )
        }
    }

    suspend fun deleteRestaurant(tenant: RestaurantTenant): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/restaurants/${tenant.id}"
        return runSafe(OperationType.DELETE, path) {
            requireUserId()
            require(tenant.id.isNotBlank()) { "Invalid restaurant ID." }
            userDoc(uid).collection("restaurants").document(tenant.id).delete().await()
            writeAuditLogInternal(
                uid,
                "Deleted Restaurant Account",
                "RESTAURANTS",
                "Removed ${tenant.restaurantName} (${tenant.email})",
                LogSeverity.CRITICAL
            )
        }
    }

    suspend fun triggerPasswordReset(tenant: RestaurantTenant): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/restaurants/${tenant.id}"
        return runSafe(OperationType.UPDATE, path) {
            requireUserId()
            require(tenant.id.isNotBlank()) { "Invalid restaurant ID." }
            val stamp = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date())
            val note = "Reset link queued for ${tenant.email} at $stamp"
            userDoc(uid).collection("restaurants").document(tenant.id).update(
                mapOf(
                    "lastPasswordResetNote" to note.take(200),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLogInternal(
                uid,
                "Password Reset Requested",
                "RESTAURANTS",
                note,
                LogSeverity.WARNING
            )
        }
    }

    suspend fun updateSubscription(
        tenant: RestaurantTenant,
        newPlan: PlanTier,
        newCycle: BillingCycle,
        newSubStatus: SubscriptionStatus,
        trialDays: Long,
        recordRenewalPayment: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/restaurants/${tenant.id}"
        return runSafe(OperationType.UPDATE, path) {
            requireUserId()
            require(tenant.id.isNotBlank()) { "Invalid restaurant ID." }
            val daysToAdd = when {
                newSubStatus == SubscriptionStatus.TRIAL -> trialDays.coerceIn(1L, 90L)
                newCycle == BillingCycle.YEARLY -> 365L
                else -> 30L
            }
            val newExpiry = if (newSubStatus == SubscriptionStatus.EXPIRED || newSubStatus == SubscriptionStatus.CANCELLED) {
                System.currentTimeMillis() - 86_400_000L
            } else {
                System.currentTimeMillis() + daysToAdd * 86_400_000L
            }

            userDoc(uid).collection("restaurants").document(tenant.id).update(
                mapOf(
                    "plan" to newPlan.name,
                    "billingCycle" to newCycle.name,
                    "subscriptionStatus" to newSubStatus.name,
                    "trialDaysRemaining" to if (newSubStatus == SubscriptionStatus.TRIAL) trialDays.coerceIn(0L, 365L) else 0L,
                    "expiryEpochMillis" to newExpiry,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (recordRenewalPayment && newPlan != PlanTier.FREE && newSubStatus == SubscriptionStatus.ACTIVE) {
                val base = if (newCycle == BillingCycle.YEARLY) newPlan.yearlyPriceInr else newPlan.monthlyPriceInr
                val gst = (base * 0.18 * 100.0).roundToLong() / 100.0
                val total = base + gst
                val tx = PaymentTransaction(
                    transactionId = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}",
                    invoiceNumber = "INV-${(10000..99999).random()}",
                    restaurantId = tenant.id,
                    restaurantName = tenant.restaurantName,
                    plan = newPlan.name,
                    billingCycle = newCycle.name,
                    baseAmount = base,
                    gstAmount = gst,
                    totalAmount = total,
                    status = PaymentStatus.SUCCESS.name,
                    paymentMethod = "UPI"
                )
                userDoc(uid).collection("payments").document().set(tx.toCreateMap(uid)).await()
            }

            writeAuditLogInternal(
                uid,
                "Subscription Updated",
                "SUBSCRIPTIONS",
                "${tenant.restaurantName}: ${newPlan.label} (${newCycle.label}) -> ${newSubStatus.label}",
                LogSeverity.INFO
            )
        }
    }

    suspend fun recordPayment(
        restaurant: RestaurantTenant,
        plan: PlanTier,
        cycle: BillingCycle,
        method: String,
        status: PaymentStatus
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = userDoc(uid).collection("payments").document()
        return runSafe(OperationType.CREATE, docRef.path) {
            requireUserId()
            val base = if (cycle == BillingCycle.YEARLY) plan.yearlyPriceInr else plan.monthlyPriceInr
            val gst = (base * 0.18 * 100.0).roundToLong() / 100.0
            val tx = PaymentTransaction(
                transactionId = "TXN-${UUID.randomUUID().toString().take(8).uppercase()}",
                invoiceNumber = "INV-${(10000..99999).random()}",
                restaurantId = restaurant.id.ifBlank { "manual" },
                restaurantName = restaurant.restaurantName,
                plan = plan.name,
                billingCycle = cycle.name,
                baseAmount = base,
                gstAmount = gst,
                totalAmount = base + gst,
                status = status.name,
                paymentMethod = method.take(40)
            )
            docRef.set(tx.toCreateMap(uid)).await()
            writeAuditLogInternal(
                uid,
                "Recorded Payment",
                "PAYMENTS",
                "${tx.transactionId} • ₹${tx.totalAmount} • ${tx.status}",
                LogSeverity.INFO
            )
        }
    }

    suspend fun issueRefund(payment: PaymentTransaction, reason: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/payments/${payment.id}"
        return runSafe(OperationType.UPDATE, path) {
            requireUserId()
            require(payment.id.isNotBlank()) { "Invalid payment ID." }
            userDoc(uid).collection("payments").document(payment.id).update(
                mapOf(
                    "status" to PaymentStatus.REFUNDED.name,
                    "refundReason" to reason.ifBlank { "Admin approved refund" }.take(200),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLogInternal(
                uid,
                "Issued Refund",
                "PAYMENTS",
                "Refunded ${payment.transactionId} (₹${payment.totalAmount}) to ${payment.restaurantName}",
                LogSeverity.WARNING
            )
        }
    }

    suspend fun updatePaymentStatus(payment: PaymentTransaction, newStatus: PaymentStatus): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/payments/${payment.id}"
        return runSafe(OperationType.UPDATE, path) {
            requireUserId()
            userDoc(uid).collection("payments").document(payment.id).update(
                mapOf(
                    "status" to newStatus.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun saveAppConfig(config: AppConfigRecord): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = userDoc(uid).collection("appConfig").document("main")
        return runSafe(OperationType.WRITE, docRef.path) {
            requireUserId()
            require(config.appName.trim().length in 2..60) { "App Name must be 2–60 characters." }
            if (docRef.get().await().exists()) {
                docRef.update(
                    mapOf(
                        "appName" to config.appName.trim().take(60),
                        "currentVersion" to config.currentVersion.trim().take(20),
                        "latestVersion" to config.latestVersion.trim().take(20),
                        "minRequiredVersion" to config.minRequiredVersion.trim().take(20),
                        "forceUpdate" to config.forceUpdate,
                        "maintenanceMode" to config.maintenanceMode,
                        "maintenanceMessage" to config.maintenanceMessage.trim().take(250),
                        "featureOnlineOrdering" to config.featureOnlineOrdering,
                        "featureQrMenu" to config.featureQrMenu,
                        "featureKitchenDisplay" to config.featureKitchenDisplay,
                        "featureGstBilling" to config.featureGstBilling,
                        "featureLoyalty" to config.featureLoyalty,
                        "featureMultiBranch" to config.featureMultiBranch,
                        "privacyPolicyUrl" to config.privacyPolicyUrl.trim().take(300),
                        "termsUrl" to config.termsUrl.trim().take(300),
                        "androidDownloadUrl" to config.androidDownloadUrl.trim().take(300),
                        "websiteUrl" to config.websiteUrl.trim().take(300),
                        "announcementTitle" to config.announcementTitle.trim().take(100),
                        "announcementBody" to config.announcementBody.trim().take(400),
                        "announcementActive" to config.announcementActive,
                        "totalDownloads" to config.totalDownloads.coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(config.toCreateMap(uid)).await()
            }
            writeAuditLogInternal(
                uid,
                "Updated App Config",
                "APP_MANAGEMENT",
                "v${config.latestVersion} • Maintenance=${config.maintenanceMode} • ForceUpdate=${config.forceUpdate}",
                if (config.maintenanceMode) LogSeverity.WARNING else LogSeverity.INFO
            )
        }
    }

    suspend fun sendNotification(
        title: String,
        message: String,
        audience: NotificationAudience,
        category: NotificationCategory,
        targetNames: String,
        scheduledLabel: String,
        recipientCount: Long
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = userDoc(uid).collection("notifications").document()
        return runSafe(OperationType.CREATE, docRef.path) {
            requireUserId()
            require(title.trim().length in 2..100) { "Title must be 2–100 characters." }
            require(message.trim().length in 2..500) { "Message must be 2–500 characters." }
            val isScheduled = scheduledLabel.isNotBlank() && scheduledLabel != "Immediate"
            val notif = AdminNotification(
                title = title.trim(),
                message = message.trim(),
                audience = audience.name,
                category = category.name,
                targetRestaurantNames = targetNames.ifBlank { audience.label }.take(300),
                deliveryStatus = if (isScheduled) "SCHEDULED" else "DELIVERED",
                scheduledForLabel = scheduledLabel.ifBlank { "Immediate" }.take(60),
                sentCount = if (isScheduled) 0L else recipientCount.coerceAtLeast(1L),
                readCount = if (isScheduled) 0L else (recipientCount * 0.72).roundToLong().coerceAtLeast(0L)
            )
            docRef.set(notif.toCreateMap(uid)).await()
            writeAuditLogInternal(
                uid,
                if (isScheduled) "Scheduled Notification" else "Broadcast Notification",
                "NOTIFICATIONS",
                "${notif.title} -> ${notif.audience}",
                LogSeverity.INFO
            )
        }
    }

    suspend fun saveEmployee(employee: StaffEmployee): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = if (employee.id.isBlank()) {
            userDoc(uid).collection("employees").document()
        } else {
            userDoc(uid).collection("employees").document(employee.id)
        }
        return runSafe(OperationType.WRITE, docRef.path) {
            requireUserId()
            require(employee.name.trim().length in 2..80) { "Employee name must be 2–80 characters." }
            require(employee.mobile.trim().length in 6..20) { "Valid mobile required." }
            require(employee.email.trim().length in 3..120) { "Valid email required." }

            if (employee.id.isBlank()) {
                docRef.set(employee.toCreateMap(uid)).await()
                writeAuditLogInternal(uid, "Added Employee", "EMPLOYEES", "${employee.name} (${employee.role})")
            } else {
                docRef.update(
                    mapOf(
                        "name" to employee.name.trim().take(80),
                        "mobile" to employee.mobile.trim().take(20),
                        "email" to employee.email.trim().take(120),
                        "role" to employee.role,
                        "status" to employee.status,
                        "canManageRestaurants" to employee.canManageRestaurants,
                        "canManageBilling" to employee.canManageBilling,
                        "canManageAppConfig" to employee.canManageAppConfig,
                        "canManageSupport" to employee.canManageSupport,
                        "canViewAnalytics" to employee.canViewAnalytics,
                        "canManageSecurity" to employee.canManageSecurity,
                        "lastLoginLabel" to employee.lastLoginLabel.take(60),
                        "lastActionNote" to employee.lastActionNote.take(200),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                writeAuditLogInternal(uid, "Updated Employee Access", "EMPLOYEES", "${employee.name} (${employee.status})")
            }
        }
    }

    suspend fun deleteEmployee(employee: StaffEmployee): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return runSafe(OperationType.DELETE, "users/$uid/employees/${employee.id}") {
            requireUserId()
            userDoc(uid).collection("employees").document(employee.id).delete().await()
            writeAuditLogInternal(uid, "Removed Employee", "EMPLOYEES", employee.name, LogSeverity.WARNING)
        }
    }

    suspend fun createTicket(
        restaurantName: String,
        category: TicketCategory,
        subject: String,
        description: String,
        priority: TicketPriority
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = userDoc(uid).collection("tickets").document()
        return runSafe(OperationType.CREATE, docRef.path) {
            requireUserId()
            require(restaurantName.trim().length in 2..80) { "Restaurant name required." }
            require(subject.trim().length in 2..120) { "Subject required." }
            val ticket = SupportTicket(
                restaurantName = restaurantName.trim(),
                category = category.name,
                subject = subject.trim(),
                description = description.trim().ifBlank { subject.trim() },
                priority = priority.name,
                status = TicketStatus.PENDING.name
            )
            docRef.set(ticket.toCreateMap(uid)).await()
            writeAuditLogInternal(uid, "Logged Support Ticket", "SUPPORT", "$restaurantName: $subject")
        }
    }

    suspend fun updateTicket(
        ticket: SupportTicket,
        priority: TicketPriority,
        status: TicketStatus,
        reply: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return runSafe(OperationType.UPDATE, "users/$uid/tickets/${ticket.id}") {
            requireUserId()
            userDoc(uid).collection("tickets").document(ticket.id).update(
                mapOf(
                    "priority" to priority.name,
                    "status" to status.name,
                    "adminReply" to reply.trim().take(600),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLogInternal(
                uid,
                "Updated Support Ticket",
                "SUPPORT",
                "${ticket.subject} -> ${status.label} (${priority.label})"
            )
        }
    }

    suspend fun saveCampaign(campaign: MarketingCampaign): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = if (campaign.id.isBlank()) {
            userDoc(uid).collection("campaigns").document()
        } else {
            userDoc(uid).collection("campaigns").document(campaign.id)
        }
        return runSafe(OperationType.WRITE, docRef.path) {
            requireUserId()
            require(campaign.name.trim().length in 2..80) { "Campaign name required." }
            require(campaign.promoCode.trim().length in 2..30) { "Promo code required." }
            if (campaign.id.isBlank()) {
                docRef.set(campaign.toCreateMap(uid)).await()
                writeAuditLogInternal(uid, "Launched Campaign", "MARKETING", "${campaign.name} (${campaign.promoCode})")
            } else {
                docRef.update(
                    mapOf(
                        "name" to campaign.name.trim().take(80),
                        "promoCode" to campaign.promoCode.trim().uppercase().take(30),
                        "discountPercent" to campaign.discountPercent.coerceIn(0L, 100L),
                        "targetPlan" to campaign.targetPlan,
                        "redemptions" to campaign.redemptions.coerceAtLeast(0L),
                        "budgetInr" to campaign.budgetInr.coerceAtLeast(0.0),
                        "active" to campaign.active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
    }

    suspend fun resolveCrash(crash: CrashReport): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return runSafe(OperationType.UPDATE, "users/$uid/crashes/${crash.id}") {
            requireUserId()
            userDoc(uid).collection("crashes").document(crash.id).update(
                mapOf(
                    "resolved" to !crash.resolved,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun dismissSuspiciousAlert(log: AdminAuditLog): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return runSafe(OperationType.UPDATE, "users/$uid/auditLogs/${log.id}") {
            requireUserId()
            userDoc(uid).collection("auditLogs").document(log.id).update(
                mapOf("suspicious" to false)
            ).await()
        }
    }

    suspend fun saveSecuritySettings(settings: SecuritySettings): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val docRef = userDoc(uid).collection("securityConfig").document("main")
        return runSafe(OperationType.WRITE, docRef.path) {
            requireUserId()
            if (docRef.get().await().exists()) {
                docRef.update(
                    mapOf(
                        "twoFactorEnabled" to settings.twoFactorEnabled,
                        "unauthorizedProtection" to settings.unauthorizedProtection,
                        "sessionTimeoutMinutes" to settings.sessionTimeoutMinutes.coerceIn(5L, 1440L),
                        "securityPinHash" to settings.securityPinHash.take(64),
                        "lastBackupLabel" to settings.lastBackupLabel.take(80),
                        "lastRestoreLabel" to settings.lastRestoreLabel.take(80),
                        "failedAttemptsCount" to settings.failedAttemptsCount.coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(settings.toCreateMap(uid)).await()
            }
            writeAuditLogInternal(
                uid,
                "Updated Security Policy",
                "SECURITY",
                "2FA=${settings.twoFactorEnabled}, Timeout=${settings.sessionTimeoutMinutes}m",
                LogSeverity.WARNING
            )
        }
    }

    suspend fun recordBackupComplete(currentSettings: SecuritySettings): Result<Unit> {
        val stamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        return saveSecuritySettings(currentSettings.copy(lastBackupLabel = stamp))
    }

    suspend fun restoreFromBackupJson(jsonText: String, currentSettings: SecuritySettings): Result<Int> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return runSafe(OperationType.WRITE, "users/$uid/restaurants") {
            requireUserId()
            val root = JSONObject(jsonText)
            val arr = root.optJSONArray("restaurants") ?: JSONArray()
            var restored = 0
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val tenant = RestaurantTenant(
                    userCode = obj.optString("userCode", "RST-${(1000..9999).random()}"),
                    restaurantName = obj.optString("restaurantName", "Restored Restaurant"),
                    ownerName = obj.optString("ownerName", "Owner"),
                    mobileNumber = obj.optString("mobileNumber", "9999999999"),
                    email = obj.optString("email", "restored@restaurant.in"),
                    address = obj.optString("address", ""),
                    plan = obj.optString("plan", PlanTier.FREE.name),
                    billingCycle = obj.optString("billingCycle", BillingCycle.MONTHLY.name),
                    subscriptionStatus = obj.optString("subscriptionStatus", SubscriptionStatus.ACTIVE.name),
                    accountStatus = obj.optString("accountStatus", "ACTIVE"),
                    demoAccount = obj.optBoolean("demoAccount", false),
                    expiryEpochMillis = obj.optLong("expiryEpochMillis", System.currentTimeMillis() + 86_400_000L * 30)
                )
                userDoc(uid).collection("restaurants").document().set(tenant.toCreateMap(uid)).await()
                restored++
            }
            val stamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
            saveSecuritySettings(currentSettings.copy(lastRestoreLabel = "$stamp ($restored records)"))
            writeAuditLogInternal(
                uid,
                "Database Restored",
                "SECURITY",
                "Restored $restored restaurant records from JSON backup",
                LogSeverity.WARNING
            )
            restored
        }
    }

    suspend fun seedSamplePortfolio(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return runSafe(OperationType.WRITE, "users/$uid/seed") {
            requireUserId()
            val now = System.currentTimeMillis()
            val day = 86_400_000L
            val batch = db.batch()

            val sampleRestaurants = listOf(
                RestaurantTenant(
                    userCode = "RST-1001",
                    restaurantName = "Saffron Courtyard",
                    ownerName = "Aarav Mehta",
                    mobileNumber = "+91 98201 44510",
                    email = "aarav@saffroncourtyard.in",
                    address = "Bandra West, Mumbai",
                    plan = PlanTier.ENTERPRISE.name,
                    billingCycle = BillingCycle.YEARLY.name,
                    subscriptionStatus = SubscriptionStatus.ACTIVE.name,
                    accountStatus = "ACTIVE",
                    downloadsCount = 42,
                    activeUsersToday = 19,
                    monthlyActiveUsers = 68,
                    onlineNow = true,
                    demoAccount = false,
                    expiryEpochMillis = now + 240 * day
                ),
                RestaurantTenant(
                    userCode = "RST-1002",
                    restaurantName = "Dakshin Spice House",
                    ownerName = "Lakshmi Iyer",
                    mobileNumber = "+91 98450 77821",
                    email = "lakshmi@dakshin.in",
                    address = "Indiranagar, Bengaluru",
                    plan = PlanTier.PREMIUM.name,
                    billingCycle = BillingCycle.MONTHLY.name,
                    subscriptionStatus = SubscriptionStatus.ACTIVE.name,
                    accountStatus = "ACTIVE",
                    downloadsCount = 18,
                    activeUsersToday = 11,
                    monthlyActiveUsers = 34,
                    onlineNow = true,
                    demoAccount = false,
                    expiryEpochMillis = now + 5 * day
                ),
                RestaurantTenant(
                    userCode = "RST-1003",
                    restaurantName = "Royal Tandoor Haveli",
                    ownerName = "Vikramjit Gill",
                    mobileNumber = "+91 98140 22901",
                    email = "vikram@royaltandoor.in",
                    address = "Sector 17, Chandigarh",
                    plan = PlanTier.PREMIUM.name,
                    billingCycle = BillingCycle.MONTHLY.name,
                    subscriptionStatus = SubscriptionStatus.TRIAL.name,
                    accountStatus = "ACTIVE",
                    trialDaysRemaining = 9,
                    downloadsCount = 9,
                    activeUsersToday = 5,
                    monthlyActiveUsers = 15,
                    onlineNow = true,
                    demoAccount = true,
                    expiryEpochMillis = now + 9 * day
                ),
                RestaurantTenant(
                    userCode = "RST-1004",
                    restaurantName = "Coastal Curry Kitchen",
                    ownerName = "Rohan Deshmukh",
                    mobileNumber = "+91 97652 33109",
                    email = "rohan@coastalcurry.in",
                    address = "Koregaon Park, Pune",
                    plan = PlanTier.FREE.name,
                    billingCycle = BillingCycle.MONTHLY.name,
                    subscriptionStatus = SubscriptionStatus.ACTIVE.name,
                    accountStatus = "ACTIVE",
                    downloadsCount = 6,
                    activeUsersToday = 3,
                    monthlyActiveUsers = 12,
                    onlineNow = false,
                    demoAccount = false,
                    expiryEpochMillis = now + 30 * day
                ),
                RestaurantTenant(
                    userCode = "RST-1005",
                    restaurantName = "Bombay Bistro Lab",
                    ownerName = "Zoya Khan",
                    mobileNumber = "+91 99102 66432",
                    email = "zoya@bombaybistro.in",
                    address = "Hauz Khas, New Delhi",
                    plan = PlanTier.ENTERPRISE.name,
                    billingCycle = BillingCycle.MONTHLY.name,
                    subscriptionStatus = SubscriptionStatus.EXPIRED.name,
                    accountStatus = "BLOCKED",
                    downloadsCount = 27,
                    activeUsersToday = 0,
                    monthlyActiveUsers = 19,
                    onlineNow = false,
                    demoAccount = false,
                    expiryEpochMillis = now - 3 * day
                )
            )

            val restIds = sampleRestaurants.map { tenant ->
                val ref = userDoc(uid).collection("restaurants").document()
                batch.set(ref, tenant.toCreateMap(uid))
                ref.id to tenant
            }

            val samplePayments = listOf(
                PaymentTransaction(
                    transactionId = "TXN-8841209A",
                    invoiceNumber = "INV-2026-101",
                    restaurantId = restIds[0].first,
                    restaurantName = restIds[0].second.restaurantName,
                    plan = PlanTier.ENTERPRISE.name,
                    billingCycle = BillingCycle.YEARLY.name,
                    baseAmount = 2000.0,
                    gstAmount = 360.0,
                    totalAmount = 2360.0,
                    status = PaymentStatus.SUCCESS.name,
                    paymentMethod = "NetBanking"
                ),
                PaymentTransaction(
                    transactionId = "TXN-8841210B",
                    invoiceNumber = "INV-2026-102",
                    restaurantId = restIds[1].first,
                    restaurantName = restIds[1].second.restaurantName,
                    plan = PlanTier.PREMIUM.name,
                    billingCycle = BillingCycle.MONTHLY.name,
                    baseAmount = 100.0,
                    gstAmount = 18.0,
                    totalAmount = 118.0,
                    status = PaymentStatus.SUCCESS.name,
                    paymentMethod = "UPI"
                ),
                PaymentTransaction(
                    transactionId = "TXN-8841211C",
                    invoiceNumber = "INV-2026-103",
                    restaurantId = restIds[4].first,
                    restaurantName = restIds[4].second.restaurantName,
                    plan = PlanTier.ENTERPRISE.name,
                    billingCycle = BillingCycle.MONTHLY.name,
                    baseAmount = 200.0,
                    gstAmount = 36.0,
                    totalAmount = 236.0,
                    status = PaymentStatus.FAILED.name,
                    paymentMethod = "Card"
                ),
                PaymentTransaction(
                    transactionId = "TXN-8841212D",
                    invoiceNumber = "INV-2026-104",
                    restaurantId = restIds[2].first,
                    restaurantName = restIds[2].second.restaurantName,
                    plan = PlanTier.PREMIUM.name,
                    billingCycle = BillingCycle.MONTHLY.name,
                    baseAmount = 100.0,
                    gstAmount = 18.0,
                    totalAmount = 118.0,
                    status = PaymentStatus.PENDING.name,
                    paymentMethod = "UPI"
                )
            )
            samplePayments.forEach { pay ->
                batch.set(userDoc(uid).collection("payments").document(), pay.toCreateMap(uid))
            }

            val employees = listOf(
                StaffEmployee(
                    name = "Priya Nair",
                    mobile = "+91 98112 90451",
                    email = "priya@restroadmin.app",
                    role = "BILLING_MANAGER",
                    status = "ACTIVE",
                    canManageRestaurants = true,
                    canManageBilling = true,
                    canManageAppConfig = false,
                    canManageSupport = false,
                    canViewAnalytics = true,
                    canManageSecurity = false,
                    lastLoginLabel = "12 mins ago",
                    lastActionNote = "Verified GST batch"
                ),
                StaffEmployee(
                    name = "Kabir Malhotra",
                    mobile = "+91 98711 55230",
                    email = "kabir@restroadmin.app",
                    role = "SUPPORT_AGENT",
                    status = "ACTIVE",
                    canManageRestaurants = true,
                    canManageBilling = false,
                    canManageAppConfig = false,
                    canManageSupport = true,
                    canViewAnalytics = false,
                    canManageSecurity = false,
                    lastLoginLabel = "1 hr ago",
                    lastActionNote = "Replied to KOT ticket"
                )
            )
            employees.forEach { emp ->
                batch.set(userDoc(uid).collection("employees").document(), emp.toCreateMap(uid))
            }

            val tickets = listOf(
                SupportTicket(
                    restaurantName = "Dakshin Spice House",
                    category = TicketCategory.BUG_REPORT.name,
                    subject = "Thermal KOT Printer Auto-Reconnect",
                    description = "Bluetooth printer drops connection when tablet sleeps for >10 minutes.",
                    priority = TicketPriority.HIGH.name,
                    status = TicketStatus.PENDING.name
                ),
                SupportTicket(
                    restaurantName = "Saffron Courtyard",
                    category = TicketCategory.FEATURE_REQUEST.name,
                    subject = "Multi-outlet consolidated GST sheet",
                    description = "Need combined HSN summary across 4 branches in single CSV.",
                    priority = TicketPriority.MEDIUM.name,
                    status = TicketStatus.RESOLVED.name,
                    adminReply = "Enabled Multi-Branch GST consolidation in v2.4.0."
                )
            )
            tickets.forEach { tkt ->
                batch.set(userDoc(uid).collection("tickets").document(), tkt.toCreateMap(uid))
            }

            val campaigns = listOf(
                MarketingCampaign(
                    name = "Annual Upgrade 20% Off",
                    promoCode = "YEARLY20",
                    discountPercent = 20,
                    targetPlan = "ENTERPRISE",
                    redemptions = 14,
                    budgetInr = 8000.0,
                    active = true
                )
            )
            campaigns.forEach { camp ->
                batch.set(userDoc(uid).collection("campaigns").document(), camp.toCreateMap(uid))
            }

            val crashes = listOf(
                CrashReport(
                    appVersion = "2.4.0",
                    deviceName = "Samsung Galaxy Tab A9",
                    errorTitle = "NullPointerException in KotPrintQueue",
                    stackSummary = "com.restro.pos.print.KotPrintQueue.flush(KotPrintQueue.kt:89)",
                    affectedUsers = 3,
                    resolved = false
                )
            )
            crashes.forEach { crash ->
                batch.set(userDoc(uid).collection("crashes").document(), crash.toCreateMap(uid))
            }

            val notif = AdminNotification(
                title = "Premium Renewal Reminder",
                message = "Renew before expiry to keep GST & Kitchen Display uninterrupted.",
                audience = NotificationAudience.PREMIUM.name,
                category = NotificationCategory.EXPIRY_REMINDER.name,
                targetRestaurantNames = "Premium Plan Tenants",
                deliveryStatus = "DELIVERED",
                scheduledForLabel = "Immediate",
                sentCount = 12,
                readCount = 10
            )
            batch.set(userDoc(uid).collection("notifications").document(), notif.toCreateMap(uid))

            val suspiciousLog = AdminAuditLog(
                actorEmail = "unknown-proxy@external.net",
                action = "Blocked Login Attempt",
                module = "SECURITY",
                details = "Unrecognized IP location attempted admin console access",
                deviceModel = "Linux x86_64 Headless",
                severity = LogSeverity.CRITICAL.name,
                suspicious = true
            )
            batch.set(userDoc(uid).collection("auditLogs").document(), suspiciousLog.toCreateMap(uid))

            batch.commit().await()
            writeAuditLogInternal(
                uid,
                "Loaded SaaS Demo Portfolio",
                "DEMO_ACCOUNTS",
                "Populated 5 restaurants, payments, staff, tickets, campaigns, and telemetry"
            )
        }
    }

    private suspend fun <T> runSafe(
        op: OperationType,
        path: String,
        block: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, op, path)
            Result.failure(e)
        }
    }
}
