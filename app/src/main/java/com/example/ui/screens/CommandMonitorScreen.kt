package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminAuditLog
import com.example.data.model.CrashReport
import com.example.data.model.RestaurantTenant
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.UsageProgressBar
import com.example.ui.components.formatInr
import com.example.ui.components.statusColor
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.RoseError
import com.example.ui.theme.SaffronPrimary
import com.example.viewmodel.AdminWorkspaceData

@Composable
fun CommandMonitorScreen(
    data: AdminWorkspaceData,
    onToggleCrashResolved: (CrashReport) -> Unit,
    onDismissSuspicious: (AdminAuditLog) -> Unit,
    onConvertDemoToReal: (RestaurantTenant) -> Unit,
    onSeedPortfolio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val m = data.metrics
    val demoAccounts = data.restaurants.filter { it.demoAccount }
    val suspiciousLogs = data.auditLogs.filter { it.suspicious }
    val totalNotifSent = data.notifications.sumOf { it.sentCount }.coerceAtLeast(1L)
    val totalNotifRead = data.notifications.sumOf { it.readCount }
    val deliveryRate = ((totalNotifRead.toFloat() / totalNotifSent.toFloat()) * 100f).toInt().coerceIn(0, 100)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Live Command & Ops Monitor",
                subtitle = "Real-time telemetry, server/Firebase usage, crashes & demo accounts"
            )
        }

        // Live Online Users & Live Monitoring KPIs
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiMetricCard(
                    title = "Live Online Users",
                    value = "${m.liveOnlineUsers}",
                    caption = "Active POS terminals",
                    icon = Icons.Default.Sensors,
                    accentColor = EmeraldAccent,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = "Open Crash Reports",
                    value = "${m.openCrashesCount}",
                    caption = "${data.crashes.size} total captured",
                    icon = Icons.Default.BugReport,
                    accentColor = if (m.openCrashesCount > 0) RoseError else EmeraldAccent,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Server, Firebase Database & Storage Usage Monitoring
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CloudQueue, contentDescription = null, tint = CyanAccent)
                        Text("Server, Firebase DB & Storage Telemetry", style = MaterialTheme.typography.titleMedium)
                    }
                    UsageProgressBar(
                        label = "Server API Load (Latency 38ms)",
                        valueLabel = "${(22 + m.liveOnlineUsers * 4).coerceAtMost(92)}%",
                        progress = ((22 + m.liveOnlineUsers * 4) / 100f),
                        color = CyanAccent
                    )
                    UsageProgressBar(
                        label = "Firebase Database Usage (Enterprise)",
                        valueLabel = "${m.firestoreDocCount} docs / 50k quota",
                        progress = (m.firestoreDocCount / 500f).coerceIn(0.04f, 1f),
                        color = EmeraldAccent
                    )
                    UsageProgressBar(
                        label = "Cloud Storage Footprint",
                        valueLabel = "${"%.1f".format(m.estimatedDbKb)} KB / 1 GB",
                        progress = (m.estimatedDbKb / 10240.0).toFloat().coerceIn(0.03f, 1f),
                        color = IndigoAccent
                    )
                    UsageProgressBar(
                        label = "Notification Delivery Read Rate",
                        valueLabel = "$deliveryRate% ($totalNotifRead/$totalNotifSent)",
                        progress = deliveryRate / 100f,
                        color = SaffronPrimary
                    )
                }
            }
        }

        // Live Subscription & SaaS P&L Snapshot
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Live Subscription & P&L Pulse", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Active: ${m.activeSubscriptions} • Trial: ${m.trialCount} • Expiring (7d): ${m.upcomingExpiryCount} • Expired: ${m.expiredSubscriptions}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Gross: ${formatInr(m.totalRevenue)} | Infra+Fees: ${formatInr(m.gatewayFeesInr + m.serverInfraCostInr)} | Net Profit: ${formatInr(m.netProfitInr)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldAccent
                    )
                }
            }
        }

        // Suspicious Login Alerts
        item {
            SectionHeader(
                title = "Suspicious Login Alerts (${suspiciousLogs.size})",
                subtitle = "Anomaly detection & unauthorized access guard"
            )
        }
        if (suspiciousLogs.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No active suspicious login alerts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EmeraldAccent,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(suspiciousLogs, key = { "susp_${it.id}" }) { log ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = RoseError)
                            Text(log.action, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        Text("${log.details} (${log.deviceModel})", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        OutlinedButton(onClick = { onDismissSuspicious(log) }) {
                            Text("Acknowledge & Clear Alert")
                        }
                    }
                }
            }
        }

        // App Crash Reports
        item {
            SectionHeader(
                title = "App Crash Reports (${data.crashes.size})",
                subtitle = "Stack traces across restaurant POS builds"
            )
        }
        if (data.crashes.isEmpty()) {
            item {
                Text(
                    text = "Zero crash reports captured.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(data.crashes, key = { it.id }) { crash ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(crash.errorTitle, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            StatusPill(
                                if (crash.resolved) "RESOLVED" else "OPEN",
                                statusColor(if (crash.resolved) "RESOLVED" else "FAILED")
                            )
                        }
                        Text("v${crash.appVersion} • ${crash.deviceName} • ${crash.affectedUsers} users", style = MaterialTheme.typography.labelMedium, color = SaffronPrimary)
                        Text(crash.stackSummary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = { onToggleCrashResolved(crash) }) {
                            Text(if (crash.resolved) "Reopen Issue" else "Mark Resolved")
                        }
                    }
                }
            }
        }

        // Demo Account Management
        item {
            SectionHeader(
                title = "Demo Account Management",
                subtitle = "Manage sandbox tenants or seed sample SaaS data"
            )
        }
        item {
            Button(
                onClick = onSeedPortfolio,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AddBusiness, contentDescription = null)
                Text("  Seed Sample SaaS Portfolio into Firestore")
            }
        }
        items(demoAccounts, key = { "demo_${it.id}" }) { demo ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(demo.restaurantName, style = MaterialTheme.typography.titleMedium)
                        Text("${demo.userCode} • Trial ${demo.trialDaysRemaining}d left", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = { onConvertDemoToReal(demo) }) {
                        Text("Convert Live")
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
