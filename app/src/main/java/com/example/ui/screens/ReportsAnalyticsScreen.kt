package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.RevenueTrendBarChart
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatInr
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.SaffronPrimary
import com.example.utils.ReportExporter
import com.example.viewmodel.AdminWorkspaceData

private enum class ReportType(val label: String) {
    DAILY_REG("Daily Registration"),
    WEEKLY_REG("Weekly Registration"),
    MONTHLY_REG("Monthly Registration"),
    YEARLY_REG("Yearly Registration"),
    REVENUE("Revenue Report"),
    SUBSCRIPTION("Subscription Report"),
    ACTIVE_USER("Active User Report"),
    DOWNLOAD("Download Report"),
    GROWTH("Restaurant Growth"),
    PAYMENT("Payment Report"),
    EMPLOYEE("Employee Report")
}

@Composable
fun ReportsAnalyticsScreen(
    data: AdminWorkspaceData,
    onToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedReport by remember { mutableStateOf(ReportType.REVENUE) }
    val m = data.metrics

    val reportLines = remember(selectedReport, data) {
        buildReportLines(selectedReport, data)
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            ReportExporter.writePdfReport(context, uri, selectedReport.label, reportLines)
                .fold(
                    onSuccess = { onToast("Exported ${selectedReport.label} PDF") },
                    onFailure = { onToast(it.message ?: "PDF export failed") }
                )
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            val (headers, rows) = buildCsvTable(selectedReport, data)
            ReportExporter.writeCsvReport(context, uri, headers, rows)
                .fold(
                    onSuccess = { onToast("Exported ${selectedReport.label} Excel CSV") },
                    onFailure = { onToast(it.message ?: "CSV export failed") }
                )
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Reports & Analytics",
                subtitle = "11 executive reports with PDF & Excel CSV export"
            )
        }

        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportType.entries.forEach { r ->
                    FilterChip(
                        selected = selectedReport == r,
                        onClick = { selectedReport = r },
                        label = { Text(r.label) }
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(selectedReport.label, style = MaterialTheme.typography.titleLarge)
                    val bars = when (selectedReport) {
                        ReportType.REVENUE, ReportType.PAYMENT -> listOf(
                            "Today" to m.todaysRevenue,
                            "Month" to m.monthlyRevenue,
                            "Year" to m.yearlyRevenue,
                            "GST" to m.totalGstCollected,
                            "Net" to m.netProfitInr.coerceAtLeast(0.0)
                        )
                        ReportType.SUBSCRIPTION -> listOf(
                            "Free" to m.freePlanUsers.toDouble(),
                            "Prem" to m.premiumPlanUsers.toDouble(),
                            "Ent" to m.enterprisePlanUsers.toDouble(),
                            "Active" to m.activeSubscriptions.toDouble(),
                            "Expired" to m.expiredSubscriptions.toDouble()
                        )
                        ReportType.ACTIVE_USER, ReportType.DOWNLOAD -> listOf(
                            "Online" to m.liveOnlineUsers.toDouble(),
                            "DAU" to m.dailyActiveUsers.toDouble(),
                            "MAU" to m.monthlyActiveUsers.toDouble(),
                            "Downloads" to m.totalDownloads.toDouble()
                        )
                        ReportType.EMPLOYEE -> listOf(
                            "Total" to data.employees.size.toDouble(),
                            "Active" to data.employees.count { it.status == "ACTIVE" }.toDouble(),
                            "Billing" to data.employees.count { it.canManageBilling }.toDouble(),
                            "Support" to data.employees.count { it.canManageSupport }.toDouble()
                        )
                        else -> listOf(
                            "Today" to m.todaysNewRegistrations.toDouble(),
                            "Active" to m.totalActiveRestaurants.toDouble(),
                            "Trial" to m.trialCount.toDouble(),
                            "Blocked" to m.totalBlockedRestaurants.toDouble(),
                            "Total" to m.totalRegisteredRestaurants.toDouble()
                        )
                    }
                    val color = when (selectedReport) {
                        ReportType.REVENUE, ReportType.PAYMENT -> EmeraldAccent
                        ReportType.SUBSCRIPTION -> SaffronPrimary
                        ReportType.EMPLOYEE -> IndigoAccent
                        else -> CyanAccent
                    }
                    RevenueTrendBarChart(bars = bars, barColor = color)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { pdfLauncher.launch("restro_${selectedReport.name.lowercase()}.pdf") },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("export_pdf_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Export PDF")
                }
                OutlinedButton(
                    onClick = { csvLauncher.launch("restro_${selectedReport.name.lowercase()}.csv") },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("export_excel_button")
                ) {
                    Icon(Icons.Default.TableView, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Export Excel")
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Report Preview", style = MaterialTheme.typography.titleMedium)
                    reportLines.forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun buildReportLines(type: ReportType, data: AdminWorkspaceData): List<String> {
    val m = data.metrics
    return when (type) {
        ReportType.DAILY_REG, ReportType.WEEKLY_REG, ReportType.MONTHLY_REG, ReportType.YEARLY_REG, ReportType.GROWTH -> {
            listOf(
                "Report: ${type.label}",
                "Total Registered Restaurants: ${m.totalRegisteredRestaurants}",
                "Today's Registrations: ${m.todaysNewRegistrations}",
                "Active: ${m.totalActiveRestaurants} | Blocked: ${m.totalBlockedRestaurants}",
                "--- Tenants ---"
            ) + data.restaurants.map { "${it.userCode} | ${it.restaurantName} | ${it.plan} | ${it.accountStatus}" }
        }
        ReportType.REVENUE, ReportType.PAYMENT -> {
            listOf(
                "Report: ${type.label}",
                "Total Revenue: ${formatInr(m.totalRevenue)} | Today: ${formatInr(m.todaysRevenue)}",
                "Monthly: ${formatInr(m.monthlyRevenue)} | Yearly: ${formatInr(m.yearlyRevenue)}",
                "GST Collected: ${formatInr(m.totalGstCollected)} | Net Profit: ${formatInr(m.netProfitInr)}",
                "--- Transactions ---"
            ) + data.payments.map { "${it.transactionId} | ${it.restaurantName} | ${formatInr(it.totalAmount)} | ${it.status}" }
        }
        ReportType.SUBSCRIPTION -> {
            listOf(
                "Active Subscriptions: ${m.activeSubscriptions} | Expired: ${m.expiredSubscriptions}",
                "Free: ${m.freePlanUsers} | Premium (₹100): ${m.premiumPlanUsers} | Enterprise (₹200): ${m.enterprisePlanUsers}",
                "Upcoming Expiries (7d): ${m.upcomingExpiryCount} | Trials: ${m.trialCount}"
            ) + data.restaurants.map { "${it.restaurantName} | ${it.plan} (${it.billingCycle}) | ${it.subscriptionStatus}" }
        }
        ReportType.ACTIVE_USER, ReportType.DOWNLOAD -> {
            listOf(
                "Live Online Users: ${m.liveOnlineUsers}",
                "Daily Active Users (DAU): ${m.dailyActiveUsers}",
                "Monthly Active Users (MAU): ${m.monthlyActiveUsers}",
                "Total App Downloads: ${m.totalDownloads}"
            ) + data.restaurants.map { "${it.restaurantName} | DAU:${it.activeUsersToday} MAU:${it.monthlyActiveUsers} DL:${it.downloadsCount}" }
        }
        ReportType.EMPLOYEE -> {
            listOf("Total Staff Employees: ${data.employees.size}") +
                data.employees.map { "${it.name} | ${it.role} | ${it.status} | ${it.email}" }
        }
    }
}

private fun buildCsvTable(type: ReportType, data: AdminWorkspaceData): Pair<List<String>, List<List<String>>> {
    return when (type) {
        ReportType.REVENUE, ReportType.PAYMENT -> {
            listOf("TransactionID", "Invoice", "Restaurant", "Plan", "Base", "GST", "Total", "Status") to
                data.payments.map {
                    listOf(it.transactionId, it.invoiceNumber, it.restaurantName, it.plan, "${it.baseAmount}", "${it.gstAmount}", "${it.totalAmount}", it.status)
                }
        }
        ReportType.EMPLOYEE -> {
            listOf("Name", "Role", "Mobile", "Email", "Status", "LastLogin") to
                data.employees.map { listOf(it.name, it.role, it.mobile, it.email, it.status, it.lastLoginLabel) }
        }
        else -> {
            listOf("UserCode", "Restaurant", "Owner", "Mobile", "Email", "Plan", "Cycle", "SubStatus", "AccountStatus") to
                data.restaurants.map {
                    listOf(it.userCode, it.restaurantName, it.ownerName, it.mobileNumber, it.email, it.plan, it.billingCycle, it.subscriptionStatus, it.accountStatus)
                }
        }
    }
}
