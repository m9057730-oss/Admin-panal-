package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BillingCycle
import com.example.data.model.PlanTier
import com.example.data.model.RestaurantTenant
import com.example.data.model.SubscriptionStatus
import com.example.ui.components.PlanBadge
import com.example.ui.components.StatusPill
import com.example.ui.components.statusColor
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.RoseError
import com.example.ui.theme.SaffronPrimary
import com.example.viewmodel.AdminWorkspaceData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantsScreen(
    data: AdminWorkspaceData,
    onSaveRestaurant: (RestaurantTenant) -> Unit,
    onToggleBlock: (RestaurantTenant) -> Unit,
    onDeleteRestaurant: (RestaurantTenant) -> Unit,
    onResetPassword: (RestaurantTenant) -> Unit,
    onUpdateSubscription: (RestaurantTenant, PlanTier, BillingCycle, SubscriptionStatus, Long, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = User Management, 1 = Subscriptions
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") }
    var planFilter by remember { mutableStateOf("ALL") }
    var subFilter by remember { mutableStateOf("ALL") }

    var editingTenant by remember { mutableStateOf<RestaurantTenant?>(null) }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var viewingTenant by remember { mutableStateOf<RestaurantTenant?>(null) }
    var subTenant by remember { mutableStateOf<RestaurantTenant?>(null) }
    var deleteConfirmTenant by remember { mutableStateOf<RestaurantTenant?>(null) }

    val now = System.currentTimeMillis()
    val weekAhead = now + 7L * 86_400_000L

    val filteredRestaurants = remember(data.restaurants, searchQuery, statusFilter, planFilter, subFilter, selectedTab) {
        data.restaurants.filter { r ->
            val matchesSearch = searchQuery.isBlank() ||
                r.restaurantName.contains(searchQuery, ignoreCase = true) ||
                r.ownerName.contains(searchQuery, ignoreCase = true) ||
                r.mobileNumber.contains(searchQuery, ignoreCase = true) ||
                r.email.contains(searchQuery, ignoreCase = true) ||
                r.userCode.contains(searchQuery, ignoreCase = true)

            val matchesPlan = planFilter == "ALL" || r.plan == planFilter
            val matchesAccount = statusFilter == "ALL" || r.accountStatus == statusFilter
            val matchesSub = when (subFilter) {
                "ALL" -> true
                "UPCOMING" -> r.subscriptionStatus == "ACTIVE" && r.expiryEpochMillis in now..weekAhead
                "MONTHLY" -> r.billingCycle == "MONTHLY"
                "YEARLY" -> r.billingCycle == "YEARLY"
                else -> r.subscriptionStatus == subFilter
            }
            matchesSearch && matchesPlan && (if (selectedTab == 0) matchesAccount else matchesSub)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTenant = RestaurantTenant()
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_restaurant_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Restaurant")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SecondaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Users (${data.restaurants.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Subscriptions") }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search restaurant, owner, mobile, email, ID...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_restaurants_input")
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL" to "All Plans", "FREE" to "Free (₹0)", "PREMIUM" to "Premium ₹100", "ENTERPRISE" to "Enterprise ₹200").forEach { (key, label) ->
                        FilterChip(
                            selected = planFilter == key,
                            onClick = { planFilter = key },
                            label = { Text(label) }
                        )
                    }
                }

                if (selectedTab == 0) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ALL" to "All Status", "ACTIVE" to "Active", "BLOCKED" to "Blocked").forEach { (key, label) ->
                            FilterChip(
                                selected = statusFilter == key,
                                onClick = { statusFilter = key },
                                label = { Text(label) }
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ALL" to "All Subs",
                            "ACTIVE" to "Active",
                            "TRIAL" to "Trial",
                            "EXPIRED" to "Expired",
                            "UPCOMING" to "Upcoming Expiry",
                            "MONTHLY" to "Monthly",
                            "YEARLY" to "Yearly"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = subFilter == key,
                                onClick = { subFilter = key },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedTab == 1) {
                    item {
                        SubscriptionPricingHeader(data)
                    }
                }
                if (filteredRestaurants.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No matching restaurants found. Tap + to register a restaurant.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                }
                items(filteredRestaurants, key = { it.id }) { tenant ->
                    if (selectedTab == 0) {
                        RestaurantUserCard(
                            tenant = tenant,
                            onViewDetails = { viewingTenant = tenant },
                            onEdit = {
                                editingTenant = tenant
                                showAddEditDialog = true
                            },
                            onToggleBlock = { onToggleBlock(tenant) },
                            onDelete = { deleteConfirmTenant = tenant },
                            onResetPassword = { onResetPassword(tenant) },
                            onManageSub = { subTenant = tenant }
                        )
                    } else {
                        SubscriptionTenantCard(
                            tenant = tenant,
                            onManageSub = { subTenant = tenant },
                            onQuickRenew = {
                                val tier = PlanTier.fromRaw(tenant.plan)
                                val cycle = BillingCycle.fromRaw(tenant.billingCycle)
                                onUpdateSubscription(tenant, tier, cycle, SubscriptionStatus.ACTIVE, 0L, true)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddEditDialog && editingTenant != null) {
        RestaurantFormDialog(
            initial = editingTenant!!,
            onDismiss = { showAddEditDialog = false },
            onSave = { updated ->
                onSaveRestaurant(updated)
                showAddEditDialog = false
            }
        )
    }

    if (viewingTenant != null) {
        RestaurantDetailsDialog(
            tenant = viewingTenant!!,
            auditLogs = data.auditLogs.filter {
                it.details.contains(viewingTenant!!.restaurantName, ignoreCase = true) ||
                    it.details.contains(viewingTenant!!.userCode, ignoreCase = true)
            },
            onDismiss = { viewingTenant = null }
        )
    }

    if (subTenant != null) {
        SubscriptionEditorDialog(
            tenant = subTenant!!,
            onDismiss = { subTenant = null },
            onApply = { plan, cycle, status, trialDays, recordPay ->
                onUpdateSubscription(subTenant!!, plan, cycle, status, trialDays, recordPay)
                subTenant = null
            }
        )
    }

    if (deleteConfirmTenant != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmTenant = null },
            title = { Text("Delete Restaurant Account?") },
            text = { Text("Permanently delete ${deleteConfirmTenant!!.restaurantName} (${deleteConfirmTenant!!.userCode})?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteRestaurant(deleteConfirmTenant!!)
                        deleteConfirmTenant = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmTenant = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SubscriptionPricingHeader(data: AdminWorkspaceData) {
    val m = data.metrics
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("SaaS Subscription Plans", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Free: ₹0/mo (${m.freePlanUsers})", style = MaterialTheme.typography.bodyMedium)
                Text("Premium: ₹100/mo (${m.premiumPlanUsers})", style = MaterialTheme.typography.bodyMedium, color = SaffronPrimary)
                Text("Ent: ₹200/mo (${m.enterprisePlanUsers})", style = MaterialTheme.typography.bodyMedium, color = IndigoAccent)
            }
            Text(
                text = "Active: ${m.activeSubscriptions} • Trial: ${m.trialCount} • Expired: ${m.expiredSubscriptions} • Upcoming Expiry (7d): ${m.upcomingExpiryCount}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RestaurantUserCard(
    tenant: RestaurantTenant,
    onViewDetails: () -> Unit,
    onEdit: () -> Unit,
    onToggleBlock: () -> Unit,
    onDelete: () -> Unit,
    onResetPassword: () -> Unit,
    onManageSub: () -> Unit
) {
    val regDate = tenant.createdAt?.toDate()?.let {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it)
    } ?: "Just now"

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewDetails)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(tenant.restaurantName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${tenant.ownerName} • ${tenant.userCode}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${tenant.mobileNumber} • ${tenant.email}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (tenant.address.isNotBlank()) {
                        Text("${tenant.address} • Joined $regDate", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusPill(tenant.accountStatus, statusColor(tenant.accountStatus))
                    PlanBadge(tenant.plan)
                }
            }

            if (tenant.lastPasswordResetNote.isNotBlank()) {
                Text(
                    text = tenant.lastPasswordResetNote,
                    style = MaterialTheme.typography.labelSmall,
                    color = SaffronPrimary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit restaurant")
                    }
                    IconButton(onClick = onToggleBlock) {
                        Icon(
                            imageVector = if (tenant.accountStatus == "BLOCKED") Icons.Default.CheckCircle else Icons.Default.Block,
                            contentDescription = if (tenant.accountStatus == "BLOCKED") "Activate account" else "Block account",
                            tint = if (tenant.accountStatus == "BLOCKED") EmeraldAccent else RoseError
                        )
                    }
                    IconButton(onClick = onResetPassword) {
                        Icon(Icons.Default.Key, contentDescription = "Reset password", tint = SaffronPrimary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete account", tint = RoseError)
                    }
                }
                OutlinedButton(onClick = onManageSub) {
                    Icon(Icons.Default.Upgrade, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Plan")
                }
            }
        }
    }
}

@Composable
private fun SubscriptionTenantCard(
    tenant: RestaurantTenant,
    onManageSub: () -> Unit,
    onQuickRenew: () -> Unit
) {
    val expiryStr = if (tenant.expiryEpochMillis > 0L) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(tenant.expiryEpochMillis))
    } else "N/A"
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(tenant.restaurantName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${tenant.billingCycle} • Expires $expiryStr" +
                            if (tenant.trialDaysRemaining > 0) " • Trial ${tenant.trialDaysRemaining}d left" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PlanBadge(tenant.plan)
                    StatusPill(tenant.subscriptionStatus, statusColor(tenant.subscriptionStatus))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onManageSub, modifier = Modifier.weight(1f)) {
                    Text("Upgrade / Manage")
                }
                OutlinedButton(onClick = onQuickRenew, modifier = Modifier.weight(1f)) {
                    Text("Renew Now")
                }
            }
        }
    }
}

@Composable
private fun RestaurantFormDialog(
    initial: RestaurantTenant,
    onDismiss: () -> Unit,
    onSave: (RestaurantTenant) -> Unit
) {
    var restaurantName by remember { mutableStateOf(initial.restaurantName) }
    var ownerName by remember { mutableStateOf(initial.ownerName) }
    var mobile by remember { mutableStateOf(initial.mobileNumber) }
    var email by remember { mutableStateOf(initial.email) }
    var address by remember { mutableStateOf(initial.address) }
    var plan by remember { mutableStateOf(PlanTier.fromRaw(initial.plan)) }
    var demo by remember { mutableStateOf(initial.demoAccount) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id.isBlank()) "Register Restaurant" else "Edit Restaurant") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = restaurantName, onValueChange = { restaurantName = it }, label = { Text("Restaurant Name") }, singleLine = true)
                OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Owner Name") }, singleLine = true)
                OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile Number") }, singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, singleLine = true)
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Restaurant Address") }, maxLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PlanTier.entries.forEach { tier ->
                        FilterChip(
                            selected = plan == tier,
                            onClick = { plan = tier },
                            label = { Text(tier.label) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = demo, onCheckedChange = { demo = it })
                    Text("Mark as Demo Account", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            restaurantName = restaurantName,
                            ownerName = ownerName,
                            mobileNumber = mobile,
                            email = email,
                            address = address,
                            plan = plan.name,
                            demoAccount = demo,
                            expiryEpochMillis = if (initial.expiryEpochMillis == 0L) System.currentTimeMillis() + 30L * 86_400_000L else initial.expiryEpochMillis
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun RestaurantDetailsDialog(
    tenant: RestaurantTenant,
    auditLogs: List<com.example.data.model.AdminAuditLog>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tenant.restaurantName) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("User ID: ${tenant.userCode}", style = MaterialTheme.typography.labelSmall)
                Text("Owner: ${tenant.ownerName}")
                Text("Mobile: ${tenant.mobileNumber}")
                Text("Email: ${tenant.email}")
                Text("Address: ${tenant.address}")
                Text("Plan: ${tenant.plan} (${tenant.billingCycle})")
                Text("Subscription: ${tenant.subscriptionStatus} • Account: ${tenant.accountStatus}")
                Text("DAU: ${tenant.activeUsersToday} • MAU: ${tenant.monthlyActiveUsers} • Downloads: ${tenant.downloadsCount}")
                Spacer(Modifier.height(6.dp))
                Text("Activity History", style = MaterialTheme.typography.titleMedium)
                if (auditLogs.isEmpty()) {
                    Text("No recent events logged for this tenant.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    auditLogs.take(5).forEach { log ->
                        Text("• ${log.action}: ${log.details}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun SubscriptionEditorDialog(
    tenant: RestaurantTenant,
    onDismiss: () -> Unit,
    onApply: (PlanTier, BillingCycle, SubscriptionStatus, Long, Boolean) -> Unit
) {
    var plan by remember { mutableStateOf(PlanTier.fromRaw(tenant.plan)) }
    var cycle by remember { mutableStateOf(BillingCycle.fromRaw(tenant.billingCycle)) }
    var status by remember { mutableStateOf(SubscriptionStatus.fromRaw(tenant.subscriptionStatus)) }
    var trialDays by remember { mutableStateOf("14") }
    var recordPayment by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Subscription • ${tenant.restaurantName}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select Plan (Upgrade / Downgrade)", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PlanTier.entries.forEach { tier ->
                        FilterChip(
                            selected = plan == tier,
                            onClick = { plan = tier },
                            label = { Text("${tier.label} ₹${tier.monthlyPriceInr.toInt()}") }
                        )
                    }
                }
                Text("Billing Cycle", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BillingCycle.entries.forEach { b ->
                        FilterChip(
                            selected = cycle == b,
                            onClick = { cycle = b },
                            label = { Text(b.label) }
                        )
                    }
                }
                Text("Activation / Status", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SubscriptionStatus.entries.forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(s.label) }
                        )
                    }
                }
                if (status == SubscriptionStatus.TRIAL) {
                    OutlinedTextField(
                        value = trialDays,
                        onValueChange = { trialDays = it.filter(Char::isDigit) },
                        label = { Text("Trial Period Days") },
                        singleLine = true
                    )
                }
                if (status == SubscriptionStatus.ACTIVE && plan != PlanTier.FREE) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = recordPayment, onCheckedChange = { recordPayment = it })
                        Text("Log renewal payment + 18% GST", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(plan, cycle, status, trialDays.toLongOrNull() ?: 14L, recordPayment)
                }
            ) { Text("Apply Changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
