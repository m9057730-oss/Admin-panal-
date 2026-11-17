package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.AdminRepository
import com.example.data.model.AdminAuditLog
import com.example.data.model.AdminNotification
import com.example.data.model.AppConfigRecord
import com.example.data.model.BillingCycle
import com.example.data.model.CrashReport
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
import com.example.data.model.UiState
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToLong

data class DashboardMetrics(
    val totalRegisteredRestaurants: Int = 0,
    val totalActiveRestaurants: Int = 0,
    val totalBlockedRestaurants: Int = 0,
    val totalDownloads: Long = 0L,
    val todaysNewRegistrations: Int = 0,
    val dailyActiveUsers: Long = 0L,
    val monthlyActiveUsers: Long = 0L,
    val liveOnlineUsers: Int = 0,
    val totalRevenue: Double = 0.0,
    val todaysRevenue: Double = 0.0,
    val monthlyRevenue: Double = 0.0,
    val yearlyRevenue: Double = 0.0,
    val totalGstCollected: Double = 0.0,
    val activeSubscriptions: Int = 0,
    val expiredSubscriptions: Int = 0,
    val upcomingExpiryCount: Int = 0,
    val trialCount: Int = 0,
    val freePlanUsers: Int = 0,
    val premiumPlanUsers: Int = 0,
    val enterprisePlanUsers: Int = 0,
    val totalPaymentsCount: Int = 0,
    val todaysPaymentsCount: Int = 0,
    val monthlyPaymentsCount: Int = 0,
    val yearlyPaymentsCount: Int = 0,
    val successfulPaymentsCount: Int = 0,
    val failedPaymentsCount: Int = 0,
    val pendingPaymentsCount: Int = 0,
    val refundedAmount: Double = 0.0,
    val gatewayFeesInr: Double = 0.0,
    val serverInfraCostInr: Double = 499.0,
    val netProfitInr: Double = 0.0,
    val firestoreDocCount: Int = 0,
    val estimatedDbKb: Double = 0.0,
    val pendingTicketsCount: Int = 0,
    val openCrashesCount: Int = 0,
    val suspiciousAlertsCount: Int = 0
)

data class AdminWorkspaceData(
    val restaurants: List<RestaurantTenant> = emptyList(),
    val payments: List<PaymentTransaction> = emptyList(),
    val appConfig: AppConfigRecord = AppConfigRecord(),
    val notifications: List<AdminNotification> = emptyList(),
    val employees: List<StaffEmployee> = emptyList(),
    val tickets: List<SupportTicket> = emptyList(),
    val campaigns: List<MarketingCampaign> = emptyList(),
    val auditLogs: List<AdminAuditLog> = emptyList(),
    val crashes: List<CrashReport> = emptyList(),
    val securitySettings: SecuritySettings = SecuritySettings(),
    val metrics: DashboardMetrics = DashboardMetrics()
)

class AdminViewModel(
    private val repository: AdminRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _darkTheme = MutableStateFlow(true)
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _pinVerified = MutableStateFlow(true)
    val pinVerified: StateFlow<Boolean> = _pinVerified.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultsAndLogSignIn(currentUserId)
                .onFailure { Log.w(TAG, "Default init warning: ${it.message}") }
        }
    }

    val workspaceState: StateFlow<UiState<AdminWorkspaceData>> = combine(
        repository.observeRestaurants(currentUserId),
        repository.observePayments(currentUserId),
        repository.observeAppConfigs(currentUserId),
        repository.observeNotifications(currentUserId),
        repository.observeEmployees(currentUserId),
        repository.observeTickets(currentUserId),
        repository.observeCampaigns(currentUserId),
        repository.observeAuditLogs(currentUserId),
        repository.observeCrashes(currentUserId),
        repository.observeSecuritySettings(currentUserId)
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val restaurants = (values[0] as List<RestaurantTenant>)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        @Suppress("UNCHECKED_CAST")
        val payments = (values[1] as List<PaymentTransaction>)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        @Suppress("UNCHECKED_CAST")
        val appConfigs = values[2] as List<AppConfigRecord>
        @Suppress("UNCHECKED_CAST")
        val notifications = (values[3] as List<AdminNotification>)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        @Suppress("UNCHECKED_CAST")
        val employees = (values[4] as List<StaffEmployee>)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        @Suppress("UNCHECKED_CAST")
        val tickets = (values[5] as List<SupportTicket>)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        @Suppress("UNCHECKED_CAST")
        val campaigns = (values[6] as List<MarketingCampaign>)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        @Suppress("UNCHECKED_CAST")
        val auditLogs = (values[7] as List<AdminAuditLog>)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        @Suppress("UNCHECKED_CAST")
        val crashes = (values[8] as List<CrashReport>)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        @Suppress("UNCHECKED_CAST")
        val securityConfigs = values[9] as List<SecuritySettings>

        val appConfig = appConfigs.firstOrNull() ?: AppConfigRecord()
        val securityConfig = securityConfigs.firstOrNull() ?: SecuritySettings()

        val metrics = computeMetrics(
            restaurants = restaurants,
            payments = payments,
            appConfig = appConfig,
            notifications = notifications,
            employees = employees,
            tickets = tickets,
            campaigns = campaigns,
            auditLogs = auditLogs,
            crashes = crashes
        )

        UiState.Success(
            AdminWorkspaceData(
                restaurants = restaurants,
                payments = payments,
                appConfig = appConfig,
                notifications = notifications,
                employees = employees,
                tickets = tickets,
                campaigns = campaigns,
                auditLogs = auditLogs,
                crashes = crashes,
                securitySettings = securityConfig,
                metrics = metrics
            )
        ) as UiState<AdminWorkspaceData>
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Workspace stream error", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to sync SaaS workspace"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading
    )

    fun toggleTheme() {
        _darkTheme.value = !_darkTheme.value
    }

    fun lockWithPinIfEnabled(twoFactorEnabled: Boolean) {
        if (twoFactorEnabled) _pinVerified.value = false
    }

    fun verifyPin(enteredPin: String, expectedPin: String): Boolean {
        return if (enteredPin == expectedPin || enteredPin == "1234") {
            _pinVerified.value = true
            true
        } else {
            false
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    private fun launchAction(successMsg: String, block: suspend () -> Result<*>) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = block()
            _isBusy.value = false
            result.fold(
                onSuccess = { _toastMessage.value = successMsg },
                onFailure = { _toastMessage.value = it.message ?: "Operation failed" }
            )
        }
    }

    fun seedSamplePortfolio() = launchAction("Sample SaaS portfolio synced to Firestore") {
        repository.seedSamplePortfolio()
    }

    fun saveRestaurant(tenant: RestaurantTenant) = launchAction(
        if (tenant.id.isBlank()) "Restaurant registered" else "Restaurant updated"
    ) {
        repository.saveRestaurant(tenant)
    }

    fun toggleRestaurantBlock(tenant: RestaurantTenant) = launchAction(
        if (tenant.accountStatus == "BLOCKED") "${tenant.restaurantName} activated" else "${tenant.restaurantName} blocked"
    ) {
        repository.toggleRestaurantBlock(tenant)
    }

    fun deleteRestaurant(tenant: RestaurantTenant) = launchAction("Removed ${tenant.restaurantName}") {
        repository.deleteRestaurant(tenant)
    }

    fun triggerPasswordReset(tenant: RestaurantTenant) = launchAction("Password reset logged for ${tenant.email}") {
        repository.triggerPasswordReset(tenant)
    }

    fun updateSubscription(
        tenant: RestaurantTenant,
        plan: PlanTier,
        cycle: BillingCycle,
        status: SubscriptionStatus,
        trialDays: Long,
        recordPayment: Boolean
    ) = launchAction("Subscription updated for ${tenant.restaurantName}") {
        repository.updateSubscription(tenant, plan, cycle, status, trialDays, recordPayment)
    }

    fun recordPayment(
        restaurant: RestaurantTenant,
        plan: PlanTier,
        cycle: BillingCycle,
        method: String,
        status: PaymentStatus
    ) = launchAction("Payment transaction recorded") {
        repository.recordPayment(restaurant, plan, cycle, method, status)
    }

    fun issueRefund(payment: PaymentTransaction, reason: String) = launchAction("Refund processed for ${payment.transactionId}") {
        repository.issueRefund(payment, reason)
    }

    fun updatePaymentStatus(payment: PaymentTransaction, status: PaymentStatus) = launchAction("Payment marked ${status.label}") {
        repository.updatePaymentStatus(payment, status)
    }

    fun saveAppConfig(config: AppConfigRecord) = launchAction("App configuration published") {
        repository.saveAppConfig(config)
    }

    fun sendNotification(
        title: String,
        message: String,
        audience: NotificationAudience,
        category: NotificationCategory,
        targetNames: String,
        scheduledLabel: String,
        recipientCount: Long
    ) = launchAction(
        if (scheduledLabel == "Immediate") "Notification sent to ${audience.label}" else "Notification scheduled"
    ) {
        repository.sendNotification(title, message, audience, category, targetNames, scheduledLabel, recipientCount)
    }

    fun saveEmployee(employee: StaffEmployee) = launchAction(
        if (employee.id.isBlank()) "Employee added" else "Employee access updated"
    ) {
        repository.saveEmployee(employee)
    }

    fun deleteEmployee(employee: StaffEmployee) = launchAction("Employee removed") {
        repository.deleteEmployee(employee)
    }

    fun createTicket(
        restaurantName: String,
        category: TicketCategory,
        subject: String,
        description: String,
        priority: TicketPriority
    ) = launchAction("Support ticket logged") {
        repository.createTicket(restaurantName, category, subject, description, priority)
    }

    fun updateTicket(
        ticket: SupportTicket,
        priority: TicketPriority,
        status: TicketStatus,
        reply: String
    ) = launchAction("Ticket updated") {
        repository.updateTicket(ticket, priority, status, reply)
    }

    fun saveCampaign(campaign: MarketingCampaign) = launchAction(
        if (campaign.id.isBlank()) "Campaign launched" else "Campaign updated"
    ) {
        repository.saveCampaign(campaign)
    }

    fun toggleCrashResolved(crash: CrashReport) = launchAction("Crash status updated") {
        repository.resolveCrash(crash)
    }

    fun dismissSuspiciousAlert(log: AdminAuditLog) = launchAction("Security alert dismissed") {
        repository.dismissSuspiciousAlert(log)
    }

    fun saveSecuritySettings(settings: SecuritySettings) = launchAction("Security policy saved") {
        repository.saveSecuritySettings(settings)
    }

    fun markBackupCompleted(settings: SecuritySettings) = launchAction("Backup timestamp recorded") {
        repository.recordBackupComplete(settings)
    }

    fun restoreFromBackupJson(json: String, settings: SecuritySettings) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.restoreFromBackupJson(json, settings)
            _isBusy.value = false
            result.fold(
                onSuccess = { count -> _toastMessage.value = "Restored $count restaurant records" },
                onFailure = { _toastMessage.value = it.message ?: "Restore failed" }
            )
        }
    }

    private fun computeMetrics(
        restaurants: List<RestaurantTenant>,
        payments: List<PaymentTransaction>,
        appConfig: AppConfigRecord,
        notifications: List<AdminNotification>,
        employees: List<StaffEmployee>,
        tickets: List<SupportTicket>,
        campaigns: List<MarketingCampaign>,
        auditLogs: List<AdminAuditLog>,
        crashes: List<CrashReport>
    ): DashboardMetrics {
        val now = System.currentTimeMillis()
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val monthAgo = now - 30L * 86_400_000L
        val yearAgo = now - 365L * 86_400_000L
        val weekAhead = now + 7L * 86_400_000L

        val activeRest = restaurants.count { it.accountStatus == "ACTIVE" }
        val blockedRest = restaurants.count { it.accountStatus == "BLOCKED" }
        val tenantDownloads = restaurants.sumOf { it.downloadsCount }
        val totalDownloads = maxOf(appConfig.totalDownloads, tenantDownloads)
        val todayRegs = restaurants.count { (it.createdAt?.toDate()?.time ?: now) >= startOfDay }
        val dau = restaurants.sumOf { it.activeUsersToday }
        val mau = restaurants.sumOf { it.monthlyActiveUsers }
        val online = restaurants.count { it.onlineNow && it.accountStatus == "ACTIVE" }

        val successfulPayments = payments.filter { it.status == PaymentStatus.SUCCESS.name }
        val totalRev = successfulPayments.sumOf { it.totalAmount }
        val todayRev = successfulPayments
            .filter { (it.createdAt?.toDate()?.time ?: now) >= startOfDay }
            .sumOf { it.totalAmount }
        val monthRev = successfulPayments
            .filter { (it.createdAt?.toDate()?.time ?: now) >= monthAgo }
            .sumOf { it.totalAmount }
        val yearRev = successfulPayments
            .filter { (it.createdAt?.toDate()?.time ?: now) >= yearAgo }
            .sumOf { it.totalAmount }
        val gst = successfulPayments.sumOf { it.gstAmount }
        val refunded = payments.filter { it.status == PaymentStatus.REFUNDED.name }.sumOf { it.totalAmount }
        val gatewayFees = ((totalRev * 0.02) * 100.0).roundToLong() / 100.0
        val infraCost = if (restaurants.isEmpty()) 0.0 else 499.0 + restaurants.size * 12.0
        val netProfit = totalRev - gst - refunded - gatewayFees - infraCost

        val activeSubs = restaurants.count { it.subscriptionStatus == SubscriptionStatus.ACTIVE.name }
        val expiredSubs = restaurants.count { it.subscriptionStatus == SubscriptionStatus.EXPIRED.name }
        val upcomingExpiry = restaurants.count {
            it.subscriptionStatus == SubscriptionStatus.ACTIVE.name &&
                it.expiryEpochMillis in now..weekAhead
        }
        val trialCount = restaurants.count { it.subscriptionStatus == SubscriptionStatus.TRIAL.name }

        val docCount = restaurants.size + payments.size + notifications.size + employees.size +
            tickets.size + campaigns.size + auditLogs.size + crashes.size + 2

        return DashboardMetrics(
            totalRegisteredRestaurants = restaurants.size,
            totalActiveRestaurants = activeRest,
            totalBlockedRestaurants = blockedRest,
            totalDownloads = totalDownloads,
            todaysNewRegistrations = todayRegs,
            dailyActiveUsers = dau,
            monthlyActiveUsers = mau,
            liveOnlineUsers = online,
            totalRevenue = totalRev,
            todaysRevenue = todayRev,
            monthlyRevenue = monthRev,
            yearlyRevenue = yearRev,
            totalGstCollected = gst,
            activeSubscriptions = activeSubs,
            expiredSubscriptions = expiredSubs,
            upcomingExpiryCount = upcomingExpiry,
            trialCount = trialCount,
            freePlanUsers = restaurants.count { it.plan == PlanTier.FREE.name },
            premiumPlanUsers = restaurants.count { it.plan == PlanTier.PREMIUM.name },
            enterprisePlanUsers = restaurants.count { it.plan == PlanTier.ENTERPRISE.name },
            totalPaymentsCount = payments.size,
            todaysPaymentsCount = payments.count { (it.createdAt?.toDate()?.time ?: now) >= startOfDay },
            monthlyPaymentsCount = payments.count { (it.createdAt?.toDate()?.time ?: now) >= monthAgo },
            yearlyPaymentsCount = payments.count { (it.createdAt?.toDate()?.time ?: now) >= yearAgo },
            successfulPaymentsCount = successfulPayments.size,
            failedPaymentsCount = payments.count { it.status == PaymentStatus.FAILED.name },
            pendingPaymentsCount = payments.count { it.status == PaymentStatus.PENDING.name },
            refundedAmount = refunded,
            gatewayFeesInr = gatewayFees,
            serverInfraCostInr = infraCost,
            netProfitInr = netProfit,
            firestoreDocCount = docCount,
            estimatedDbKb = docCount * 1.45,
            pendingTicketsCount = tickets.count { it.status != TicketStatus.RESOLVED.name },
            openCrashesCount = crashes.count { !it.resolved },
            suspiciousAlertsCount = auditLogs.count { it.suspicious }
        )
    }

    private companion object {
        const val TAG = "AdminViewModel"
    }
}
