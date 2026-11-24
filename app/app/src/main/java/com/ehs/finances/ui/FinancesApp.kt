package com.ehs.finances.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ehs.finances.core.i18n.LocalStrings
import com.ehs.finances.core.i18n.Strings
import com.ehs.finances.core.settings.AppTheme
import com.ehs.finances.core.settings.LocalAppSettings
import com.ehs.finances.core.ui.LocalSnackbarHost
import com.ehs.finances.core.ui.components.SettingsActions
import com.ehs.finances.core.ui.theme.FinancesTheme
import com.ehs.finances.data.model.FinancialAccount
import com.ehs.finances.di.AppContainer
import com.ehs.finances.di.LocalContainer
import com.ehs.finances.feature.auth.AuthScreen
import com.ehs.finances.feature.cards.CardInvoiceScreen
import com.ehs.finances.feature.cards.CardsScreen
import com.ehs.finances.feature.categories.CategoriesScreen
import com.ehs.finances.feature.dashboard.DashboardScreen
import com.ehs.finances.feature.goals.GoalsScreen
import com.ehs.finances.feature.investments.InvestmentsScreen
import com.ehs.finances.feature.profile.ProfileScreen
import com.ehs.finances.feature.recurrences.RecurrencesScreen
import com.ehs.finances.feature.transactions.StatementImportScreen
import com.ehs.finances.feature.transactions.TransactionsScreen
import com.ehs.finances.feature.transfers.TransfersScreen

private object Routes {
    const val DASHBOARD = "dashboard"
    const val TRANSACTIONS = "transactions"
    const val CATEGORIES = "categories"
    const val RECURRENCES = "recurrences"
    const val GOALS = "goals"
    const val INVESTMENTS = "investments"
    const val PROFILE = "profile"
    const val IMPORT = "import/{review}"
    const val CARDS = "cards"
    const val INVOICES = "cards/{cardId}/invoices"
    const val TRANSFERS = "transfers"

    fun invoices(cardId: String) = "cards/$cardId/invoices"
}

private data class Tab(
    val route: String,
    val labelKey: String,
    val icon: ImageVector,
)

private val tabs = listOf(
    Tab(Routes.DASHBOARD, "dashboard", Icons.Filled.Dashboard),
    Tab(Routes.TRANSACTIONS, "transactions", Icons.Filled.SwapHoriz),
    Tab(Routes.CATEGORIES, "categories", Icons.Filled.Category),
    Tab(Routes.RECURRENCES, "recurrences", Icons.Filled.Repeat),
    Tab(Routes.GOALS, "goals", Icons.Filled.Flag),
    Tab(Routes.INVESTMENTS, "investments", Icons.Filled.TrendingUp),
)

/** Raiz do app: aplica tema/idioma e alterna entre login e área autenticada. */
@Composable
fun FinancesApp(container: AppContainer) {
    val settings = container.settings
    val theme by settings.theme.collectAsStateWithLifecycle()
    val language by settings.language.collectAsStateWithLifecycle()
    val session by container.sessionManager.session.collectAsStateWithLifecycle()
    val strings = remember(language) { Strings(language) }

    LaunchedEffect(session) {
        if (session == null) container.financeState.clear()
    }

    CompositionLocalProvider(
        LocalContainer provides container,
        LocalAppSettings provides settings,
        LocalStrings provides strings,
    ) {
        FinancesTheme(darkTheme = theme == AppTheme.DARK) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                if (session == null) {
                    AuthScreen()
                } else {
                    AuthenticatedApp(container)
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedApp(container: AppContainer) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val strings = LocalStrings.current
    val session by container.authRepository.currentSession.collectAsStateWithLifecycle()
    val accounts by container.financeState.visibleAccounts.collectAsStateWithLifecycle()
    val activeAccountId by container.financeState.activeAccountId.collectAsStateWithLifecycle()

    val showChrome = route == null || tabs.any { it.route == route }

    LaunchedEffect(Unit) {
        container.financeState.load()
        // Renova o token ao abrir o app; falha de rede não derruba a sessão salva.
        runCatching { container.authRepository.refreshUser() }
    }

    CompositionLocalProvider(LocalSnackbarHost provides snackbarHostState) {
        Scaffold(
            topBar = {
                if (showChrome) {
                    AppTopBar(
                        accounts = accounts,
                        activeAccountId = activeAccountId,
                        profileName = session?.user?.name,
                        onSelectAccount = container.financeState::setActiveAccount,
                        onOpenProfile = { navController.navigateOnce(Routes.PROFILE) },
                        onOpenCards = { navController.navigateOnce(Routes.CARDS) },
                        onOpenTransfers = { navController.navigateOnce(Routes.TRANSFERS) },
                    )
                }
            },
            bottomBar = {
                if (showChrome) {
                    NavigationBar {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = route == tab.route,
                                onClick = { navController.switchTab(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = null, modifier = Modifier.size(22.dp)) },
                                label = {
                                    Text(
                                        strings[tab.labelKey],
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                            )
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.DASHBOARD,
                modifier = Modifier.padding(padding),
            ) {
                composable(Routes.DASHBOARD) {
                    DashboardScreen(
                        onOpenProfile = { navController.navigateOnce(Routes.PROFILE) },
                        onOpenCards = { navController.navigateOnce(Routes.CARDS) },
                    )
                }
                composable(Routes.TRANSACTIONS) {
                    TransactionsScreen(
                        onOpenImport = { review ->
                            navController.navigate("import/$review")
                        },
                        onOpenTransfers = { navController.navigateOnce(Routes.TRANSFERS) },
                        onOpenInvoices = { navController.navigateOnce(Routes.CARDS) },
                    )
                }
                composable(Routes.CARDS) {
                    CardsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenInvoices = { cardId -> navController.navigate(Routes.invoices(cardId)) },
                    )
                }
                composable(
                    route = Routes.INVOICES,
                    arguments = listOf(navArgument("cardId") { type = NavType.StringType }),
                ) { entry ->
                    CardInvoiceScreen(
                        cardId = entry.arguments?.getString("cardId").orEmpty(),
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.TRANSFERS) {
                    TransfersScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.CATEGORIES) { CategoriesScreen() }
                composable(Routes.RECURRENCES) { RecurrencesScreen() }
                composable(Routes.GOALS) { GoalsScreen() }
                composable(Routes.INVESTMENTS) { InvestmentsScreen() }
                composable(Routes.PROFILE) {
                    ProfileScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    route = Routes.IMPORT,
                    arguments = listOf(navArgument("review") { type = NavType.BoolType }),
                ) { entry ->
                    StatementImportScreen(
                        reviewOnly = entry.arguments?.getBoolean("review") ?: false,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppTopBar(
    accounts: List<FinancialAccount>,
    activeAccountId: String?,
    profileName: String?,
    onSelectAccount: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenCards: () -> Unit,
    onOpenTransfers: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AccountMenu(
                    accounts = accounts,
                    activeAccountId = activeAccountId,
                    onSelect = onSelectAccount,
                    onManage = onOpenProfile,
                    onOpenCards = onOpenCards,
                    onOpenTransfers = onOpenTransfers,
                    modifier = Modifier.weight(1f),
                )
                SettingsActions(profileName = profileName, onProfile = onOpenProfile)
            }
            HorizontalDivider(color = FinancesTheme.colors.border)
        }
    }
}

@Composable
private fun AccountMenu(
    accounts: List<FinancialAccount>,
    activeAccountId: String?,
    onSelect: (String) -> Unit,
    onManage: () -> Unit,
    onOpenCards: () -> Unit,
    onOpenTransfers: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    val active = accounts.firstOrNull { it.id == activeAccountId } ?: accounts.firstOrNull()

    Box(modifier) {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            Column(Modifier.weight(1f, fill = false)) {
                Text(
                    strings["activeAccountLabel"],
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = active?.let { "${it.type} · ${it.name}" } ?: strings["noAccounts"],
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(account.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                account.type,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(account.id)
                    },
                )
            }
            if (accounts.isNotEmpty()) HorizontalDivider()
            DropdownMenuItem(
                text = { Text(strings["manageCards"]) },
                leadingIcon = { Icon(Icons.Filled.CreditCard, contentDescription = null) },
                onClick = {
                    expanded = false
                    onOpenCards()
                },
            )
            DropdownMenuItem(
                text = { Text(strings["transfers"]) },
                leadingIcon = { Icon(Icons.Filled.SwapHoriz, contentDescription = null) },
                onClick = {
                    expanded = false
                    onOpenTransfers()
                },
            )
            DropdownMenuItem(
                text = { Text(strings["manageAccounts"]) },
                leadingIcon = { Icon(Icons.Filled.ManageAccounts, contentDescription = null) },
                onClick = {
                    expanded = false
                    onManage()
                },
            )
        }
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.navigateOnce(route: String) {
    navigate(route) { launchSingleTop = true }
}
