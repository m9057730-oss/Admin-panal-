package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BillingCycle
import com.example.data.model.PaymentStatus
import com.example.data.model.PaymentTransaction
import com.example.data.model.PlanTier
import com.example.data.model.RestaurantTenant
import com.example.ui.components.KpiMetricCard
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
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    data: AdminWorkspaceData,
    onRecordPayment: (RestaurantTenant, PlanTier, BillingCycle, String, PaymentStatus) -> Unit,
    onIssueRefund: (PaymentTransaction, String) -> Unit,
    onUpdatePaymentStatus: (PaymentTransaction, PaymentStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Payments, 1 = GST & Invoices, 2 = P&L
    var searchTx by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") }
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var invoiceTarget by remember { mutableStateOf<PaymentTransaction?>(null) }
    var refundTarget by remember { mutableStateOf<PaymentTransaction?>(null) }

    val m = data.metrics
    val filteredPayments = remember(data.payments, searchTx, statusFilter) {
        data.payments.filter { p ->
            val matchQuery = searchTx.isBlank() ||
                p.transactionId.contains(searchTx, ignoreCase = true) ||
                p.restaurantName.contains(searchTx, ignoreCase = true) ||
                p.invoiceNumber.contains(searchTx, ignoreCase = true)
            val matchStatus = statusFilter == "ALL" || p.status == statusFilter
            matchQuery && matchStatus
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddPaymentDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_payment_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Record Payment")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SecondaryTabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Payments") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("GST & Invoices") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("SaaS P&L") })
            }

            when (selectedTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    KpiMetricCard(
                                        title = "Total Payments",
                                        value = "${m.totalPaymentsCount}",
                                        caption = "Today: ${m.todaysPaymentsCount} • Mo: ${m.monthlyPaymentsCount}",
                                        icon = Icons.Default.CurrencyRupee,
                                        accentColor = EmeraldAccent,
                                        modifier = Modifier.weight(1f)
                                    )
                                    KpiMetricCard(
                                        title = "Success / Failed",
                                        value = "${m.successfulPaymentsCount} / ${m.failedPaymentsCount}",
                                        caption = "Pending: ${m.pendingPaymentsCount}",
                                        icon = Icons.Default.CheckCircle,
                                        accentColor = SaffronPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                OutlinedTextField(
                                    value = searchTx,
                                    onValueChange = { searchTx = it },
                                    placeholder = { Text("Search Transaction ID, Invoice #, Restaurant...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("ALL", "SUCCESS", "PENDING", "FAILED", "REFUNDED").forEach { st ->
                                        FilterChip(
                                            selected = statusFilter == st,
                                            onClick = { statusFilter = st },
                                            label = { Text(st) }
                                        )
                                    }
                                }
                            }
                        }

                        if (filteredPayments.isEmpty()) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "No payments match this filter. Tap + to record a transaction.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(18.dp)
                                    )
                                }
                            }
                        }

                        items(filteredPayments, key = { it.id }) { pay ->
                            PaymentCard(
                                payment = pay,
                                onViewInvoice = { invoiceTarget = pay },
                                onRefund = { refundTarget = pay },
                                onMarkPaid = { onUpdatePaymentStatus(pay, PaymentStatus.SUCCESS) }
                            )
                        }
                    }
                }

                1 -> {
                    val successful = data.payments.filter { it.status == PaymentStatus.SUCCESS.name }
                    val taxableBase = successful.sumOf { it.baseAmount }
                    val totalGst = successful.sumOf { it.gstAmount }
                    val cgst = totalGst / 2.0
                    val sgst = totalGst / 2.0

                    val premiumRev = successful.filter { it.plan == PlanTier.PREMIUM.name }.sumOf { it.totalAmount }
                    val entRev = successful.filter { it.plan == PlanTier.ENTERPRISE.name }.sumOf { it.totalAmount }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            SectionHeader(
                                title = "GST Tax & Revenue Report",
                                subtitle = "18% GST (9% CGST + 9% SGST) on SaaS subscriptions"
                            )
                        }
                        item {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FinanceLineRow("Taxable Subscription Base", formatInr(taxableBase))
                                    FinanceLineRow("CGST (9%)", formatInr(cgst))
                                    FinanceLineRow("SGST (9%)", formatInr(sgst))
                                    FinanceLineRow("Total GST Payable", formatInr(totalGst), highlight = true)
                                    Spacer(Modifier.height(6.dp))
                                    SectionHeader(title = "Subscription-wise Revenue")
                                    FinanceLineRow("Premium Plan (₹100/mo)", formatInr(premiumRev))
                                    FinanceLineRow("Enterprise Plan (₹200/mo)", formatInr(entRev))
                                }
                            }
                        }
                        item {
                            SectionHeader(title = "Tax Invoices", subtitle = "Tap any invoice to view breakdown")
                        }
                        items(data.payments, key = { "inv_${it.id}" }) { pay ->
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
                                    Column {
                                        Text(pay.invoiceNumber, style = MaterialTheme.typography.titleMedium)
                                        Text("${pay.restaurantName} • ${pay.plan}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    OutlinedButton(onClick = { invoiceTarget = pay }) {
                                        Icon(Icons.Default.ReceiptLong, contentDescription = null)
                                        Text(" View")
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            SectionHeader(
                                title = "Overall SaaS Business Profit & Loss",
                                subtitle = "Real-time unit economics & operating margin"
                            )
                        }
                        item {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    FinanceLineRow("Gross Subscription Revenue", formatInr(m.totalRevenue))
                                    FinanceLineRow("Less: 18% GST Remittance", "- ${formatInr(m.totalGstCollected)}")
                                    FinanceLineRow("Less: Customer Refunds", "- ${formatInr(m.refundedAmount)}")
                                    FinanceLineRow("Less: Payment Gateway Fees (2%)", "- ${formatInr(m.gatewayFeesInr)}")
                                    FinanceLineRow("Less: Cloud & Firebase Infra", "- ${formatInr(m.serverInfraCostInr)}")
                                    Spacer(Modifier.height(4.dp))
                                    FinanceLineRow("Net SaaS Profit", formatInr(m.netProfitInr), highlight = true)
                                    val margin = if (m.totalRevenue > 0) ((m.netProfitInr / m.totalRevenue) * 100).toInt() else 0
                                    Text(
                                        text = "Operating Margin: $margin% • Monthly Run Rate: ${formatInr(m.monthlyRevenue)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (m.netProfitInr >= 0) EmeraldAccent else RoseError
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddPaymentDialog) {
        RecordPaymentDialog(
            restaurants = data.restaurants,
            onDismiss = { showAddPaymentDialog = false },
            onSave = { rest, plan, cycle, method, status ->
                onRecordPayment(rest, plan, cycle, method, status)
                showAddPaymentDialog = false
            }
        )
    }

    if (invoiceTarget != null) {
        val inv = invoiceTarget!!
        AlertDialog(
            onDismissRequest = { invoiceTarget = null },
            title = { Text("Tax Invoice • ${inv.invoiceNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Transaction ID: ${inv.transactionId}", style = MaterialTheme.typography.labelSmall)
                    Text("Billed To: ${inv.restaurantName}")
                    Text("Plan: ${inv.plan} (${inv.billingCycle})")
                    Text("Payment Method: ${inv.paymentMethod}")
                    Text("Taxable Value: ${formatInr(inv.baseAmount)}")
                    Text("CGST (9%): ${formatInr(inv.gstAmount / 2.0)}")
                    Text("SGST (9%): ${formatInr(inv.gstAmount / 2.0)}")
                    Text("Total Invoice Amount: ${formatInr(inv.totalAmount)}", fontWeight = FontWeight.Bold)
                    Text("Status: ${inv.status}")
                }
            },
            confirmButton = { TextButton(onClick = { invoiceTarget = null }) { Text("Done") } }
        )
    }

    if (refundTarget != null) {
        var reason by remember { mutableStateOf("Customer requested cancellation") }
        AlertDialog(
            onDismissRequest = { refundTarget = null },
            title = { Text("Issue Refund • ${refundTarget!!.transactionId}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Refund ${formatInr(refundTarget!!.totalAmount)} to ${refundTarget!!.restaurantName}?")
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Refund Reason") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onIssueRefund(refundTarget!!, reason)
                        refundTarget = null
                    }
                ) { Text("Process Refund") }
            },
            dismissButton = { TextButton(onClick = { refundTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun PaymentCard(
    payment: PaymentTransaction,
    onViewInvoice: () -> Unit,
    onRefund: () -> Unit,
    onMarkPaid: () -> Unit
) {
    val dateText = payment.createdAt?.toDate()?.let {
        SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(it)
    } ?: "Just now"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(payment.restaurantName, style = MaterialTheme.typography.titleMedium)
                    Text("${payment.transactionId} • ${payment.paymentMethod}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${payment.plan} (${payment.billingCycle}) • $dateText", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(formatInr(payment.totalAmount), style = MaterialTheme.typography.titleLarge, color = EmeraldAccent, fontWeight = FontWeight.Bold)
                    StatusPill(payment.status, statusColor(payment.status))
                }
            }
            if (payment.refundReason.isNotBlank()) {
                Text("Refund Note: ${payment.refundReason}", style = MaterialTheme.typography.labelMedium, color = RoseError)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onViewInvoice) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null)
                    Text(" Invoice")
                }
                if (payment.status == PaymentStatus.SUCCESS.name) {
                    OutlinedButton(onClick = onRefund) {
                        Icon(Icons.Default.Replay, contentDescription = null)
                        Text(" Refund")
                    }
                }
                if (payment.status == PaymentStatus.PENDING.name) {
                    Button(onClick = onMarkPaid) {
                        Icon(Icons.Default.PendingActions, contentDescription = null)
                        Text(" Mark Paid")
                    }
                }
            }
        }
    }
}

@Composable
private fun FinanceLineRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (highlight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (highlight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
            color = if (highlight) EmeraldAccent else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RecordPaymentDialog(
    restaurants: List<RestaurantTenant>,
    onDismiss: () -> Unit,
    onSave: (RestaurantTenant, PlanTier, BillingCycle, String, PaymentStatus) -> Unit
) {
    var selectedRest by remember {
        mutableStateOf(
            restaurants.firstOrNull() ?: RestaurantTenant(restaurantName = "Walk-in Restaurant")
        )
    }
    var plan by remember { mutableStateOf(PlanTier.PREMIUM) }
    var cycle by remember { mutableStateOf(BillingCycle.MONTHLY) }
    var method by remember { mutableStateOf("UPI") }
    var status by remember { mutableStateOf(PaymentStatus.SUCCESS) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Subscription Payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (restaurants.isNotEmpty()) {
                    Text("Restaurant", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        restaurants.forEach { r ->
                            FilterChip(
                                selected = selectedRest.id == r.id,
                                onClick = { selectedRest = r },
                                label = { Text(r.restaurantName) }
                            )
                        }
                    }
                }
                Text("Plan", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(PlanTier.PREMIUM, PlanTier.ENTERPRISE).forEach { tier ->
                        FilterChip(
                            selected = plan == tier,
                            onClick = { plan = tier },
                            label = { Text("${tier.label} ₹${tier.monthlyPriceInr.toInt()}") }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BillingCycle.entries.forEach { c ->
                        FilterChip(selected = cycle == c, onClick = { cycle = c }, label = { Text(c.label) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("UPI", "Card", "NetBanking").forEach { m ->
                        FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(PaymentStatus.SUCCESS, PaymentStatus.PENDING, PaymentStatus.FAILED).forEach { s ->
                        FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s.label) })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(selectedRest, plan, cycle, method, status) }) { Text("Save Payment") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
