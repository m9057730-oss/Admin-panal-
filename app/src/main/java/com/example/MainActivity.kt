package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.SuperAdminAuthScreen
import com.example.auth.authStateFlow
import com.example.auth.signOutAdmin
import com.example.data.firebase.AdminRepository
import com.example.data.model.UiState
import com.example.ui.screens.AdminMenuScreen
import com.example.ui.screens.AppManagementScreen
import com.example.ui.screens.CommandMonitorScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EmployeesScreen
import com.example.ui.screens.FinanceScreen
import com.example.ui.screens.OperationsScreen
import com.example.ui.screens.ReportsAnalyticsScreen
import com.example.ui.screens.RestaurantsScreen
import com.example.ui.screens.SecurityScreen
import com.example.ui.theme.RestroAdminTheme
import com.example.viewmodel.AdminViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

object AdminRoutes {
    const val DASHBOARD = "dashboard"
    const val RESTAURANTS = "restaurants"
    const val FINANCE = "finance"
    const val OPERATIONS = "operations"
    const val MENU = "menu"
    const val APP_MANAGEMENT = "app_management"
    const val EMPLOYEES = "employees"
    const val REPORTS = "reports"
    const val SECURITY = "security"
    const val COMMAND_MONITOR = "command_monitor"
}

private data class TopNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val primaryTabs = listOf(
    TopNavItem(AdminRoutes.DASHBOARD, "Dashboard", Icons.Default.Dashboard),
    TopNavItem(AdminRoutes.RESTAURANTS, "Tenants", Icons.Default.Restaurant),
    TopNavItem(AdminRoutes.FINANCE, "Finance", Icons.Default.CurrencyRupee),
    TopNavItem(AdminRoutes.OPERATIONS, "Ops & Care", Icons.Default.SupportAgent),
    TopNavItem(AdminRoutes.MENU, "Menu", Icons.Default.GridView)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        RestroAdminTheme(darkTheme = true) {
            SuperAdminAuthScreen(onAuthSuccess = {})
        }
    } else {
        key(user.uid) {
            val viewModel: AdminViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        AdminViewModel(AdminRepository(db), user.uid)
                    }
                }
            )
            val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()
            RestroAdminTheme(darkTheme = darkTheme) {
                AuthenticatedAdminShell(viewModel = viewModel, darkTheme = darkTheme)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedAdminShell(
    viewModel: AdminViewModel,
    darkTheme: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AdminRoutes.DASHBOARD

    val workspaceState by viewModel.workspaceState.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val pinVerified by viewModel.pinVerified.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    val isPrimaryTab = primaryTabs.any { it.route == currentRoute }
    val screenTitle = when (currentRoute) {
        AdminRoutes.DASHBOARD -> "RestroAdmin Command"
        AdminRoutes.RESTAURANTS -> "Tenants & Subscriptions"
        AdminRoutes.FINANCE -> "Payments, GST & P&L"
        AdminRoutes.OPERATIONS -> "Support, Alerts & Growth"
        AdminRoutes.MENU -> "Admin Panel Menu"
        AdminRoutes.APP_MANAGEMENT -> "App Configuration"
        AdminRoutes.EMPLOYEES -> "Employee & RBAC"
        AdminRoutes.REPORTS -> "Reports & Analytics"
        AdminRoutes.SECURITY -> "Security & Backups"
        AdminRoutes.COMMAND_MONITOR -> "Live Ops & Telemetry"
        else -> "RestroAdmin"
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val onSignOut = {
        signOutAdmin(credentialManager = credentialManager, scope = scope)
    }

    if (!pinVerified && workspaceState is UiState.Success) {
        val expectedPin = (workspaceState as UiState.Success).data.securitySettings.securityPinHash
        PinLockScreen(
            onVerify = { pin -> viewModel.verifyPin(pin, expectedPin) },
            onSignOut = onSignOut
        )
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 700.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = screenTitle,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        if (!isPrimaryTab) {
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier.testTag("nav_back_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar {
                        primaryTabs.forEach { tab ->
                            NavigationBarItem(
                                selected = currentRoute == tab.route,
                                onClick = { navigateTo(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        primaryTabs.forEach { tab ->
                            NavigationRailItem(
                                selected = currentRoute == tab.route,
                                onClick = { navigateTo(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when (val state = workspaceState) {
                        is UiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    Text("Syncing SaaS Firestore Workspace...", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        is UiState.Error -> {
                            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("Firestore Sync Error", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                                        Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                                        Button(onClick = onSignOut) { Text("Re-Authenticate") }
                                    }
                                }
                            }
                        }
                        is UiState.Success -> {
                            val data = state.data
                            NavHost(
                                navController = navController,
                                startDestination = AdminRoutes.DASHBOARD,
                                modifier = Modifier.widthIn(max = 960.dp).fillMaxSize()
                            ) {
                                composable(AdminRoutes.DASHBOARD) {
                                    DashboardScreen(
                                        data = data,
                                        onNavigateRoute = { navigateTo(it) },
                                        onSeedPortfolio = { viewModel.seedSamplePortfolio() }
                                    )
                                }
                                composable(AdminRoutes.RESTAURANTS) {
                                    RestaurantsScreen(
                                        data = data,
                                        onSaveRestaurant = { viewModel.saveRestaurant(it) },
                                        onToggleBlock = { viewModel.toggleRestaurantBlock(it) },
                                        onDeleteRestaurant = { viewModel.deleteRestaurant(it) },
                                        onResetPassword = { viewModel.triggerPasswordReset(it) },
                                        onUpdateSubscription = { tenant, plan, cycle, subStatus, trialDays, pay ->
                                            viewModel.updateSubscription(tenant, plan, cycle, subStatus, trialDays, pay)
                                        }
                                    )
                                }
                                composable(AdminRoutes.FINANCE) {
                                    FinanceScreen(
                                        data = data,
                                        onRecordPayment = { rest, plan, cycle, method, status ->
                                            viewModel.recordPayment(rest, plan, cycle, method, status)
                                        },
                                        onIssueRefund = { pay, reason -> viewModel.issueRefund(pay, reason) },
                                        onUpdatePaymentStatus = { pay, status -> viewModel.updatePaymentStatus(pay, status) }
                                    )
                                }
                                composable(AdminRoutes.OPERATIONS) {
                                    OperationsScreen(
                                        data = data,
                                        onCreateTicket = { rest, cat, sub, desc, prio ->
                                            viewModel.createTicket(rest, cat, sub, desc, prio)
                                        },
                                        onUpdateTicket = { tkt, prio, st, reply ->
                                            viewModel.updateTicket(tkt, prio, st, reply)
                                        },
                                        onSendNotification = { title, msg, aud, cat, targets, sched, count ->
                                            viewModel.sendNotification(title, msg, aud, cat, targets, sched, count)
                                        },
                                        onSaveCampaign = { viewModel.saveCampaign(it) }
                                    )
                                }
                                composable(AdminRoutes.MENU) {
                                    AdminMenuScreen(
                                        data = data,
                                        isDarkTheme = darkTheme,
                                        onToggleTheme = { viewModel.toggleTheme() },
                                        onNavigateRoute = { navigateTo(it) },
                                        onSignOut = onSignOut
                                    )
                                }
                                composable(AdminRoutes.APP_MANAGEMENT) {
                                    AppManagementScreen(
                                        config = data.appConfig,
                                        onSaveConfig = { viewModel.saveAppConfig(it) }
                                    )
                                }
                                composable(AdminRoutes.EMPLOYEES) {
                                    EmployeesScreen(
                                        data = data,
                                        onSaveEmployee = { viewModel.saveEmployee(it) },
                                        onDeleteEmployee = { viewModel.deleteEmployee(it) }
                                    )
                                }
                                composable(AdminRoutes.REPORTS) {
                                    ReportsAnalyticsScreen(
                                        data = data,
                                        onToast = { viewModel.showToast(it) }
                                    )
                                }
                                composable(AdminRoutes.SECURITY) {
                                    SecurityScreen(
                                        data = data,
                                        onSaveSecurity = { viewModel.saveSecuritySettings(it) },
                                        onBackupRecorded = { viewModel.markBackupCompleted(it) },
                                        onRestoreJson = { json, sec -> viewModel.restoreFromBackupJson(json, sec) },
                                        onLockSession = { viewModel.lockWithPinIfEnabled(true) },
                                        onSignOut = onSignOut,
                                        onToast = { viewModel.showToast(it) }
                                    )
                                }
                                composable(AdminRoutes.COMMAND_MONITOR) {
                                    CommandMonitorScreen(
                                        data = data,
                                        onToggleCrashResolved = { viewModel.toggleCrashResolved(it) },
                                        onDismissSuspicious = { viewModel.dismissSuspiciousAlert(it) },
                                        onConvertDemoToReal = { tenant ->
                                            viewModel.saveRestaurant(tenant.copy(demoAccount = false))
                                        },
                                        onSeedPortfolio = { viewModel.seedSamplePortfolio() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinLockScreen(
    onVerify: (String) -> Boolean,
    onSignOut: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Super Admin 2FA PIN Lock", style = MaterialTheme.typography.titleLarge)
                Text("Enter your security PIN (default: 1234)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6) pin = it.filter(Char::isDigit) },
                    label = { Text("Security PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error) {
                    Text("Invalid PIN. Try again.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                }
                Button(
                    onClick = {
                        error = !onVerify(pin)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Unlock Console")
                }
                Spacer(Modifier.height(4.dp))
                Button(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                    Text("Sign Out")
                }
            }
        }
    }
}
