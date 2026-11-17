package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsApplications
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.PlanBadge
import com.example.ui.components.PlanDonutChart
import com.example.ui.components.RevenueTrendBarChart
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.formatInr
import com.example.ui.components.statusColor
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.RoseError
import com.example.ui.theme.SaffronPrimary
import com.example.viewmodel.AdminWorkspaceData

@Composable
fun DashboardScreen(
    data: AdminWorkspaceData,
    onNavigateRoute: (String) -> Unit,
    onSeedPortfolio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val m = data.metrics

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Executive Hero Command Banner
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(164.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_command_hero_1794885223388),
                        contentDescription = "SaaS Command Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPill(
                                label = "${m.liveOnlineUsers} Live Online",
                                color = EmeraldAccent
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "v${data.appConfig.latestVersion}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "${data.appConfig.appName} Super Admin",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Total Revenue ${formatInr(m.totalRevenue)} • ${m.totalRegisteredRestaurants} Restaurants • ${m.activeSubscriptions} Active Plans",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Empty-state Demo Data Seeder Banner if no restaurants yet
        if (data.restaurants.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Workspace Ready — Populate Sample SaaS Data?",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Load 5 realistic restaurants (Free, ₹100 Premium, ₹200 Enterprise), GST payments, staff roles, tickets, crash telemetry, and demo accounts into Cloud Firestore, or register restaurants manually.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onSeedPortfolio,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("seed_sample_portfolio_button")
                            ) {
                                Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Load Sample SaaS Data")
                            }
                            OutlinedButton(onClick = { onNavigateRoute("restaurants") }) {
                                Text("Add Manually")
                            }
                        }
                    }
                }
            }
        }

        // 2. Restaurant & User Activity KPIs
        item {
            SectionHeader(
                title = "Restaurant & Engagement KPIs",
                subtitle = "Real-time tenant & active user metrics",
                actionLabel = "Manage",
                onActionClick = { onNavigateRoute("restaurants") }
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiMetricCard(
                        title = "Total Registered",
                        value = "${m.totalRegisteredRestaurants}",
                        caption = "+${m.todaysNewRegistrations} today",
                        icon = Icons.Default.Restaurant,
                        accentColor = SaffronPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateRoute("restaurants") }
                    )
                    KpiMetricCard(
                        title = "Active Restaurants",
                        value = "${m.totalActiveRestaurants}",
                        caption = "Verified tenants",
                        icon = Icons.Default.Storefront,
                        accentColor = EmeraldAccent,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateRoute("restaurants") }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiMetricCard(
                        title = "Blocked Accounts",
                        value = "${m.totalBlockedRestaurants}",
                        caption = "Suspended access",
                        icon = Icons.Default.Block,
                        accentColor = RoseError,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateRoute("restaurants") }
                    )
                    KpiMetricCard(
                        title = "Total Downloads",
                        value = "${m.totalDownloads}",
                        caption = "POS & Admin installs",
                        icon = Icons.Default.CloudDownload,
                        accentColor = CyanAccent,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateRoute("app_management") }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiMetricCard(
                        title = "Daily Active (DAU)",
                        value = "${m.dailyActiveUsers}",
                        caption = "${m.liveOnlineUsers} online now",
                        icon = Icons.Default.Groups,
                        accentColor = IndigoAccent,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "Monthly Active (MAU)",
                        value = "${m.monthlyActiveUsers}",
                        caption = "Today's Reg: ${m.todaysNewRegistrations}",
                        icon = Icons.Default.PersonAdd,
                        accentColor = EmeraldAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Revenue & Subscription Metrics
        item {
            SectionHeader(
                title = "Revenue & Subscriptions",
                subtitle = "INR billing across Free, ₹100 Premium & ₹200 Enterprise",
                actionLabel = "Finance",
                onActionClick = { onNavigateRoute("finance") }
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiMetricCard(
                        title = "Total Revenue",
                        value = formatInr(m.totalRevenue),
                        caption = "GST: ${formatInr(m.totalGstCollected)}",
                        icon = Icons.Default.CurrencyRupee,
                        accentColor = EmeraldAccent,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateRoute("finance") }
                    )
                    KpiMetricCard(
                        title = "Today's Revenue",
                        value = formatInr(m.todaysRevenue),
                        caption = "${m.todaysPaymentsCount} tx today",
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = SaffronPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiMetricCard(
                        title = "Monthly Revenue",
                        value = formatInr(m.monthlyRevenue),
                        caption = "Last 30 days",
                        icon = Icons.Default.Analytics,
                        accentColor = IndigoAccent,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "Yearly Revenue",
                        value = formatInr(m.yearlyRevenue),
                        caption = "Net Profit: ${formatInr(m.netProfitInr)}",
                        icon = Icons.Default.Verified,
                        accentColor = CyanAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. Plan Distribution Donut & Revenue Breakdown Chart
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Subscription Tiers",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Active ${m.activeSubscriptions} • Expired ${m.expiredSubscriptions}",
                            style = MaterialTheme.typography.labelMedium,
                            color = SaffronPrimary
                        )
                    }
                    PlanDonutChart(
                        freeCount = m.freePlanUsers,
                        premiumCount = m.premiumPlanUsers,
                        enterpriseCount = m.enterprisePlanUsers
                    )
                    val freeRev = 0.0
                    val premRev = m.premiumPlanUsers * 100.0
                    val entRev = m.enterprisePlanUsers * 200.0
                    RevenueTrendBarChart(
                        bars = listOf(
                            "Today" to m.todaysRevenue,
                            "Month" to m.monthlyRevenue,
                            "Year" to m.yearlyRevenue,
                            "Prem MRR" to premRev,
                            "Ent MRR" to entRev,
                            "Free" to freeRev
                        ),
                        barColor = SaffronPrimary
                    )
                }
            }
        }

        // 5. Real-Time Restaurant Registrations Feed
        item {
            SectionHeader(
                title = "Real-Time Restaurant Registrations",
                subtitle = "Latest onboarded tenants",
                actionLabel = "All Users",
                onActionClick = { onNavigateRoute("restaurants") }
            )
        }
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (data.restaurants.isEmpty()) {
                    Text(
                        text = "No restaurants registered yet. Tap 'Add Manually' or 'Load Sample SaaS Data' above.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        data.restaurants.take(4).forEach { tenant ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                    .clickable { onNavigateRoute("restaurants") }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = tenant.restaurantName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${tenant.userCode} • ${tenant.ownerName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    PlanBadge(tenant.plan)
                                    StatusPill(tenant.accountStatus, statusColor(tenant.accountStatus))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Quick Module Access Grid
        item {
            SectionHeader(
                title = "Admin Command Modules",
                subtitle = "Jump directly to any control center",
                actionLabel = "Full Menu",
                onActionClick = { onNavigateRoute("menu") }
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickModuleCard(
                        title = "App Config",
                        subtitle = if (data.appConfig.maintenanceMode) "Maintenance ON" else "Live v${data.appConfig.latestVersion}",
                        icon = Icons.Default.SettingsApplications,
                        color = CyanAccent,
                        onClick = { onNavigateRoute("app_management") },
                        modifier = Modifier.weight(1f)
                    )
                    QuickModuleCard(
                        title = "Employees",
                        subtitle = "${data.employees.size} Staff",
                        icon = Icons.Default.Badge,
                        color = IndigoAccent,
                        onClick = { onNavigateRoute("employees") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickModuleCard(
                        title = "Support & Alerts",
                        subtitle = "${m.pendingTicketsCount} Pending",
                        icon = Icons.Default.ConfirmationNumber,
                        color = SaffronPrimary,
                        onClick = { onNavigateRoute("operations") },
                        modifier = Modifier.weight(1f)
                    )
                    QuickModuleCard(
                        title = "Reports & Export",
                        subtitle = "PDF & Excel CSV",
                        icon = Icons.Default.Analytics,
                        color = EmeraldAccent,
                        onClick = { onNavigateRoute("reports") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickModuleCard(
                        title = "Security & Backup",
                        subtitle = "${m.suspiciousAlertsCount} Alerts",
                        icon = Icons.Default.Security,
                        color = RoseError,
                        onClick = { onNavigateRoute("security") },
                        modifier = Modifier.weight(1f)
                    )
                    QuickModuleCard(
                        title = "Live Monitor & Ops",
                        subtitle = "${m.openCrashesCount} Crash Logs",
                        icon = Icons.Default.AdminPanelSettings,
                        color = EmeraldAccent,
                        onClick = { onNavigateRoute("command_monitor") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun QuickModuleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                    maxLines = 1
                )
            }
        }
    }
}
