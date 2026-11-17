package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DemoKhataStore
import com.example.data.DuplicatePaymentException
import com.example.data.KhataLedgerStore
import com.example.data.LanguagePreferences
import com.example.localization.stringsFor
import com.example.model.ActivityLog
import com.example.model.AppLanguage
import com.example.model.Customer
import com.example.model.LedgerTransaction
import com.example.model.OwnerRole
import com.example.model.PaymentMethod
import com.example.model.PaymentRecord
import com.example.model.Shop
import com.example.model.ShopMember
import com.example.model.ShopNotification
import com.example.model.UiState
import com.example.util.ConnectivityMonitor
import com.example.util.CurrencyAndDateUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LedgerEvent {
  data class Message(val text: String, val isError: Boolean = false) : LedgerEvent
  data class PaymentReceiptReady(val receipt: PaymentRecord) : LedgerEvent
  data class NewCoOwnerNotification(val notification: ShopNotification) : LedgerEvent
}

data class DashboardStats(
  val totalCustomers: Int = 0,
  val totalOutstandingPaise: Long = 0L,
  val totalCreditPaise: Long = 0L,
  val totalPaidPaise: Long = 0L,
  val todayCreditPaise: Long = 0L,
  val todayPaymentPaise: Long = 0L,
  val todayTransactionCount: Int = 0,
  val monthCreditPaise: Long = 0L,
  val monthPaymentPaise: Long = 0L,
)

@OptIn(ExperimentalCoroutinesApi::class)
class KhataViewModel(
  private val repository: KhataLedgerStore,
  private val languagePrefs: LanguagePreferences,
  connectivityMonitor: ConnectivityMonitor,
  val currentUserId: String,
  val currentUserName: String,
  val currentUserEmail: String,
  val isDemoMode: Boolean = false,
  private val demoStore: DemoKhataStore? = null,
) : ViewModel() {

  private val _events = MutableSharedFlow<LedgerEvent>(extraBufferCapacity = 16)
  val events: SharedFlow<LedgerEvent> = _events.asSharedFlow()

  private val _isSubmitting = MutableStateFlow(false)
  val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

  private val seenNotificationIds = mutableSetOf<String>()
  private var initialNotifSnapshotHandled = false

  val activeDemoRole: StateFlow<OwnerRole> =
    demoStore?.activeRole ?: MutableStateFlow(OwnerRole.MAIN_ADMIN).asStateFlow()

  val activeOwnerId: StateFlow<String> =
    demoStore?.activeOwnerUid ?: MutableStateFlow(currentUserId).asStateFlow()

  val isCustomerPreview: StateFlow<Boolean> =
    demoStore?.isCustomerPreview ?: MutableStateFlow(false).asStateFlow()

  val previewCustomerId: StateFlow<String> =
    demoStore?.previewCustomerId ?: MutableStateFlow("").asStateFlow()

  val isDemoOwnerDeactivated: StateFlow<Boolean> =
    demoStore?.isCurrentActorDeactivated ?: MutableStateFlow(false).asStateFlow()

  val language: StateFlow<AppLanguage> =
    languagePrefs.languageFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = AppLanguage.EN,
    )

  val isOnline: StateFlow<Boolean> =
    connectivityMonitor.observeIsOnline().stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = connectivityMonitor.isCurrentlyOnline(),
    )

  init {
    if (!isDemoMode) {
      viewModelScope.launch {
        runCatching {
          repository.ensureUserProfile(
            name = currentUserName,
            email = currentUserEmail,
            language = language.value,
          )
        }
      }
    }
  }

  private fun <T> Flow<T>.toUiState(label: String): StateFlow<UiState<T>> =
    map<T, UiState<T>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, "Error observing $label", root)
        emit(UiState.Error(root.localizedMessage ?: "Failed to load $label"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading,
      )

  val userShopsState: StateFlow<UiState<List<Shop>>> =
    repository.observeUserShops(currentUserId).toUiState("shops")

  val pendingInvitesState: StateFlow<UiState<List<Shop>>> =
    repository.observePendingInvites(currentUserEmail).toUiState("invites")

  val activeShop: StateFlow<Shop?> =
    combine(userShopsState, languagePrefs.activeShopIdFlow) { shopsUi, preferredId ->
      val shops = (shopsUi as? UiState.Success)?.data.orEmpty()
      shops.firstOrNull { it.id == preferredId } ?: shops.firstOrNull()
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = null,
    )

  val membersState: StateFlow<UiState<List<ShopMember>>> =
    activeShop.flatMapLatest { shop ->
      if (shop == null) flowOf(emptyList())
      else repository.observeMembers(shop.id)
    }.toUiState("members")

  val customersState: StateFlow<UiState<List<Customer>>> =
    activeShop.flatMapLatest { shop ->
      if (shop == null) flowOf(emptyList())
      else repository.observeCustomers(shop.id)
    }.toUiState("customers")

  val transactionsState: StateFlow<UiState<List<LedgerTransaction>>> =
    activeShop.flatMapLatest { shop ->
      if (shop == null) flowOf(emptyList())
      else repository.observeTransactions(shop.id)
    }.toUiState("transactions")

  val paymentsState: StateFlow<UiState<List<PaymentRecord>>> =
    activeShop.flatMapLatest { shop ->
      if (shop == null) flowOf(emptyList())
      else repository.observePayments(shop.id)
    }.toUiState("payments")

  val notificationsState: StateFlow<UiState<List<ShopNotification>>> =
    activeShop.flatMapLatest { shop ->
      if (shop == null) flowOf(emptyList())
      else repository.observeNotifications(shop.id)
    }.map { list ->
      handleIncomingNotifications(list)
      list
    }.toUiState("notifications")

  val activityLogsState: StateFlow<UiState<List<ActivityLog>>> =
    activeShop.flatMapLatest { shop ->
      if (shop == null) flowOf(emptyList())
      else repository.observeActivityLogs(shop.id)
    }.toUiState("activity logs")

  val dashboardStats: StateFlow<DashboardStats> =
    combine(customersState, transactionsState) { custUi, txUi ->
      val customers = (custUi as? UiState.Success)?.data.orEmpty()
      val transactions = (txUi as? UiState.Success)?.data.orEmpty()

      val confirmedTx = transactions.filter { it.status == "CONFIRMED" }
      val todayCredits = confirmedTx.filter {
        it.type == "CREDIT" && CurrencyAndDateUtils.isToday(it.createdAt)
      }.sumOf { it.amountPaise }
      val todayPayments = confirmedTx.filter {
        it.type == "PAYMENT" && CurrencyAndDateUtils.isToday(it.createdAt)
      }.sumOf { it.amountPaise }
      val todayCount = transactions.count { CurrencyAndDateUtils.isToday(it.createdAt) }
      val monthCredits = confirmedTx.filter {
        it.type == "CREDIT" && CurrencyAndDateUtils.isThisMonth(it.createdAt)
      }.sumOf { it.amountPaise }
      val monthPayments = confirmedTx.filter {
        it.type == "PAYMENT" && CurrencyAndDateUtils.isThisMonth(it.createdAt)
      }.sumOf { it.amountPaise }

      DashboardStats(
        totalCustomers = customers.size,
        totalOutstandingPaise = customers.sumOf { it.outstandingBalancePaise },
        totalCreditPaise = customers.sumOf { it.openingBalancePaise + it.totalCreditPaise },
        totalPaidPaise = customers.sumOf { it.totalPaidPaise },
        todayCreditPaise = todayCredits,
        todayPaymentPaise = todayPayments,
        todayTransactionCount = todayCount,
        monthCreditPaise = monthCredits,
        monthPaymentPaise = monthPayments,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = DashboardStats(),
    )

  val unreadNotificationCount: StateFlow<Int> =
    combine(notificationsState, activeOwnerId) { ui, ownerId ->
      val list = (ui as? UiState.Success)?.data.orEmpty()
      list.count { it.actorUid != ownerId && !it.readByUids.contains(ownerId) }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = 0,
    )

  private fun handleIncomingNotifications(list: List<ShopNotification>) {
    if (!initialNotifSnapshotHandled) {
      seenNotificationIds.addAll(list.map { it.id })
      initialNotifSnapshotHandled = true
      return
    }
    val ownerId = activeOwnerId.value
    list.forEach { notif ->
      if (seenNotificationIds.add(notif.id) &&
        notif.actorUid != ownerId &&
        !notif.readByUids.contains(ownerId)
      ) {
        _events.tryEmit(LedgerEvent.NewCoOwnerNotification(notif))
      }
    }
  }

  fun switchDemoOwnerRole(role: OwnerRole) {
    demoStore?.switchDemoOwner(role)
  }

  fun setCustomerPreviewMode(enabled: Boolean) {
    demoStore?.setCustomerPreviewMode(enabled)
  }

  fun selectPreviewCustomer(customerId: String) {
    demoStore?.selectPreviewCustomer(customerId)
  }

  fun setLanguage(newLang: AppLanguage) {
    viewModelScope.launch {
      languagePrefs.setLanguage(newLang)
      if (!isDemoMode) {
        repository.updateUserLanguage(newLang)
      }
    }
  }

  fun selectActiveShop(shopId: String) {
    viewModelScope.launch {
      initialNotifSnapshotHandled = false
      seenNotificationIds.clear()
      languagePrefs.setActiveShopId(shopId)
    }
  }

  private fun resolvedActorName(): String {
    val ownerId = activeOwnerId.value
    val members = (membersState.value as? UiState.Success)?.data.orEmpty()
    return members.firstOrNull { it.uid == ownerId }?.name
      ?: currentUserName.ifBlank { currentUserEmail.substringBefore("@") }
  }

  fun createShop(shopName: String, ownerName: String, ownerPhone: String?) {
    if (_isSubmitting.value) return
    val s = stringsFor(language.value)
    viewModelScope.launch {
      _isSubmitting.value = true
      try {
        val shopId = repository.createShop(shopName, ownerName, currentUserEmail, ownerPhone)
        languagePrefs.setActiveShopId(shopId)
      } catch (e: Exception) {
        _events.emit(LedgerEvent.Message(e.localizedMessage ?: s.errorGeneric, isError = true))
      } finally {
        _isSubmitting.value = false
      }
    }
  }

  fun acceptInvite(shopId: String, ownerName: String, ownerPhone: String?) {
    if (_isSubmitting.value) return
    val s = stringsFor(language.value)
    viewModelScope.launch {
      _isSubmitting.value = true
      try {
        repository.acceptShopInvite(shopId, ownerName, currentUserEmail, ownerPhone)
        languagePrefs.setActiveShopId(shopId)
      } catch (e: Exception) {
        _events.emit(LedgerEvent.Message(e.localizedMessage ?: s.errorGeneric, isError = true))
      } finally {
        _isSubmitting.value = false
      }
    }
  }

  fun inviteCoOwner(email: String) {
    val shop = activeShop.value ?: return
    if (_isSubmitting.value) return
    val s = stringsFor(language.value)
    viewModelScope.launch {
      _isSubmitting.value = true
      try {
        repository.inviteOwner(shop.id, email, resolvedActorName())
        _events.emit(LedgerEvent.Message("Invitation sent to ${email.trim().lowercase()}"))
      } catch (e: Exception) {
        _events.emit(LedgerEvent.Message(e.localizedMessage ?: s.errorGeneric, isError = true))
      } finally {
        _isSubmitting.value = false
      }
    }
  }

  fun removeInvite(email: String) {
    val shop = activeShop.value ?: return
    viewModelScope.launch {
      runCatching { repository.removeInvite(shop.id, email) }
    }
  }

  fun toggleOwnerActive(member: ShopMember, activate: Boolean) {
    val shop = activeShop.value ?: return
    val s = stringsFor(language.value)
    viewModelScope.launch {
      runCatching {
        repository.toggleOwnerActive(shop.id, member, activate, resolvedActorName())
      }.onFailure { e ->
        _events.emit(LedgerEvent.Message(e.localizedMessage ?: s.errorGeneric, isError = true))
      }
    }
  }

  fun addCustomer(
    name: String,
    phone: String,
    address: String?,
    openingBalancePaise: Long,
    onSuccess: (String) -> Unit = {},
  ) {
    val shop = activeShop.value ?: return
    if (_isSubmitting.value) return
    val s = stringsFor(language.value)
    viewModelScope.launch {
      _isSubmitting.value = true
      try {
        val custId =
          repository.addCustomer(
            shopId = shop.id,
            name = name,
            phone = phone,
            address = address,
            openingBalancePaise = openingBalancePaise,
            actorName = resolvedActorName(),
          )
        _events.emit(LedgerEvent.Message(s.customerAddedSuccess(name.trim())))
        onSuccess(custId)
      } catch (e: Exception) {
        _events.emit(LedgerEvent.Message(e.localizedMessage ?: s.errorGeneric, isError = true))
      } finally {
        _isSubmitting.value = false
      }
    }
  }

  fun updateCustomer(
    customerId: String,
    newName: String,
    newPhone: String,
    newAddress: String?,
    onSuccess: () -> Unit = {},
  ) {
    val shop = activeShop.value ?: return
    if (_isSubmitting.value) return
    val s = stringsFor(language.value)
    viewModelScope.launch {
      _isSubmitting.value = true
      try {
        repository.updateCustomerDetails(
          shopId = shop.id,
          customerId = customerId,
          newName = newName,
          newPhone = newPhone,
          newAddress = newAddress,
          actorName = resolvedActorName(),
        )
        onSuccess()
      } catch (e: Exception) {
        _events.emit(LedgerEvent.Message(e.localizedMessage ?: s.errorGeneric, isError = true))
      } finally {
        _isSubmitting.value = false
      }
    }
  }

  fun addCredit(
    customer: Customer,
    amountPaise: Long,
    description: String,
    quantity: String?,
    notes: String?,
    clientTxId: String = "tx_${UUID.randomUUID().toString().replace("-", "")}",
    onSuccess: () -> Unit = {},
  ) {
    val shop = activeShop.value ?: return
    if (_isSubmitting.value) return
    val s = stringsFor(language.value)
    viewModelScope.launch {
      _isSubmitting.value = true
      try {
        repository.addCreditTransaction(
          shopId = shop.id,
          customerId = customer.id,
          amountPaise = amountPaise,
          description = description,
          quantity = quantity,
          notes = notes,
          actorName = resolvedActorName(),
          clientTxId = clientTxId,
        )
        _events.emit(
          LedgerEvent.Message(
            s.udharSuccessTemplate(customer.name, CurrencyAndDateUtils.formatRupees(amountPaise))
          )
        )
        onSuccess()
      } catch (e: Exception) {
        _events.emit(LedgerEvent.Message(e.localizedMessage ?: s.errorGeneric, isError = true))
      } finally {
        _isSubmitting.value = false
      }
    }
  }

  fun receivePayment(
    customer: Customer,
    amountPaise: Long,
    method: PaymentMethod,
    notes: String?,
    clientPaymentId: String,
    onSuccess: () -> Unit = {},
  ) {
    val shop = activeShop.value ?: return
    val s = stringsFor(language.value)
    if (!isDemoMode && !isOnline.value) {
      viewModelScope.launch {
        _events.emit(LedgerEvent.Message(s.offlineActionBlocked, isError = true))
      }
      return
    }
    if (_isSubmitting.value) return
    viewModelScope.launch {
      _isSubmitting.value = true
      try {
        val receipt =
          repository.receivePayment(
            shopId = shop.id,
            customerId = customer.id,
            amountPaise = amountPaise,
            method = method,
            notes = notes,
            actorName = resolvedActorName(),
            clientPaymentId = clientPaymentId,
          )
        val amtStr = CurrencyAndDateUtils.formatRupees(amountPaise)
        val remStr = CurrencyAndDateUtils.formatRupees(receipt.balanceAfterPaise ?: 0L)
        _events.emit(LedgerEvent.Message(s.paymentSuccessTemplate(customer.name, amtStr, remStr)))
        _events.emit(LedgerEvent.PaymentReceiptReady(receipt))
        onSuccess()
      } catch (_: DuplicatePaymentException) {
        _events.emit(LedgerEvent.Message(s.duplicatePaymentError, isError = true))
      } catch (e: Exception) {
        _events.emit(LedgerEvent.Message(e.localizedMessage ?: s.errorGeneric, isError = true))
      } finally {
        _isSubmitting.value = false
      }
    }
  }

  fun reverseTransaction(transactionId: String, reason: String, onSuccess: () -> Unit = {}) {
    val shop = activeShop.value ?: return
    if (_isSubmitting.value) return
    val s = stringsFor(language.value)
    viewModelScope.launch {
      _isSubmitting.value = true
      try {
        repository.reverseTransaction(
          shopId = shop.id,
          transactionId = transactionId,
          reason = reason,
          actorName = resolvedActorName(),
        )
        _events.emit(LedgerEvent.Message(s.reversalSuccess))
        onSuccess()
      } catch (e: Exception) {
        _events.emit(LedgerEvent.Message(e.localizedMessage ?: s.errorGeneric, isError = true))
      } finally {
        _isSubmitting.value = false
      }
    }
  }

  fun markNotificationRead(notificationId: String) {
    val shop = activeShop.value ?: return
    viewModelScope.launch {
      repository.markNotificationRead(shop.id, notificationId)
    }
  }

  fun markAllNotificationsRead(notifications: List<ShopNotification>) {
    val shop = activeShop.value ?: return
    val ownerId = activeOwnerId.value
    val unreadIds =
      notifications
        .filter { it.actorUid != ownerId && !it.readByUids.contains(ownerId) }
        .map { it.id }
    if (unreadIds.isEmpty()) return
    viewModelScope.launch {
      repository.markAllNotificationsRead(shop.id, unreadIds)
    }
  }

  private companion object {
    const val TAG = "KhataVM"
  }
}
