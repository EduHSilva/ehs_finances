package com.ehs.finances.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
import com.ehs.finances.core.ui.components.InitialAvatar
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
import kotlinx.coroutines.launch

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

private val drawerDestinations = tabs + Tab(Routes.PROFILE, "profile", Icons.Filled.ManageAccounts)

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
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val strings = LocalStrings.current
    val session by container.authRepository.currentSession.collectAsStateWithLifecycle()
    val accounts by container.financeState.visibleAccounts.collectAsStateWithLifecycle()
    val activeAccountId by container.financeState.activeAccountId.collectAsStateWithLifecycle()

    val showChrome = route == null || tabs.any { it.route == route }
    val onLogout: () -> Unit = {
        scope.launch { drawerState.close() }
        container.authRepository.logout()
    }

    LaunchedEffect(Unit) {
        container.financeState.load()
        // Renova o token ao abrir o app; falha de rede não derruba a sessão salva.
        runCatching { container.authRepository.refreshUser() }
    }

    CompositionLocalProvider(LocalSnackbarHost provides snackbarHostState) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = showChrome && drawerState.isOpen,
            drawerContent = {
                FinancesDrawer(
                    profileName = session?.user?.name,
                    profileEmail = session?.user?.email,
                    currentRoute = route,
                    onNavigate = { destination ->
                        scope.launch { drawerState.close() }
                        if (tabs.any { it.route == destination }) {
                            navController.switchTab(destination)
                        } else {
                            navController.navigateOnce(destination)
                        }
                    },
                    onLogout = onLogout,
                )
            },
        ) {
            Scaffold(
                topBar = {
                    if (showChrome) {
                        AppTopBar(
                            accounts = accounts,
                            activeAccountId = activeAccountId,
                            profileName = session?.user?.name,
                            onOpenMenu = { scope.launch { drawerState.open() } },
                            onSelectAccount = container.financeState::setActiveAccount,
                            onOpenProfile = { navController.navigateOnce(Routes.PROFILE) },
                            onOpenCards = { navController.navigateOnce(Routes.CARDS) },
                            onOpenTransfers = { navController.navigateOnce(Routes.TRANSFERS) },
                        )
                    }
                },
            bottomBar = {
                if (showChrome) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                    ) {
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
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = FinancesTheme.colors.primarySoft,
                                ),
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
}

@Composable
private fun FinancesDrawer(
    profileName: String?,
    profileEmail: String?,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
) {
    val s = LocalStrings.current
    val colors = FinancesTheme.colors

    ModalDrawerSheet(
        drawerContainerColor = colors.sidebar,
        drawerContentColor = Color.White,
        modifier = Modifier.width(300.dp),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(colors.sidebarTop, colors.sidebar),
                    ),
                )
                .padding(20.dp),
        ) {
            BrandMark(dark = true)
            Spacer(Modifier.size(28.dp))
            drawerDestinations.forEach { destination ->
                val active = currentRoute == destination.route
                val rowBrush: Brush = if (active) {
                    Brush.horizontalGradient(
                        listOf(
                            colors.sidebarAccent.copy(alpha = 0.20f),
                            colors.sidebarAccent.copy(alpha = 0.06f),
                        ),
                    )
                } else {
                    SolidColor(Color.Transparent)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(rowBrush, RoundedCornerShape(8.dp))
                        .clickable { onNavigate(destination.route) }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .height(16.dp)
                            .background(
                                if (active) colors.sidebarAccent else Color.Transparent,
                                RoundedCornerShape(2.dp),
                            ),
                    )
                    Icon(
                        destination.icon,
                        contentDescription = null,
                        tint = if (active) Color.White else colors.sidebarText,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        s[destination.labelKey],
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (active) Color.White else colors.sidebarText,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.size(4.dp))
            }

            Spacer(Modifier.weight(1f))
            HorizontalDivider(color = Color.White.copy(alpha = 0.13f))
            Spacer(Modifier.size(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InitialAvatar(
                    name = profileName.orEmpty(),
                    size = 38.dp,
                    background = colors.sidebarAccent,
                    contentColor = colors.sidebarOnAccent,
                )
                Column {
                    Text(
                        profileName?.takeIf { it.isNotBlank() } ?: s["profile"],
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        profileEmail?.takeIf { it.isNotBlank() } ?: "—",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.sidebarText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.size(14.dp))
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = colors.sidebarText),
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text(s["logoutAction"], style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.size(10.dp))
            Text(
                "Desenvolvido por EHS Solutions",
                style = MaterialTheme.typography.labelSmall,
                color = colors.sidebarText,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun BrandMark(dark: Boolean = false) {
    val colors = FinancesTheme.colors
    val badgeColor = if (dark) colors.sidebarAccent else MaterialTheme.colorScheme.primary
    val badgeContent = if (dark) colors.sidebarOnAccent else MaterialTheme.colorScheme.onPrimary
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(badgeColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.AccountBalanceWallet,
                contentDescription = null,
                tint = badgeContent,
                modifier = Modifier.size(19.dp),
            )
        }
        Text(
            text = LocalStrings.current["appName"],
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (dark) Color.White else MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun AppTopBar(
    accounts: List<FinancialAccount>,
    activeAccountId: String?,
    profileName: String?,
    onOpenMenu: () -> Unit,
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
                IconButton(onClick = onOpenMenu) {
                    Icon(
                        Icons.Filled.Menu,
                        contentDescription = LocalStrings.current["menu"],
                        modifier = Modifier.size(22.dp),
                    )
                }
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
