package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.MarketingCampaign
import com.example.data.model.NotificationAudience
import com.example.data.model.NotificationCategory
import com.example.data.model.SupportTicket
import com.example.data.model.TicketCategory
import com.example.data.model.TicketPriority
import com.example.data.model.TicketStatus
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.formatInr
import com.example.ui.components.statusColor
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.SaffronPrimary
import com.example.viewmodel.AdminWorkspaceData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationsScreen(
    data: AdminWorkspaceData,
    onCreateTicket: (String, TicketCategory, String, String, TicketPriority) -> Unit,
    onUpdateTicket: (SupportTicket, TicketPriority, TicketStatus, String) -> Unit,
    onSendNotification: (String, String, NotificationAudience, NotificationCategory, String, String, Long) -> Unit,
    onSaveCampaign: (MarketingCampaign) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Support, 1 = Notifications, 2 = Marketing
    var ticketStatusFilter by remember { mutableStateOf("ALL") }
    var replyTicket by remember { mutableStateOf<SupportTicket?>(null) }
    var showCreateTicket by remember { mutableStateOf(false) }
    var showComposeNotif by remember { mutableStateOf(false) }
    var showCreateCampaign by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0 -> showCreateTicket = true
                        1 -> showComposeNotif = true
                        else -> showCreateCampaign = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create item")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SecondaryTabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Support (${data.tickets.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Notifications") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Campaigns") })
            }

            when (selectedTab) {
                0 -> {
                    val filteredTickets = data.tickets.filter {
                        ticketStatusFilter == "ALL" || it.status == ticketStatusFilter
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("ALL", "PENDING", "IN_PROGRESS", "RESOLVED").forEach { st ->
                                    FilterChip(
                                        selected = ticketStatusFilter == st,
                                        onClick = { ticketStatusFilter = st },
                                        label = { Text(st) }
                                    )
                                }
                            }
                        }
                        if (filteredTickets.isEmpty()) {
                            item {
                                Text(
                                    text = "No support tickets in this queue.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                        items(filteredTickets, key = { it.id }) { ticket ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(ticket.subject, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                        StatusPill(ticket.priority, statusColor(if (ticket.priority == "CRITICAL" || ticket.priority == "HIGH") "FAILED" else "WARNING"))
                                    }
                                    Text("${ticket.restaurantName} • ${TicketCategory.fromRaw(ticket.category).label}", style = MaterialTheme.typography.labelMedium, color = SaffronPrimary)
                                    Text(ticket.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (ticket.adminReply.isNotBlank()) {
                                        Text("Admin Reply: ${ticket.adminReply}", style = MaterialTheme.typography.bodyMedium, color = EmeraldAccent)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StatusPill(ticket.status, statusColor(ticket.status))
                                        OutlinedButton(onClick = { replyTicket = ticket }) {
                                            Icon(Icons.Default.Reply, contentDescription = null)
                                            Text(" Reply / Priority")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            SectionHeader(
                                title = "Notification Broadcasts & History",
                                subtitle = "Target All, Selected, Free, Premium or Enterprise",
                                actionLabel = "Compose",
                                onActionClick = { showComposeNotif = true }
                            )
                        }
                        items(data.notifications, key = { it.id }) { notif ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = SaffronPrimary)
                                            Text(notif.title, style = MaterialTheme.typography.titleMedium)
                                        }
                                        StatusPill(notif.deliveryStatus, statusColor(notif.deliveryStatus))
                                    }
                                    Text(notif.message, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "${NotificationCategory.fromRaw(notif.category).label} • Audience: ${notif.targetRestaurantNames} • Schedule: ${notif.scheduledForLabel}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Sent: ${notif.sentCount} • Opened: ${notif.readCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldAccent
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            SectionHeader(
                                title = "Marketing Campaign Management",
                                subtitle = "Promo codes, plan discounts & redemptions",
                                actionLabel = "New Campaign",
                                onActionClick = { showCreateCampaign = true }
                            )
                        }
                        items(data.campaigns, key = { it.id }) { camp ->
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
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.Campaign, contentDescription = null, tint = SaffronPrimary)
                                            Text(camp.name, style = MaterialTheme.typography.titleMedium)
                                        }
                                        Text("Code: ${camp.promoCode} (${camp.discountPercent}% OFF ${camp.targetPlan})", style = MaterialTheme.typography.labelSmall, color = EmeraldAccent)
                                        Text("Redemptions: ${camp.redemptions} • Budget: ${formatInr(camp.budgetInr)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = camp.active,
                                        onCheckedChange = { onSaveCampaign(camp.copy(active = it)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateTicket) {
        var restName by remember { mutableStateOf(data.restaurants.firstOrNull()?.restaurantName ?: "") }
        var category by remember { mutableStateOf(TicketCategory.COMPLAINT) }
        var subject by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var priority by remember { mutableStateOf(TicketPriority.HIGH) }

        AlertDialog(
            onDismissRequest = { showCreateTicket = false },
            title = { Text("New Support Ticket") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = restName, onValueChange = { restName = it }, label = { Text("Restaurant Name") }, singleLine = true)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TicketCategory.entries.forEach { cat ->
                            FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat.label) })
                        }
                    }
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, maxLines = 3)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TicketPriority.entries.forEach { p ->
                            FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p.label) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onCreateTicket(restName, category, subject, desc, priority)
                    showCreateTicket = false
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreateTicket = false }) { Text("Cancel") } }
        )
    }

    if (replyTicket != null) {
        val tkt = replyTicket!!
        var reply by remember { mutableStateOf(tkt.adminReply) }
        var priority by remember { mutableStateOf(TicketPriority.fromRaw(tkt.priority)) }
        var status by remember { mutableStateOf(TicketStatus.fromRaw(tkt.status)) }

        AlertDialog(
            onDismissRequest = { replyTicket = null },
            title = { Text("Reply & Priority • ${tkt.restaurantName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tkt.subject, style = MaterialTheme.typography.titleMedium)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TicketPriority.entries.forEach { p ->
                            FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p.label) })
                        }
                    }
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TicketStatus.entries.forEach { s ->
                            FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s.label) })
                        }
                    }
                    OutlinedTextField(value = reply, onValueChange = { reply = it }, label = { Text("Reply to Customer") }, maxLines = 3)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateTicket(tkt, priority, status, reply)
                    replyTicket = null
                }) { Text("Send Reply") }
            },
            dismissButton = { TextButton(onClick = { replyTicket = null }) { Text("Cancel") } }
        )
    }

    if (showComposeNotif) {
        var title by remember { mutableStateOf("") }
        var message by remember { mutableStateOf("") }
        var audience by remember { mutableStateOf(NotificationAudience.ALL) }
        var category by remember { mutableStateOf(NotificationCategory.PAYMENT_REMINDER) }
        var scheduleText by remember { mutableStateOf("Immediate") }
        val selectedNames = remember { mutableStateListOf<String>() }

        AlertDialog(
            onDismissRequest = { showComposeNotif = false },
            title = { Text("Compose Notification") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Message") }, maxLines = 3)
                    Text("Audience", style = MaterialTheme.typography.labelLarge)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        NotificationAudience.entries.forEach { a ->
                            FilterChip(selected = audience == a, onClick = { audience = a }, label = { Text(a.label) })
                        }
                    }
                    if (audience == NotificationAudience.SELECTED && data.restaurants.isNotEmpty()) {
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            data.restaurants.forEach { r ->
                                val picked = r.restaurantName in selectedNames
                                FilterChip(
                                    selected = picked,
                                    onClick = { if (picked) selectedNames.remove(r.restaurantName) else selectedNames.add(r.restaurantName) },
                                    label = { Text(r.restaurantName) }
                                )
                            }
                        }
                    }
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        NotificationCategory.entries.forEach { c ->
                            FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.label) })
                        }
                    }
                    OutlinedTextField(
                        value = scheduleText,
                        onValueChange = { scheduleText = it },
                        label = { Text("Schedule ('Immediate' or date/time)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val count = when (audience) {
                        NotificationAudience.ALL -> data.restaurants.size.toLong()
                        NotificationAudience.SELECTED -> selectedNames.size.toLong()
                        NotificationAudience.FREE -> data.metrics.freePlanUsers.toLong()
                        NotificationAudience.PREMIUM -> data.metrics.premiumPlanUsers.toLong()
                        NotificationAudience.ENTERPRISE -> data.metrics.enterprisePlanUsers.toLong()
                    }.coerceAtLeast(1L)
                    val targetStr = if (audience == NotificationAudience.SELECTED) selectedNames.joinToString(", ") else audience.label
                    onSendNotification(title, message, audience, category, targetStr, scheduleText, count)
                    showComposeNotif = false
                }) { Text(if (scheduleText == "Immediate") "Send Now" else "Schedule") }
            },
            dismissButton = { TextButton(onClick = { showComposeNotif = false }) { Text("Cancel") } }
        )
    }

    if (showCreateCampaign) {
        var name by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        var discount by remember { mutableStateOf("20") }
        var budget by remember { mutableStateOf("5000") }
        var targetPlan by remember { mutableStateOf("PREMIUM") }

        AlertDialog(
            onDismissRequest = { showCreateCampaign = false },
            title = { Text("Launch Marketing Campaign") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Campaign Name") }, singleLine = true)
                    OutlinedTextField(value = code, onValueChange = { code = it.uppercase() }, label = { Text("Promo Code") }, singleLine = true)
                    OutlinedTextField(value = discount, onValueChange = { discount = it.filter(Char::isDigit) }, label = { Text("Discount %") }, singleLine = true)
                    OutlinedTextField(value = budget, onValueChange = { budget = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Budget (₹)") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ALL", "FREE", "PREMIUM", "ENTERPRISE").forEach { p ->
                            FilterChip(selected = targetPlan == p, onClick = { targetPlan = p }, label = { Text(p) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSaveCampaign(
                        MarketingCampaign(
                            name = name,
                            promoCode = code,
                            discountPercent = discount.toLongOrNull() ?: 20L,
                            targetPlan = targetPlan,
                            budgetInr = budget.toDoubleOrNull() ?: 5000.0
                        )
                    )
                    showCreateCampaign = false
                }) { Text("Launch") }
            },
            dismissButton = { TextButton(onClick = { showCreateCampaign = false }) { Text("Cancel") } }
        )
    }
}
