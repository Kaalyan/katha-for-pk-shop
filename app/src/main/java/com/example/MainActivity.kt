package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.DemoKhataStore
import com.example.data.KhataRepository
import com.example.data.LanguagePreferences
import com.example.localization.stringsFor
import com.example.model.AppLanguage
import com.example.model.Customer
import com.example.model.LedgerTransaction
import com.example.model.OwnerRole
import com.example.model.PaymentRecord
import com.example.model.UiState
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.ShopOnboardingScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOutUser
import com.example.ui.components.ConnectivityStatusBar
import com.example.ui.components.DemoModeBanner
import com.example.ui.components.LanguageTogglePill
import com.example.ui.components.LiveSyncBadge
import com.example.ui.screens.ActivityAndNotificationsScreen
import com.example.ui.screens.AddEditCustomerDialog
import com.example.ui.screens.AddUdharDialog
import com.example.ui.screens.CustomerKhataDetailScreen
import com.example.ui.screens.CustomerPreviewScreen
import com.example.ui.screens.CustomersListScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.KhataTransactionsAndReportsScreen
import com.example.ui.screens.PaymentReceiptDialog
import com.example.ui.screens.ReceivePaymentDialog
import com.example.ui.screens.ReverseTransactionDialog
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.KhataCrimsonDue
import com.example.ui.theme.KhataForestGreen
import com.example.ui.theme.KiranaKhataTheme
import com.example.ui.viewmodel.KhataViewModel
import com.example.ui.viewmodel.LedgerEvent
import com.example.util.ConnectivityMonitor
import com.example.util.NotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class KhataTab { HOME, CUSTOMERS, KHATA, ACTIVITY, SETTINGS }

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    NotificationHelper.ensureChannel(this)
    setContent {
      AppRoot()
    }
  }
}

@Composable
private fun AppRoot() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val languagePrefs = remember(context) { LanguagePreferences(context.applicationContext) }
  val language by languagePrefs.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.EN)

  var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }
  var isDemoMode by remember { mutableStateOf(false) }
  var demoInitialCustomerView by remember { mutableStateOf(false) }
  var demoSessionId by remember { mutableIntStateOf(0) }

  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth ->
      currentUser = auth.currentUser
      if (auth.currentUser != null) {
        isDemoMode = false
        demoInitialCustomerView = false
      }
    }
    Firebase.auth.addAuthStateListener(listener)
    onDispose { Firebase.auth.removeAuthStateListener(listener) }
  }

  LaunchedEffect(Unit) {
    if (Firebase.auth.currentUser == null) {
      val cm = CredentialManager.create(context)
      attemptAutoSignIn(
        context = context,
        credentialManager = cm,
        onAuthSuccess = {
          isDemoMode = false
          demoInitialCustomerView = false
          currentUser = Firebase.auth.currentUser
        },
        onUnauthenticated = {},
        scope = scope,
      )
    }
  }

  KiranaKhataTheme(language = language) {
    val user = currentUser
    if (user == null && !isDemoMode) {
      AuthScreenWrapper(
        language = language,
        languagePrefs = languagePrefs,
        onAuthSuccess = {
          isDemoMode = false
          demoInitialCustomerView = false
          currentUser = Firebase.auth.currentUser
        },
        onTryDemoClick = {
          demoInitialCustomerView = false
          demoSessionId += 1
          isDemoMode = true
        },
        onTryCustomerPreviewClick = {
          demoInitialCustomerView = true
          demoSessionId += 1
          isDemoMode = true
        },
      )
    } else if (user == null && isDemoMode) {
      val demoSessionKey = "demo_mode_${demoSessionId}_${demoInitialCustomerView}"
      key(demoSessionKey) {
        val demoStore = remember(demoSessionKey) { DemoKhataStore(initialCustomerView = demoInitialCustomerView) }
        AuthenticatedKhataHost(
          user = null,
          demoStore = demoStore,
          demoSessionKey = demoSessionKey,
          languagePrefs = languagePrefs,
          onSignedOut = {
            isDemoMode = false
            demoInitialCustomerView = false
          },
        )
      }
    } else {
      val authUser = checkNotNull(user)
      val uid = authUser.uid
      key(uid) {
        AuthenticatedKhataHost(
          user = authUser,
          demoStore = null,
          demoSessionKey = null,
          languagePrefs = languagePrefs,
          onSignedOut = { currentUser = null },
        )
      }
    }
  }
}

@Composable
private fun AuthScreenWrapper(
  language: AppLanguage,
  languagePrefs: LanguagePreferences,
  onAuthSuccess: () -> Unit,
  onTryDemoClick: () -> Unit,
  onTryCustomerPreviewClick: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  AuthScreen(
    language = language,
    onLanguageChange = { newLang ->
      scope.launch { languagePrefs.setLanguage(newLang) }
    },
    onAuthSuccess = onAuthSuccess,
    onTryDemoClick = onTryDemoClick,
    onTryCustomerPreviewClick = onTryCustomerPreviewClick,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedKhataHost(
  user: FirebaseUser?,
  demoStore: DemoKhataStore?,
  demoSessionKey: String?,
  languagePrefs: LanguagePreferences,
  onSignedOut: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val vmKey = demoSessionKey ?: requireNotNull(user).uid
  val viewModel: KhataViewModel =
    viewModel(
      key = vmKey,
      factory =
        viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY])
            if (demoStore != null) {
              KhataViewModel(
                repository = demoStore,
                languagePrefs = LanguagePreferences(app),
                connectivityMonitor = ConnectivityMonitor(app),
                currentUserId = DemoKhataStore.OWNER_1_UID,
                currentUserName = "Ramesh (Main Admin)",
                currentUserEmail = "ramesh.kirana@demo.local",
                isDemoMode = true,
                demoStore = demoStore,
              )
            } else {
              val authUser = requireNotNull(user)
              val databaseId = app.getString(R.string.firestore_database_id)
              val db = FirebaseFirestore.getInstance(databaseId)
              KhataViewModel(
                repository = KhataRepository(db, Firebase.auth),
                languagePrefs = LanguagePreferences(app),
                connectivityMonitor = ConnectivityMonitor(app),
                currentUserId = authUser.uid,
                currentUserName = authUser.displayName ?: authUser.email?.substringBefore("@") ?: "Owner",
                currentUserEmail = authUser.email ?: "",
              )
            }
          }
        },
    )

  // Request runtime notification permission gracefully on API 33+ (real mode)
  val notifPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
  LaunchedEffect(viewModel.isDemoMode) {
    if (!viewModel.isDemoMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      if (!granted) {
        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  val language by viewModel.language.collectAsStateWithLifecycle()
  val strings = remember(language) { stringsFor(language) }
  val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
  val shopsState by viewModel.userShopsState.collectAsStateWithLifecycle()
  val invitesState by viewModel.pendingInvitesState.collectAsStateWithLifecycle()
  val activeShop by viewModel.activeShop.collectAsStateWithLifecycle()
  val membersState by viewModel.membersState.collectAsStateWithLifecycle()
  val customersState by viewModel.customersState.collectAsStateWithLifecycle()
  val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
  val paymentsState by viewModel.paymentsState.collectAsStateWithLifecycle()
  val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
  val activityLogsState by viewModel.activityLogsState.collectAsStateWithLifecycle()
  val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
  val unreadCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
  val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
  val activeOwnerId by viewModel.activeOwnerId.collectAsStateWithLifecycle()
  val activeDemoRole by viewModel.activeDemoRole.collectAsStateWithLifecycle()
  val isCustomerPreview by viewModel.isCustomerPreview.collectAsStateWithLifecycle()
  val previewCustomerId by viewModel.previewCustomerId.collectAsStateWithLifecycle()
  val isDemoOwnerDeactivated by viewModel.isDemoOwnerDeactivated.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  var activeReceipt by remember { mutableStateOf<PaymentRecord?>(null) }

  LaunchedEffect(viewModel, language) {
    viewModel.events.collectLatest { event ->
      when (event) {
        is LedgerEvent.Message -> snackbarHostState.showSnackbar(event.text)
        is LedgerEvent.PaymentReceiptReady -> activeReceipt = event.receipt
        is LedgerEvent.NewCoOwnerNotification -> {
          val msg =
            if (language == AppLanguage.TE) event.notification.messageTe
            else event.notification.messageEn
          NotificationHelper.showOwnerAlert(
            context = context,
            idSeed = event.notification.id,
            title = activeShop?.name ?: strings.appTitle,
            body = msg,
          )
        }
      }
    }
  }

  val doSignOut: () -> Unit = {
    if (viewModel.isDemoMode) {
      onSignedOut()
    } else {
      signOutUser(
        credentialManager = credentialManager,
        scope = scope,
        onSignOutComplete = onSignedOut,
      )
    }
  }

  when (val shopsUi = shopsState) {
    is UiState.Loading -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          CircularProgressIndicator()
          Text(strings.loadingLabel, style = MaterialTheme.typography.bodyMedium)
        }
      }
    }

    is UiState.Error -> {
      Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(shopsUi.message, color = MaterialTheme.colorScheme.error)
          androidx.compose.material3.Button(onClick = doSignOut) {
            Text(strings.signOutBtn)
          }
        }
      }
    }

    is UiState.Success -> {
      val shop = activeShop
      if (shop == null) {
        val pendingInvites = (invitesState as? UiState.Success)?.data.orEmpty()
        ShopOnboardingScreen(
          language = language,
          onLanguageChange = viewModel::setLanguage,
          userEmail = user?.email ?: viewModel.currentUserEmail,
          defaultOwnerName = user?.displayName ?: viewModel.currentUserName,
          pendingInvites = pendingInvites,
          isSubmitting = isSubmitting,
          onCreateShop = viewModel::createShop,
          onAcceptInvite = viewModel::acceptInvite,
          onSignOut = doSignOut,
        )
      } else {
        val customers = (customersState as? UiState.Success)?.data.orEmpty()
        val transactions = (transactionsState as? UiState.Success)?.data.orEmpty()
        val payments = (paymentsState as? UiState.Success)?.data.orEmpty()
        val members = (membersState as? UiState.Success)?.data.orEmpty()
        val notifications = (notificationsState as? UiState.Success)?.data.orEmpty()
        val activityLogs = (activityLogsState as? UiState.Success)?.data.orEmpty()

        var currentTab by remember { mutableStateOf(KhataTab.HOME) }
        var activityInitialNotif by remember { mutableStateOf(true) }
        var selectedCustomerId by remember { mutableStateOf<String?>(null) }

        var showAddCustomerDialog by remember { mutableStateOf(false) }
        var editingCustomer by remember { mutableStateOf<Customer?>(null) }
        var udharTargetCustomer by remember { mutableStateOf<Customer?>(null) }
        var showUdharDialog by remember { mutableStateOf(false) }
        var paymentTargetCustomer by remember { mutableStateOf<Customer?>(null) }
        var showPaymentDialog by remember { mutableStateOf(false) }
        var reversalTargetTx by remember { mutableStateOf<LedgerTransaction?>(null) }

        val selectedCustomer = customers.firstOrNull { it.id == selectedCustomerId }
        val showDeactivatedOverlay = viewModel.isDemoMode && !isCustomerPreview && isDemoOwnerDeactivated

        if (selectedCustomer != null && !showDeactivatedOverlay) {
          CustomerKhataDetailScreen(
            strings = strings,
            language = language,
            shopName = shop.name,
            customer = selectedCustomer,
            transactions = transactions,
            onBack = { selectedCustomerId = null },
            onEditCustomer = {
              editingCustomer = selectedCustomer
              showAddCustomerDialog = true
            },
            onAddUdhar = {
              udharTargetCustomer = selectedCustomer
              showUdharDialog = true
            },
            onReceivePayment = {
              paymentTargetCustomer = selectedCustomer
              showPaymentDialog = true
            },
            onReverseTransaction = { tx -> reversalTargetTx = tx },
          )
        } else {
          val navItems =
            listOf(
              NavTabSpec(KhataTab.HOME, strings.navHome, Icons.Filled.Home, Icons.Outlined.Home),
              NavTabSpec(KhataTab.CUSTOMERS, strings.navCustomers, Icons.Filled.Groups, Icons.Outlined.Groups),
              NavTabSpec(KhataTab.KHATA, strings.navKhata, Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
              NavTabSpec(KhataTab.ACTIVITY, strings.navActivity, Icons.Filled.Notifications, Icons.Outlined.Notifications, unreadCount),
              NavTabSpec(KhataTab.SETTINGS, strings.navSettings, Icons.Filled.Settings, Icons.Outlined.Settings),
            )

          BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
              containerColor = MaterialTheme.colorScheme.background,
              snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
              contentWindowInsets = WindowInsets(0, 0, 0, 0),
              topBar = {
                Column {
                  TopAppBar(
                    title = {
                      Column {
                        Text(
                          text = shop.name,
                          style = MaterialTheme.typography.titleLarge,
                          fontWeight = FontWeight.Bold,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                          text = "${strings.appTitle} • ${members.count { it.active }}/3",
                          style = MaterialTheme.typography.labelSmall,
                          color = Color.White.copy(alpha = 0.85f),
                        )
                      }
                    },
                    actions = {
                      LiveSyncBadge(isOnline = isOnline, strings = strings)
                      Spacer(Modifier.padding(horizontal = 4.dp))
                      LanguageTogglePill(language = language, onLanguageChange = viewModel::setLanguage)
                      Spacer(Modifier.padding(horizontal = 4.dp))
                    },
                    colors =
                      TopAppBarDefaults.topAppBarColors(
                        containerColor = KhataForestGreen,
                        titleContentColor = Color.White,
                      ),
                  )
                  if (!viewModel.isDemoMode) {
                    ConnectivityStatusBar(isOnline = isOnline, strings = strings)
                  } else {
                    DemoModeBanner(
                      strings = strings,
                      activeRole = activeDemoRole,
                      onSwitchRole = viewModel::switchDemoOwnerRole,
                      onExitDemo = doSignOut,
                      isCustomerPreview = isCustomerPreview,
                      onSelectCustomerPreview = { viewModel.setCustomerPreviewMode(true) },
                    )
                  }
                }
              },
              bottomBar = {
                if (!useRail && !(viewModel.isDemoMode && isCustomerPreview) && !showDeactivatedOverlay) {
                  NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    navItems.forEach { item ->
                      val selected = currentTab == item.tab
                      NavigationBarItem(
                        selected = selected,
                        onClick = {
                          if (item.tab == KhataTab.ACTIVITY) activityInitialNotif = true
                          currentTab = item.tab
                        },
                        icon = {
                          BadgedIcon(
                            icon = if (selected) item.selectedIcon else item.unselectedIcon,
                            label = item.label,
                            badge = item.badge,
                          )
                        },
                        label = {
                          Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        modifier = Modifier.testTag("nav_${item.tab.name.lowercase()}"),
                      )
                    }
                  }
                }
              },
            ) { innerPadding ->
              Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (useRail && !(viewModel.isDemoMode && isCustomerPreview) && !showDeactivatedOverlay) {
                  NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surface,
                  ) {
                    Spacer(Modifier.height(12.dp))
                    navItems.forEach { item ->
                      val selected = currentTab == item.tab
                      NavigationRailItem(
                        selected = selected,
                        onClick = {
                          if (item.tab == KhataTab.ACTIVITY) activityInitialNotif = true
                          currentTab = item.tab
                        },
                        icon = {
                          BadgedIcon(
                            icon = if (selected) item.selectedIcon else item.unselectedIcon,
                            label = item.label,
                            badge = item.badge,
                          )
                        },
                        label = { Text(item.label, maxLines = 1) },
                      )
                    }
                  }
                }

                Box(
                  modifier = Modifier.weight(1f).fillMaxHeight(),
                  contentAlignment = Alignment.TopCenter,
                ) {
                  Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxSize()) {
                    if (viewModel.isDemoMode && isCustomerPreview) {
                      CustomerPreviewScreen(
                        strings = strings,
                        language = language,
                        shopName = shop.name,
                        customers = customers,
                        selectedCustomerId = previewCustomerId,
                        transactions = transactions,
                        payments = payments,
                        onSelectCustomer = viewModel::selectPreviewCustomer,
                        onViewReceipt = { receipt -> activeReceipt = receipt },
                        onSwitchToOwnerView = { viewModel.setCustomerPreviewMode(false) },
                      )
                    } else if (showDeactivatedOverlay) {
                      Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center,
                      ) {
                        Card(
                          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                          border = BorderStroke(1.5.dp, KhataCrimsonDue),
                          shape = RoundedCornerShape(20.dp),
                          modifier = Modifier.fillMaxWidth().testTag("demo_deactivated_card"),
                        ) {
                          Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                          ) {
                            Icon(
                              imageVector = Icons.Filled.Lock,
                              contentDescription = null,
                              tint = KhataCrimsonDue,
                              modifier = Modifier.size(44.dp),
                            )
                            Text(
                              text = strings.demoOwnerDeactivatedTitle,
                              style = MaterialTheme.typography.headlineSmall,
                              color = KhataCrimsonDue,
                              fontWeight = FontWeight.Bold,
                              textAlign = TextAlign.Center,
                            )
                            Text(
                              text = strings.demoOwnerDeactivatedBody,
                              style = MaterialTheme.typography.bodyMedium,
                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                              textAlign = TextAlign.Center,
                            )
                            Button(
                              onClick = { viewModel.switchDemoOwnerRole(OwnerRole.MAIN_ADMIN) },
                              modifier = Modifier.fillMaxWidth().height(50.dp).testTag("switch_to_admin_btn"),
                            ) {
                              Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                              Spacer(Modifier.width(8.dp))
                              Text(strings.switchBackToAdminBtn, fontWeight = FontWeight.Bold)
                            }
                          }
                        }
                      }
                    } else {
                      when (currentTab) {
                      KhataTab.HOME ->
                        HomeDashboardScreen(
                          strings = strings,
                          language = language,
                          stats = stats,
                          recentTransactions = transactions,
                          recentActivities = activityLogs,
                          unreadNotifications = unreadCount,
                          onAddCustomerClick = {
                            editingCustomer = null
                            showAddCustomerDialog = true
                          },
                          onAddUdharClick = {
                            udharTargetCustomer = customers.firstOrNull()
                            showUdharDialog = true
                          },
                          onReceivePaymentClick = {
                            paymentTargetCustomer = customers.firstOrNull { it.outstandingBalancePaise > 0L }
                            showPaymentDialog = true
                          },
                          onViewKhataBookClick = { currentTab = KhataTab.KHATA },
                          onNotificationsClick = {
                            activityInitialNotif = true
                            currentTab = KhataTab.ACTIVITY
                          },
                          onViewAllActivityClick = {
                            activityInitialNotif = false
                            currentTab = KhataTab.ACTIVITY
                          },
                          onOpenCustomerKhata = { cid -> selectedCustomerId = cid },
                        )

                      KhataTab.CUSTOMERS ->
                        CustomersListScreen(
                          strings = strings,
                          customers = customers,
                          onAddCustomerClick = {
                            editingCustomer = null
                            showAddCustomerDialog = true
                          },
                          onOpenCustomerKhata = { cust -> selectedCustomerId = cust.id },
                          onAddUdharForCustomer = { cust ->
                            udharTargetCustomer = cust
                            showUdharDialog = true
                          },
                          onReceivePaymentForCustomer = { cust ->
                            paymentTargetCustomer = cust
                            showPaymentDialog = true
                          },
                        )

                      KhataTab.KHATA ->
                        KhataTransactionsAndReportsScreen(
                          strings = strings,
                          language = language,
                          shopName = shop.name,
                          stats = stats,
                          customers = customers,
                          transactions = transactions,
                          onOpenCustomerKhata = { cid -> selectedCustomerId = cid },
                          onReverseTransaction = { tx -> reversalTargetTx = tx },
                        )

                      KhataTab.ACTIVITY ->
                        ActivityAndNotificationsScreen(
                          strings = strings,
                          language = language,
                          currentUserId = activeOwnerId,
                          notifications = notifications,
                          activityLogs = activityLogs,
                          initialShowNotifications = activityInitialNotif,
                          onMarkRead = viewModel::markNotificationRead,
                          onMarkAllRead = viewModel::markAllNotificationsRead,
                          onOpenCustomerKhata = { cid -> selectedCustomerId = cid },
                        )

                      KhataTab.SETTINGS ->
                        SettingsScreen(
                          strings = strings,
                          language = language,
                          shop = shop,
                          members = members,
                          currentUserId = activeOwnerId,
                          isSubmitting = isSubmitting,
                          onLanguageChange = viewModel::setLanguage,
                          onInviteOwner = viewModel::inviteCoOwner,
                          onRemoveInvite = viewModel::removeInvite,
                          onToggleOwnerActive = viewModel::toggleOwnerActive,
                          onSignOut = doSignOut,
                          isDemoMode = viewModel.isDemoMode,
                          activeDemoRole = activeDemoRole,
                          onSwitchDemoRole = viewModel::switchDemoOwnerRole,
                          onSelectCustomerPreview = { viewModel.setCustomerPreviewMode(true) },
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }

        // Modals & Dialogs
        if (showAddCustomerDialog) {
          AddEditCustomerDialog(
            strings = strings,
            existingCustomers = customers,
            editingCustomer = editingCustomer,
            isSubmitting = isSubmitting,
            onDismiss = {
              showAddCustomerDialog = false
              editingCustomer = null
            },
            onSaveNew = { name, phone, address, openingPaise ->
              viewModel.addCustomer(name, phone, address, openingPaise) {
                showAddCustomerDialog = false
              }
            },
            onUpdateExisting = { id, name, phone, address ->
              viewModel.updateCustomer(id, name, phone, address) {
                showAddCustomerDialog = false
                editingCustomer = null
              }
            },
          )
        }

        if (showUdharDialog) {
          AddUdharDialog(
            strings = strings,
            customers = customers,
            preselectedCustomer = udharTargetCustomer,
            isSubmitting = isSubmitting,
            onDismiss = { showUdharDialog = false },
            onSaveCredit = { customer, amountPaise, desc, qty, notes ->
              viewModel.addCredit(customer, amountPaise, desc, qty, notes) {
                showUdharDialog = false
              }
            },
          )
        }

        if (showPaymentDialog) {
          val customersWithDue = customers.filter { it.outstandingBalancePaise > 0L }
          ReceivePaymentDialog(
            strings = strings,
            customersWithDue = customersWithDue,
            preselectedCustomer = paymentTargetCustomer?.takeIf { it.outstandingBalancePaise > 0L },
            isSubmitting = isSubmitting,
            isOnline = isOnline || viewModel.isDemoMode,
            onDismiss = { showPaymentDialog = false },
            onConfirmPayment = { customer, amountPaise, method, notes, paymentId ->
              viewModel.receivePayment(customer, amountPaise, method, notes, paymentId) {
                showPaymentDialog = false
              }
            },
          )
        }

        activeReceipt?.let { receipt ->
          PaymentReceiptDialog(
            strings = strings,
            language = language,
            shopName = shop.name,
            receipt = receipt,
            onDismiss = { activeReceipt = null },
          )
        }

        reversalTargetTx?.let { tx ->
          ReverseTransactionDialog(
            strings = strings,
            transaction = tx,
            isSubmitting = isSubmitting,
            onDismiss = { reversalTargetTx = null },
            onConfirmReversal = { reason ->
              viewModel.reverseTransaction(tx.id, reason) {
                reversalTargetTx = null
              }
            },
          )
        }
      }
    }
  }
}

private data class NavTabSpec(
  val tab: KhataTab,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val badge: Int = 0,
)

@Composable
private fun BadgedIcon(icon: ImageVector, label: String, badge: Int) {
  if (badge > 0) {
    BadgedBox(badge = { Badge { Text(if (badge > 9) "9+" else badge.toString()) } }) {
      Icon(imageVector = icon, contentDescription = label)
    }
  } else {
    Icon(imageVector = icon, contentDescription = label)
  }
}
