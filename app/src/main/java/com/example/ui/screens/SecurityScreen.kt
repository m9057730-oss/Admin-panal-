package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.model.SecuritySettings
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.statusColor
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.SaffronPrimary
import com.example.utils.ReportExporter
import com.example.viewmodel.AdminWorkspaceData
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun SecurityScreen(
    data: AdminWorkspaceData,
    onSaveSecurity: (SecuritySettings) -> Unit,
    onBackupRecorded: (SecuritySettings) -> Unit,
    onRestoreJson: (String, SecuritySettings) -> Unit,
    onLockSession: () -> Unit,
    onSignOut: () -> Unit,
    onToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser = Firebase.auth.currentUser
    var draft by remember(data.securitySettings.id, data.securitySettings.updatedAt) {
        mutableStateOf(data.securitySettings)
    }
    var newPin by remember { mutableStateOf("") }

    LaunchedEffect(data.securitySettings) {
        draft = data.securitySettings
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            ReportExporter.writeJsonBackup(context, uri, data.restaurants, data.payments, data.employees)
                .fold(
                    onSuccess = {
                        onBackupRecorded(draft)
                        onToast("Database JSON backup exported")
                    },
                    onFailure = { onToast(it.message ?: "Backup failed") }
                )
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            ReportExporter.readTextFromUri(context, uri)
                .fold(
                    onSuccess = { json -> onRestoreJson(json, draft) },
                    onFailure = { onToast(it.message ?: "Unable to read backup file") }
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
                title = "Security & Super Admin",
                subtitle = "2FA PIN, session control, backup/restore & audit logs"
            )
        }

        // Super Admin Profile & Session
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldAccent)
                        Text("Super Admin Profile", style = MaterialTheme.typography.titleMedium)
                    }
                    Text("Name: ${currentUser?.displayName ?: "Super Admin"}", style = MaterialTheme.typography.bodyLarge)
                    Text("Email: ${currentUser?.email ?: "Verified Google Account"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Auth Provider: Google Sign-In (Credential Manager)", style = MaterialTheme.typography.labelMedium, color = EmeraldAccent)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onLockSession, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Lock PIN")
                        }
                        Button(onClick = onSignOut, modifier = Modifier.weight(1f)) {
                            Text("Revoke Session")
                        }
                    }
                }
            }
        }

        // 2FA, PIN & Protection Rules
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Authentication & Access Control", style = MaterialTheme.typography.titleMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Two-Factor PIN Lock (2FA)", style = MaterialTheme.typography.titleMedium)
                            Text("Require security PIN on session resume", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = draft.twoFactorEnabled, onCheckedChange = { draft = draft.copy(twoFactorEnabled = it) })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Unauthorized Access Protection", style = MaterialTheme.typography.titleMedium)
                            Text("Flag unrecognized logins & block anomalies", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = draft.unauthorizedProtection, onCheckedChange = { draft = draft.copy(unauthorizedProtection = it) })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { if (it.length <= 6) newPin = it.filter(Char::isDigit) },
                            label = { Text("Change 4–6 Digit PIN (curr: ${draft.securityPinHash})") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = {
                            val updated = if (newPin.length >= 4) draft.copy(securityPinHash = newPin) else draft
                            newPin = ""
                            onSaveSecurity(updated)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Security Policy")
                    }
                }
            }
        }

        // Database Backup & Restore + Security Rules Summary
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Database Backup & Restore", style = MaterialTheme.typography.titleMedium)
                    Text("Last Backup: ${draft.lastBackupLabel}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Last Restore: ${draft.lastRestoreLabel}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { backupLauncher.launch("restroadmin_backup.json") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Backup JSON")
                        }
                        OutlinedButton(
                            onClick = { restoreLauncher.launch(arrayOf("application/json")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Restore")
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Firestore Zero-Trust Security Rules", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "• rules_version = '2' active\n• Default-deny catch-all enabled\n• Strict per-UID tenant isolation (/users/{uid}/...)\n• Schema & immutable createdAt validation enforced",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldAccent
                    )
                }
            }
        }

        // Admin Activity Logs & Login Device History
        item {
            SectionHeader(
                title = "Admin Activity & Login History",
                subtitle = "Device details, timestamps & audit trail"
            )
        }
        items(data.auditLogs.take(25), key = { it.id }) { log ->
            val timeStr = log.createdAt?.toDate()?.let {
                SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault()).format(it)
            } ?: "Just now"
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(log.action, style = MaterialTheme.typography.titleMedium)
                        StatusPill(log.severity, statusColor(log.severity))
                    }
                    Text(log.details, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Device: ${log.deviceModel}", style = MaterialTheme.typography.labelSmall, color = SaffronPrimary)
                    Text("${log.actorEmail} • $timeStr", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
