package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SettingsApplications
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.SectionHeader
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.RoseError
import com.example.ui.theme.SaffronPrimary
import com.example.viewmodel.AdminWorkspaceData

@Composable
fun AdminMenuScreen(
    data: AdminWorkspaceData,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateRoute: (String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val m = data.metrics

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Final Admin Panel Menu",
                subtitle = "All 12 Super Admin & SaaS Governance Modules"
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.DarkMode, contentDescription = null, tint = SaffronPrimary)
                        Column {
                            Text("Executive Dark Mode", style = MaterialTheme.typography.titleMedium)
                            Text("Switch between Obsidian Dark & Crisp Light", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(checked = isDarkTheme, onCheckedChange = { onToggleTheme() })
                }
            }
        }

        item {
            MenuModuleRow(
                title = "1. Main Dashboard",
                subtitle = "${m.totalRegisteredRestaurants} Restaurants • ${m.liveOnlineUsers} Live Online",
                icon = Icons.Default.Dashboard,
                color = SaffronPrimary,
                onClick = { onNavigateRoute("dashboard") }
            )
        }
        item {
            MenuModuleRow(
                title = "2. User & Restaurant Management",
                subtitle = "Search, filter, block, activate, reset password & details",
                icon = Icons.Default.Restaurant,
                color = EmeraldAccent,
                onClick = { onNavigateRoute("restaurants") }
            )
        }
        item {
            MenuModuleRow(
                title = "3. Subscription Management",
                subtitle = "Free / ₹100 Premium / ₹200 Enterprise • Trials & renewals",
                icon = Icons.Default.Campaign,
                color = IndigoAccent,
                onClick = { onNavigateRoute("restaurants") }
            )
        }
        item {
            MenuModuleRow(
                title = "4. Payments, Invoices & GST",
                subtitle = "${m.totalPaymentsCount} Transactions • Refunds & SaaS P&L",
                icon = Icons.Default.CurrencyRupee,
                color = EmeraldAccent,
                onClick = { onNavigateRoute("finance") }
            )
        }
        item {
            MenuModuleRow(
                title = "5. App Management & Config",
                subtitle = "Version v${data.appConfig.latestVersion}, Force Update, Maintenance & Flags",
                icon = Icons.Default.SettingsApplications,
                color = CyanAccent,
                onClick = { onNavigateRoute("app_management") }
            )
        }
        item {
            MenuModuleRow(
                title = "6. Notifications Management",
                subtitle = "Broadcast & schedule alerts (${data.notifications.size} sent)",
                icon = Icons.Default.NotificationsActive,
                color = SaffronPrimary,
                onClick = { onNavigateRoute("operations") }
            )
        }
        item {
            MenuModuleRow(
                title = "7. Employee & Access Control",
                subtitle = "${data.employees.size} Staff members • Granular RBAC permissions",
                icon = Icons.Default.Badge,
                color = IndigoAccent,
                onClick = { onNavigateRoute("employees") }
            )
        }
        item {
            MenuModuleRow(
                title = "8. Reports & Analytics",
                subtitle = "11 Reports • Export real PDF & Excel CSV",
                icon = Icons.Default.Analytics,
                color = EmeraldAccent,
                onClick = { onNavigateRoute("reports") }
            )
        }
        item {
            MenuModuleRow(
                title = "9. Support & Marketing Ops",
                subtitle = "${m.pendingTicketsCount} Pending tickets • Promo campaigns",
                icon = Icons.Default.SupportAgent,
                color = SaffronPrimary,
                onClick = { onNavigateRoute("operations") }
            )
        }
        item {
            MenuModuleRow(
                title = "10. Security & Super Admin",
                subtitle = "2FA PIN, Login Device Logs, Backup/Restore & Rules",
                icon = Icons.Default.Security,
                color = RoseError,
                onClick = { onNavigateRoute("security") }
            )
        }
        item {
            MenuModuleRow(
                title = "11. Live Ops, Crashes & Demo Accounts",
                subtitle = "Server/Firebase DB meters, Suspicious alerts & P&L",
                icon = Icons.Default.Sensors,
                color = CyanAccent,
                onClick = { onNavigateRoute("command_monitor") }
            )
        }

        item {
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = onSignOut,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Text("  Sign Out of Super Admin")
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun MenuModuleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
