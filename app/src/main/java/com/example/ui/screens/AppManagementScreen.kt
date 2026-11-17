package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppConfigRecord
import com.example.ui.components.SectionHeader

@Composable
fun AppManagementScreen(
    config: AppConfigRecord,
    onSaveConfig: (AppConfigRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by remember(config.id, config.updatedAt) { mutableStateOf(config) }

    LaunchedEffect(config) {
        draft = config
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "App Management & Remote Config",
                subtitle = "Versions, maintenance mode, feature flags & announcements"
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_restro_mark_1794885223173),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Column {
                            Text("Brand & Versioning", style = MaterialTheme.typography.titleMedium)
                            Text("Downloads tracked: ${draft.totalDownloads}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    OutlinedTextField(
                        value = draft.appName,
                        onValueChange = { draft = draft.copy(appName = it) },
                        label = { Text("App Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = draft.currentVersion,
                            onValueChange = { draft = draft.copy(currentVersion = it) },
                            label = { Text("Current Ver") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = draft.latestVersion,
                            onValueChange = { draft = draft.copy(latestVersion = it) },
                            label = { Text("Latest Ver") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = draft.minRequiredVersion,
                            onValueChange = { draft = draft.copy(minRequiredVersion = it) },
                            label = { Text("Min Req") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    ToggleRow("Force Update", "Require users below minimum version to update", draft.forceUpdate) {
                        draft = draft.copy(forceUpdate = it)
                    }
                    ToggleRow("Maintenance Mode", "Temporarily pause client app access", draft.maintenanceMode) {
                        draft = draft.copy(maintenanceMode = it)
                    }
                    if (draft.maintenanceMode) {
                        OutlinedTextField(
                            value = draft.maintenanceMessage,
                            onValueChange = { draft = draft.copy(maintenanceMessage = it) },
                            label = { Text("Maintenance Banner Message") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enable / Disable Features", style = MaterialTheme.typography.titleMedium)
                    ToggleRow("Online Ordering", "Allow customer web & app orders", draft.featureOnlineOrdering) {
                        draft = draft.copy(featureOnlineOrdering = it)
                    }
                    ToggleRow("Table QR Menu", "Contactless QR table menu module", draft.featureQrMenu) {
                        draft = draft.copy(featureQrMenu = it)
                    }
                    ToggleRow("Kitchen Display (KDS)", "Real-time KOT display syncing", draft.featureKitchenDisplay) {
                        draft = draft.copy(featureKitchenDisplay = it)
                    }
                    ToggleRow("GST Invoicing", "Automated CGST/SGST tax billing", draft.featureGstBilling) {
                        draft = draft.copy(featureGstBilling = it)
                    }
                    ToggleRow("Loyalty Rewards", "Customer cashback & points engine", draft.featureLoyalty) {
                        draft = draft.copy(featureLoyalty = it)
                    }
                    ToggleRow("Multi-Branch Sync", "Enterprise multi-outlet management", draft.featureMultiBranch) {
                        draft = draft.copy(featureMultiBranch = it)
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Links & Website Configuration", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = draft.androidDownloadUrl,
                        onValueChange = { draft = draft.copy(androidDownloadUrl = it) },
                        label = { Text("Android App Download Link") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draft.websiteUrl,
                        onValueChange = { draft = draft.copy(websiteUrl = it) },
                        label = { Text("Website Configuration URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draft.privacyPolicyUrl,
                        onValueChange = { draft = draft.copy(privacyPolicyUrl = it) },
                        label = { Text("Privacy Policy URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draft.termsUrl,
                        onValueChange = { draft = draft.copy(termsUrl = it) },
                        label = { Text("Terms & Conditions URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
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
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("System Announcements", style = MaterialTheme.typography.titleMedium)
                    }
                    ToggleRow("Show Announcement Banner", "Broadcast inside all restaurant apps", draft.announcementActive) {
                        draft = draft.copy(announcementActive = it)
                    }
                    OutlinedTextField(
                        value = draft.announcementTitle,
                        onValueChange = { draft = draft.copy(announcementTitle = it) },
                        label = { Text("Announcement Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draft.announcementBody,
                        onValueChange = { draft = draft.copy(announcementBody = it) },
                        label = { Text("Announcement Details") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Button(
                onClick = { onSaveConfig(draft) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_app_config_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Publish App Configuration")
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
