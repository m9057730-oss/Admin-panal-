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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.EmployeeRole
import com.example.data.model.EmployeeStatus
import com.example.data.model.StaffEmployee
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.statusColor
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.RoseError
import com.example.viewmodel.AdminWorkspaceData

@Composable
fun EmployeesScreen(
    data: AdminWorkspaceData,
    onSaveEmployee: (StaffEmployee) -> Unit,
    onDeleteEmployee: (StaffEmployee) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingEmp by remember { mutableStateOf<StaffEmployee?>(null) }
    var showForm by remember { mutableStateOf(false) }

    val activeStaff = data.employees.count { it.status == EmployeeStatus.ACTIVE.name }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingEmp = StaffEmployee()
                    showForm = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Employee")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeader(
                    title = "Employee & RBAC Management",
                    subtitle = "Staff logins, access permissions & activity history"
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiMetricCard(
                        title = "Total Employees",
                        value = "${data.employees.size}",
                        caption = "$activeStaff Active staff",
                        icon = Icons.Default.Badge,
                        accentColor = IndigoAccent,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        title = "Active Logins",
                        value = "$activeStaff",
                        caption = "RBAC enforced",
                        icon = Icons.Default.Badge,
                        accentColor = EmeraldAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (data.employees.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No employees added yet. Tap + to onboard admin staff and assign permissions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            }
            items(data.employees, key = { it.id }) { emp ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(emp.name, style = MaterialTheme.typography.titleMedium)
                                Text("${EmployeeRole.fromRaw(emp.role).label} • ${emp.email}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Mobile: ${emp.mobile} • Last login: ${emp.lastLoginLabel}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            StatusPill(emp.status, statusColor(emp.status))
                        }
                        val perms = buildList {
                            if (emp.canManageRestaurants) add("Restaurants")
                            if (emp.canManageBilling) add("Billing")
                            if (emp.canManageAppConfig) add("AppConfig")
                            if (emp.canManageSupport) add("Support")
                            if (emp.canViewAnalytics) add("Analytics")
                            if (emp.canManageSecurity) add("Security")
                        }
                        Text(
                            text = "Access: ${if (perms.isEmpty()) "Read-Only" else perms.joinToString(", ")}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Recent Activity: ${emp.lastActionNote}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = {
                                editingEmp = emp
                                showForm = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit employee")
                            }
                            IconButton(onClick = { onDeleteEmployee(emp) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove employee", tint = RoseError)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForm && editingEmp != null) {
        EmployeeFormDialog(
            initial = editingEmp!!,
            onDismiss = { showForm = false },
            onSave = {
                onSaveEmployee(it)
                showForm = false
            }
        )
    }
}

@Composable
private fun EmployeeFormDialog(
    initial: StaffEmployee,
    onDismiss: () -> Unit,
    onSave: (StaffEmployee) -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var mobile by remember { mutableStateOf(initial.mobile) }
    var email by remember { mutableStateOf(initial.email) }
    var role by remember { mutableStateOf(EmployeeRole.fromRaw(initial.role)) }
    var status by remember { mutableStateOf(EmployeeStatus.fromRaw(initial.status)) }
    var pRest by remember { mutableStateOf(initial.canManageRestaurants) }
    var pBill by remember { mutableStateOf(initial.canManageBilling) }
    var pApp by remember { mutableStateOf(initial.canManageAppConfig) }
    var pSup by remember { mutableStateOf(initial.canManageSupport) }
    var pAna by remember { mutableStateOf(initial.canViewAnalytics) }
    var pSec by remember { mutableStateOf(initial.canManageSecurity) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id.isBlank()) "Add Employee" else "Edit Employee & Access") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Employee Name") }, singleLine = true)
                OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile Number") }, singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, singleLine = true)
                Text("Role", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EmployeeRole.entries.forEach { r ->
                        FilterChip(selected = role == r, onClick = { role = r }, label = { Text(r.label) })
                    }
                }
                Text("Employee Status", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EmployeeStatus.entries.forEach { s ->
                        FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s.label) })
                    }
                }
                Text("Access Control Permissions", style = MaterialTheme.typography.labelLarge)
                PermCheck("Manage Restaurants", pRest) { pRest = it }
                PermCheck("Manage Billing & Refunds", pBill) { pBill = it }
                PermCheck("Manage App Configuration", pApp) { pApp = it }
                PermCheck("Manage Support Tickets", pSup) { pSup = it }
                PermCheck("View Analytics & Reports", pAna) { pAna = it }
                PermCheck("Security & Super Admin", pSec) { pSec = it }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    initial.copy(
                        name = name,
                        mobile = mobile,
                        email = email,
                        role = role.name,
                        status = status.name,
                        canManageRestaurants = pRest,
                        canManageBilling = pBill,
                        canManageAppConfig = pApp,
                        canManageSupport = pSup,
                        canViewAnalytics = pAna,
                        canManageSecurity = pSec
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PermCheck(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
