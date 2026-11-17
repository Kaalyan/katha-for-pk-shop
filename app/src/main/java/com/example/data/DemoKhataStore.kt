package com.example.data

import com.example.model.ActivityLog
import com.example.model.AppLanguage
import com.example.model.Customer
import com.example.model.LedgerTransaction
import com.example.model.OwnerRole
import com.example.model.PaymentMethod
import com.example.model.PaymentRecord
import com.example.model.RecordStatus
import com.example.model.Shop
import com.example.model.ShopMember
import com.example.model.ShopNotification
import com.example.model.TransactionType
import com.example.model.UserProfile
import com.example.util.CurrencyAndDateUtils
import com.google.firebase.Timestamp
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DemoKhataStore(
  initialCustomerView: Boolean = false,
) : KhataLedgerStore {

  private val nowMs = System.currentTimeMillis()
  private fun tsHoursAgo(hours: Long): Timestamp = Timestamp(Date(nowMs - hours * 3_600_000L))
  private fun tsDaysAgo(days: Long): Timestamp = Timestamp(Date(nowMs - days * 86_400_000L))

  private val initialMembers = listOf(
    ShopMember(
      id = OWNER_1_UID,
      shopId = DEMO_SHOP_ID,
      uid = OWNER_1_UID,
      name = "Ramesh (Main Admin)",
      email = "ramesh.kirana@demo.local",
      phone = "9848001101",
      role = OwnerRole.MAIN_ADMIN.value,
      active = true,
      createdAt = tsDaysAgo(30),
      updatedAt = tsDaysAgo(1),
    ),
    ShopMember(
      id = OWNER_2_UID,
      shopId = DEMO_SHOP_ID,
      uid = OWNER_2_UID,
      name = "Kiran (Owner 2)",
      email = "kiran.kirana@demo.local",
      phone = "9848002202",
      role = OwnerRole.OWNER_2.value,
      active = true,
      createdAt = tsDaysAgo(25),
      updatedAt = tsDaysAgo(1),
    ),
    ShopMember(
      id = OWNER_3_UID,
      shopId = DEMO_SHOP_ID,
      uid = OWNER_3_UID,
      name = "Prakash (Owner 3)",
      email = "prakash.kirana@demo.local",
      phone = "9848003303",
      role = OwnerRole.OWNER_3.value,
      active = true,
      createdAt = tsDaysAgo(20),
      updatedAt = tsDaysAgo(1),
    ),
  )

  private val _activeRole = MutableStateFlow(OwnerRole.MAIN_ADMIN)
  val activeRole = _activeRole.asStateFlow()

  private val _activeOwnerUid = MutableStateFlow(OWNER_1_UID)
  val activeOwnerUid = _activeOwnerUid.asStateFlow()

  private val _isCustomerPreview = MutableStateFlow(initialCustomerView)
  val isCustomerPreview = _isCustomerPreview.asStateFlow()

  private val _previewCustomerId = MutableStateFlow("demo_cust_ramesh")
  val previewCustomerId = _previewCustomerId.asStateFlow()

  private val _isCurrentActorDeactivated = MutableStateFlow(false)
  val isCurrentActorDeactivated = _isCurrentActorDeactivated.asStateFlow()

  private val ledgerMutex = Mutex()

  private val _shop = MutableStateFlow(
    Shop(
      id = DEMO_SHOP_ID,
      name = "Sri Lakshmi Kirana (Demo)",
      mainOwnerUid = OWNER_1_UID,
      mainOwnerEmail = "ramesh.kirana@demo.local",
      memberUids = listOf(OWNER_1_UID, OWNER_2_UID, OWNER_3_UID),
      invitedEmails = emptyList(),
      createdAt = tsDaysAgo(30),
      updatedAt = Timestamp.now(),
    )
  )

  private val _members = MutableStateFlow(initialMembers)

  private val _customers = MutableStateFlow(
    listOf(
      Customer(
        id = "demo_cust_ramesh",
        shopId = DEMO_SHOP_ID,
        name = "Ramesh",
        phone = "9848011223",
        address = "Main Bazaar, Plot 12",
        openingBalancePaise = 50_000L,
        totalCreditPaise = 175_000L,
        totalPaidPaise = 100_000L,
        outstandingBalancePaise = 125_000L,
        lastTransactionId = "demo_tx_2",
        createdByUid = OWNER_1_UID,
        createdByName = "Ramesh (Main Admin)",
        createdAt = tsDaysAgo(12),
        updatedAt = tsHoursAgo(2),
      ),
      Customer(
        id = "demo_cust_suresh",
        shopId = DEMO_SHOP_ID,
        name = "Suresh",
        phone = "9849044556",
        address = "Gandhi Nagar",
        openingBalancePaise = 0L,
        totalCreditPaise = 240_000L,
        totalPaidPaise = 80_000L,
        outstandingBalancePaise = 160_000L,
        lastTransactionId = "demo_tx_4",
        createdByUid = OWNER_2_UID,
        createdByName = "Kiran (Owner 2)",
        createdAt = tsDaysAgo(10),
        updatedAt = tsHoursAgo(4),
      ),
      Customer(
        id = "demo_cust_lakshmi",
        shopId = DEMO_SHOP_ID,
        name = "Lakshmi",
        phone = "9989177889",
        address = "Temple Street",
        openingBalancePaise = 20_000L,
        totalCreditPaise = 95_000L,
        totalPaidPaise = 115_000L,
        outstandingBalancePaise = 0L,
        lastTransactionId = "demo_tx_6",
        createdByUid = OWNER_1_UID,
        createdByName = "Ramesh (Main Admin)",
        createdAt = tsDaysAgo(8),
        updatedAt = tsDaysAgo(1),
      ),
      Customer(
        id = "demo_cust_venkatesh",
        shopId = DEMO_SHOP_ID,
        name = "Venkatesh",
        phone = "9701233445",
        address = "Station Road",
        openingBalancePaise = 0L,
        totalCreditPaise = 65_000L,
        totalPaidPaise = 20_000L,
        outstandingBalancePaise = 45_000L,
        lastTransactionId = "demo_tx_7",
        createdByUid = OWNER_3_UID,
        createdByName = "Prakash (Owner 3)",
        createdAt = tsDaysAgo(5),
        updatedAt = tsDaysAgo(2),
      ),
    )
  )

  private val _payments = MutableStateFlow(
    listOf(
      PaymentRecord(
        id = "demo_pay_1",
        shopId = DEMO_SHOP_ID,
        customerId = "demo_cust_ramesh",
        customerName = "Ramesh",
        amountPaise = 100_000L,
        method = PaymentMethod.CASH.value,
        receiptNumber = "KK-104201",
        notes = "Part payment received at counter",
        transactionId = "demo_tx_2",
        balanceBeforePaise = 225_000L,
        balanceAfterPaise = 125_000L,
        status = RecordStatus.CONFIRMED.value,
        receivedByUid = OWNER_2_UID,
        receivedByName = "Kiran (Owner 2)",
        createdAt = tsHoursAgo(2),
      ),
      PaymentRecord(
        id = "demo_pay_2",
        shopId = DEMO_SHOP_ID,
        customerId = "demo_cust_suresh",
        customerName = "Suresh",
        amountPaise = 80_000L,
        method = PaymentMethod.UPI.value,
        receiptNumber = "KK-104198",
        notes = "PhonePe QR confirmed manually",
        transactionId = "demo_tx_4",
        balanceBeforePaise = 240_000L,
        balanceAfterPaise = 160_000L,
        status = RecordStatus.CONFIRMED.value,
        receivedByUid = OWNER_1_UID,
        receivedByName = "Ramesh (Main Admin)",
        createdAt = tsHoursAgo(4),
      ),
      PaymentRecord(
        id = "demo_pay_3",
        shopId = DEMO_SHOP_ID,
        customerId = "demo_cust_lakshmi",
        customerName = "Lakshmi",
        amountPaise = 115_000L,
        method = PaymentMethod.CASH.value,
        receiptNumber = "KK-104175",
        notes = "Full monthly settlement",
        transactionId = "demo_tx_6",
        balanceBeforePaise = 115_000L,
        balanceAfterPaise = 0L,
        status = RecordStatus.CONFIRMED.value,
        receivedByUid = OWNER_3_UID,
        receivedByName = "Prakash (Owner 3)",
        createdAt = tsDaysAgo(1),
      ),
      PaymentRecord(
        id = "demo_pay_4",
        shopId = DEMO_SHOP_ID,
        customerId = "demo_cust_venkatesh",
        customerName = "Venkatesh",
        amountPaise = 20_000L,
        method = PaymentMethod.CASH.value,
        receiptNumber = "KK-104140",
        notes = "Advance cash payment",
        transactionId = "demo_tx_7",
        balanceBeforePaise = 65_000L,
        balanceAfterPaise = 45_000L,
        status = RecordStatus.CONFIRMED.value,
        receivedByUid = OWNER_3_UID,
        receivedByName = "Prakash (Owner 3)",
        createdAt = tsDaysAgo(2),
      ),
    )
  )

  private val _transactions = MutableStateFlow(
    listOf(
      LedgerTransaction(
        id = "demo_tx_2",
        shopId = DEMO_SHOP_ID,
        customerId = "demo_cust_ramesh",
        customerName = "Ramesh",
        type = TransactionType.PAYMENT.value,
        amountPaise = 100_000L,
        description = "Payment Received (CASH) • KK-104201",
        paymentMethod = PaymentMethod.CASH.value,
        paymentId = "demo_pay_1",
        balanceAfterPaise = 125_000L,
        status = RecordStatus.CONFIRMED.value,
        createdByUid = OWNER_2_UID,
        createdByName = "Kiran (Owner 2)",
        createdAt = tsHoursAgo(2),
      ),
      LedgerTransaction(
        id = "demo_tx_1",
        shopId = DEMO_SHOP_ID,
        customerId = "demo_cust_ramesh",
        customerName = "Ramesh",
        type = TransactionType.CREDIT.value,
        amountPaise = 175_000L,
        description = "Sona Masoori Rice 25kg & Sunflower Oil",
        quantity = "1 bag + 2L",
        notes = "Monthly ration items",
        balanceAfterPaise = 225_000L,
        status = RecordStatus.CONFIRMED.value,
        createdByUid = OWNER_1_UID,
        createdByName = "Ramesh (Main Admin)",
        createdAt = tsHoursAgo(5),
      ),
      LedgerTransaction(
        id = "demo_tx_4",
        shopId = DEMO_SHOP_ID,
        customerId = "demo_cust_suresh",
        customerName = "Suresh",
        type = TransactionType.PAYMENT.value,
        amountPaise = 80_000L,
        description = "Payment Received (UPI) • KK-104198",
        paymentMethod = PaymentMethod.UPI.value,
        paymentId = "demo_pay_2",
        balanceAfterPaise = 160_000L,
        status = RecordStatus.CONFIRMED.value,
        createdByUid = OWNER_1_UID,
        createdByName = "Ramesh (Main Admin)",
        createdAt = tsHoursAgo(4),
      ),
      LedgerTransaction(
        id = "demo_tx_3",
        shopId = DEMO_SHOP_ID,
        customerId = "demo_cust_suresh",
        customerName = "Suresh",
        type = TransactionType.CREDIT.value,
        amountPaise = 240_000L,
        description = "Toor Dal, Sugar, Tea Powder & Spices",
        quantity = "Bulk grocery",
        balanceAfterPaise = 240_000L,
        status = RecordStatus.CONFIRMED.value,
        createdByUid = OWNER_2_UID,
        createdByName = "Kiran (Owner 2)",
        createdAt = tsDaysAgo(1),
      ),
      LedgerTransaction(
        id = "demo_tx_6",
        shopId = DEMO_SHOP_ID,
        customerId = "demo_cust_lakshmi",
        customerName = "Lakshmi",
        type = TransactionType.PAYMENT.value,
        amountPaise = 115_000L,
        description = "Payment Received (CASH) • KK-104175",
        paymentMethod = PaymentMethod.CASH.value,
        paymentId = "demo_pay_3",
        balanceAfterPaise = 0L,
        status = RecordStatus.CONFIRMED.value,
        createdByUid = OWNER_3_UID,
        createdByName = "Prakash (Owner 3)",
        createdAt = tsDaysAgo(1),
      ),
      LedgerTransaction(
        id = "demo_tx_5",
        shopId = DEMO_SHOP_ID,
        customerId = "demo_cust_lakshmi",
        customerName = "Lakshmi",
        type = TransactionType.CREDIT.value,
        amountPaise = 95_000L,
        description = "Atta 10kg, Groundnut Oil & Soap",
        quantity = "4 items",
        balanceAfterPaise = 115_000L,
        status = RecordStatus.CONFIRMED.value,
        createdByUid = OWNER_1_UID,
        createdByName = "Ramesh (Main Admin)",
        createdAt = tsDaysAgo(3),
      ),
      LedgerTransaction(
        id = "demo_tx_7",
        shopId = DEMO_SHOP_ID,
        customerId = "demo_cust_venkatesh",
        customerName = "Venkatesh",
        type = TransactionType.PAYMENT.value,
        amountPaise = 20_000L,
        description = "Payment Received (CASH) • KK-104140",
        paymentMethod = PaymentMethod.CASH.value,
        paymentId = "demo_pay_4",
        balanceAfterPaise = 45_000L,
        status = RecordStatus.CONFIRMED.value,
        createdByUid = OWNER_3_UID,
        createdByName = "Prakash (Owner 3)",
        createdAt = tsDaysAgo(2),
      ),
      LedgerTransaction(
        id = "demo_tx_8",
        shopId = DEMO_SHOP_ID,
        customerId = "demo_cust_venkatesh",
        customerName = "Venkatesh",
        type = TransactionType.CREDIT.value,
        amountPaise = 65_000L,
        description = "Milk Packets, Biscuits & Coffee",
        quantity = "Weekly items",
        balanceAfterPaise = 65_000L,
        status = RecordStatus.CONFIRMED.value,
        createdByUid = OWNER_3_UID,
        createdByName = "Prakash (Owner 3)",
        createdAt = tsDaysAgo(4),
      ),
    )
  )

  private val _notifications = MutableStateFlow(
    listOf(
      ShopNotification(
        id = "demo_notif_1",
        shopId = DEMO_SHOP_ID,
        type = "PAYMENT_RECEIVED",
        actorUid = OWNER_2_UID,
        actorName = "Kiran (Owner 2)",
        customerId = "demo_cust_ramesh",
        customerName = "Ramesh",
        amountPaise = 100_000L,
        balanceAfterPaise = 125_000L,
        messageEn = "Payment Received: Ramesh paid ₹1,000 (CASH). Updated Balance: ₹1,250.",
        messageTe = "చెల్లింపు నమోదు: Ramesh గారు ₹1,000 చెల్లించారు (CASH). మిగిలిన బాకీ: ₹1,250.",
        readByUids = listOf(OWNER_2_UID),
        createdAt = tsHoursAgo(2),
      ),
      ShopNotification(
        id = "demo_notif_2",
        shopId = DEMO_SHOP_ID,
        type = "CREDIT_ADDED",
        actorUid = OWNER_2_UID,
        actorName = "Kiran (Owner 2)",
        customerId = "demo_cust_suresh",
        customerName = "Suresh",
        amountPaise = 240_000L,
        balanceAfterPaise = 240_000L,
        messageEn = "Kiran (Owner 2) added ₹2,400 Udhar for Suresh. Balance: ₹2,400.",
        messageTe = "Kiran (Owner 2) Suresh గారికి ₹2,400 అప్పు నమోదు చేశారు. మొత్తం బాకీ: ₹2,400.",
        readByUids = listOf(OWNER_2_UID),
        createdAt = tsDaysAgo(1),
      ),
      ShopNotification(
        id = "demo_notif_3",
        shopId = DEMO_SHOP_ID,
        type = "PAYMENT_RECEIVED",
        actorUid = OWNER_3_UID,
        actorName = "Prakash (Owner 3)",
        customerId = "demo_cust_lakshmi",
        customerName = "Lakshmi",
        amountPaise = 115_000L,
        balanceAfterPaise = 0L,
        messageEn = "Payment Received: Lakshmi paid ₹1,150 (CASH). Updated Balance: ₹0.",
        messageTe = "చెల్లింపు నమోదు: Lakshmi గారు ₹1,150 చెల్లించారు (CASH). మిగిలిన బాకీ: ₹0.",
        readByUids = listOf(OWNER_3_UID, OWNER_1_UID),
        createdAt = tsDaysAgo(1),
      ),
    )
  )

  private val _activityLogs = MutableStateFlow(
    listOf(
      ActivityLog(
        id = "demo_log_1",
        shopId = DEMO_SHOP_ID,
        action = "PAYMENT_RECEIVED",
        actorUid = OWNER_2_UID,
        actorName = "Kiran (Owner 2)",
        customerId = "demo_cust_ramesh",
        customerName = "Ramesh",
        amountPaise = 100_000L,
        summaryEn = "Payment Received: Ramesh paid ₹1,000 (CASH). Updated Balance: ₹1,250.",
        summaryTe = "చెల్లింపు నమోదు: Ramesh గారు ₹1,000 చెల్లించారు (CASH). మిగిలిన బాకీ: ₹1,250.",
        createdAt = tsHoursAgo(2),
      ),
      ActivityLog(
        id = "demo_log_2",
        shopId = DEMO_SHOP_ID,
        action = "PAYMENT_RECEIVED",
        actorUid = OWNER_1_UID,
        actorName = "Ramesh (Main Admin)",
        customerId = "demo_cust_suresh",
        customerName = "Suresh",
        amountPaise = 80_000L,
        summaryEn = "Payment Received: Suresh paid ₹800 (UPI). Updated Balance: ₹1,600.",
        summaryTe = "చెల్లింపు నమోదు: Suresh గారు ₹800 చెల్లించారు (UPI). మిగిలిన బాకీ: ₹1,600.",
        createdAt = tsHoursAgo(4),
      ),
      ActivityLog(
        id = "demo_log_3",
        shopId = DEMO_SHOP_ID,
        action = "CREDIT_ADDED",
        actorUid = OWNER_1_UID,
        actorName = "Ramesh (Main Admin)",
        customerId = "demo_cust_ramesh",
        customerName = "Ramesh",
        amountPaise = 175_000L,
        summaryEn = "Ramesh (Main Admin) added ₹1,750 Udhar for Ramesh. Balance: ₹2,250.",
        summaryTe = "Ramesh (Main Admin) Ramesh గారికి ₹1,750 అప్పు నమోదు చేశారు. మొత్తం బాకీ: ₹2,250.",
        createdAt = tsHoursAgo(5),
      ),
    )
  )

  fun switchDemoOwner(role: OwnerRole) {
    _isCustomerPreview.value = false
    _activeRole.value = role
    _activeOwnerUid.value = uidForRole(role)
    refreshActorDeactivatedState()
  }

  fun setCustomerPreviewMode(enabled: Boolean) {
    _isCustomerPreview.value = enabled
  }

  fun selectPreviewCustomer(customerId: String) {
    _previewCustomerId.value = customerId
  }

  private fun refreshActorDeactivatedState() {
    val actor = currentActor()
    _isCurrentActorDeactivated.value = !actor.active
  }

  private fun requireActiveActor(): ShopMember {
    val actor = currentActor()
    require(actor.active) { "Owner access has been deactivated by Main Admin" }
    return actor
  }

  fun currentActor(): ShopMember {
    val targetUid = _activeOwnerUid.value
    return _members.value.firstOrNull { it.uid == targetUid } ?: _members.value.first()
  }

  private fun uidForRole(role: OwnerRole): String =
    when (role) {
      OwnerRole.MAIN_ADMIN -> OWNER_1_UID
      OwnerRole.OWNER_2 -> OWNER_2_UID
      OwnerRole.OWNER_3 -> OWNER_3_UID
    }

  override fun observeUserShops(uid: String): Flow<List<Shop>> = _shop.map { listOf(it) }
  override fun observePendingInvites(email: String): Flow<List<Shop>> = flowOf(emptyList())
  override fun observeShop(shopId: String): Flow<Shop?> = _shop
  override fun observeMembers(shopId: String): Flow<List<ShopMember>> = _members
  override fun observeCustomers(shopId: String): Flow<List<Customer>> = _customers
  override fun observeTransactions(shopId: String): Flow<List<LedgerTransaction>> = _transactions
  override fun observePayments(shopId: String): Flow<List<PaymentRecord>> = _payments
  override fun observeNotifications(shopId: String): Flow<List<ShopNotification>> = _notifications
  override fun observeActivityLogs(shopId: String): Flow<List<ActivityLog>> = _activityLogs

  override suspend fun ensureUserProfile(
    name: String,
    email: String,
    language: AppLanguage,
  ): UserProfile {
    val actor = currentActor()
    return UserProfile(
      id = actor.uid,
      uid = actor.uid,
      name = actor.name,
      email = actor.email,
      language = language.code,
      activeShopId = DEMO_SHOP_ID,
    )
  }

  override suspend fun updateUserLanguage(language: AppLanguage) {
    // Local demo mode persists language via DataStore only.
  }

  override suspend fun createShop(
    shopName: String,
    ownerName: String,
    ownerEmail: String,
    ownerPhone: String?,
  ): String = DEMO_SHOP_ID

  override suspend fun inviteOwner(shopId: String, inviteEmail: String, actorName: String) {
    val actor = requireActiveActor()
    val normalized = inviteEmail.trim().lowercase(Locale.ROOT)
    require(normalized.contains("@") && normalized.contains(".")) { "Invalid email address" }
    val currentShop = _shop.value
    require(currentShop.memberUids.size + currentShop.invitedEmails.size < 3) {
      "Maximum 3 authorized owners allowed"
    }
    require(!currentShop.invitedEmails.contains(normalized)) { "Email already invited" }
    _shop.value = currentShop.copy(invitedEmails = currentShop.invitedEmails + normalized)
    val summaryEn = "${actor.name} invited $normalized as co-owner."
    val summaryTe = "${actor.name} సహ-ఓనర్‌గా $normalized ను ఆహ్వానించారు."
    _activityLogs.value =
      listOf(
        ActivityLog(
          id = "demo_log_${UUID.randomUUID().toString().take(8)}",
          shopId = shopId,
          action = "OWNER_INVITED",
          actorUid = actor.uid,
          actorName = actor.name,
          summaryEn = summaryEn,
          summaryTe = summaryTe,
          createdAt = Timestamp.now(),
        )
      ) + _activityLogs.value
  }

  override suspend fun removeInvite(shopId: String, inviteEmail: String) {
    requireActiveActor()
    val normalized = inviteEmail.trim().lowercase(Locale.ROOT)
    val currentShop = _shop.value
    _shop.value = currentShop.copy(invitedEmails = currentShop.invitedEmails.filterNot { it == normalized })
  }

  override suspend fun acceptShopInvite(
    shopId: String,
    memberName: String,
    memberEmail: String,
    memberPhone: String?,
  ) {
    // Not applicable inside pre-seeded 3-owner demo mode.
  }

  override suspend fun toggleOwnerActive(
    shopId: String,
    targetMember: ShopMember,
    activate: Boolean,
    actorName: String,
  ) {
    val actor = currentActor()
    require(actor.uid == _shop.value.mainOwnerUid) { "Only Main Admin can manage owner access" }
    require(targetMember.uid != actor.uid) { "Cannot deactivate Main Admin" }

    _members.value = _members.value.map {
      if (it.uid == targetMember.uid) it.copy(active = activate, updatedAt = Timestamp.now()) else it
    }
    val updatedUids =
      if (activate) (_shop.value.memberUids + targetMember.uid).distinct()
      else _shop.value.memberUids.filterNot { it == targetMember.uid }
    _shop.value = _shop.value.copy(memberUids = updatedUids, updatedAt = Timestamp.now())
    refreshActorDeactivatedState()

    val msgEn =
      if (activate) "${actor.name} reactivated ${targetMember.name}'s access."
      else "${actor.name} deactivated ${targetMember.name}'s access."
    val msgTe =
      if (activate) "${actor.name} ${targetMember.name} యాక్సెస్‌ను మళ్ళీ ప్రారంభించారు."
      else "${actor.name} ${targetMember.name} యాక్సెస్‌ను నిలిపివేశారు."

    appendNotificationAndLog(
      shopId = shopId,
      type = "OWNER_UPDATED",
      action = if (activate) "OWNER_REACTIVATED" else "OWNER_DEACTIVATED",
      actor = actor,
      msgEn = msgEn,
      msgTe = msgTe,
    )
  }

  override suspend fun addCustomer(
    shopId: String,
    name: String,
    phone: String,
    address: String?,
    openingBalancePaise: Long,
    actorName: String,
  ): String {
    val cleanName = name.trim()
    val cleanPhone = phone.trim()
    require(cleanName.isNotEmpty() && cleanPhone.length >= 5) { "Valid customer name and phone required" }
    require(openingBalancePaise >= 0L) { "Opening balance cannot be negative" }

    val actor = requireActiveActor()
    val custId = "demo_cust_${UUID.randomUUID().toString().take(8)}"
    val now = Timestamp.now()
    val customer = Customer(
      id = custId,
      shopId = shopId,
      name = cleanName,
      phone = cleanPhone,
      address = address?.trim()?.takeIf { it.isNotEmpty() },
      openingBalancePaise = openingBalancePaise,
      totalCreditPaise = 0L,
      totalPaidPaise = 0L,
      outstandingBalancePaise = openingBalancePaise,
      createdByUid = actor.uid,
      createdByName = actor.name,
      createdAt = now,
      updatedAt = now,
    )
    _customers.value = listOf(customer) + _customers.value

    val msgEn = "New Customer Added: $cleanName (by ${actor.name})."
    val msgTe = "కొత్త కస్టమర్ చేర్చబడ్డారు: $cleanName (${actor.name})."
    appendNotificationAndLog(
      shopId = shopId,
      type = "CUSTOMER_ADDED",
      action = "CUSTOMER_ADDED",
      actor = actor,
      customerId = custId,
      customerName = cleanName,
      amountPaise = openingBalancePaise,
      balanceAfterPaise = openingBalancePaise,
      msgEn = msgEn,
      msgTe = msgTe,
    )
    return custId
  }

  override suspend fun updateCustomerDetails(
    shopId: String,
    customerId: String,
    newName: String,
    newPhone: String,
    newAddress: String?,
    actorName: String,
  ) {
    val cleanName = newName.trim()
    val cleanPhone = newPhone.trim()
    require(cleanName.isNotEmpty() && cleanPhone.length >= 5) { "Valid customer name and phone required" }

    val actor = requireActiveActor()
    val now = Timestamp.now()
    _customers.value = _customers.value.map { c ->
      if (c.id == customerId) {
        c.copy(
          name = cleanName,
          phone = cleanPhone,
          address = newAddress?.trim()?.takeIf { it.isNotEmpty() },
          updatedAt = now,
        )
      } else c
    }

    val msgEn = "${actor.name} updated customer details for $cleanName."
    val msgTe = "${actor.name} $cleanName కస్టమర్ వివరాలు మార్చారు."
    appendNotificationAndLog(
      shopId = shopId,
      type = "CUSTOMER_UPDATED",
      action = "CUSTOMER_UPDATED",
      actor = actor,
      customerId = customerId,
      customerName = cleanName,
      msgEn = msgEn,
      msgTe = msgTe,
    )
  }

  override suspend fun addCreditTransaction(
    shopId: String,
    customerId: String,
    amountPaise: Long,
    description: String,
    quantity: String?,
    notes: String?,
    actorName: String,
    clientTxId: String,
  ) = ledgerMutex.withLock {
    require(amountPaise > 0L) { "Credit amount must be positive" }
    val cleanDesc = description.trim()
    require(cleanDesc.isNotEmpty()) { "Product or description is required" }

    if (_transactions.value.any { it.id == clientTxId }) return@withLock
    val actor = requireActiveActor()
    val customer = _customers.value.firstOrNull { it.id == customerId } ?: error("Customer not found")
    val now = Timestamp.now()

    val newTotalCredit = customer.totalCreditPaise + amountPaise
    val newOutstanding = customer.openingBalancePaise + newTotalCredit - customer.totalPaidPaise

    val tx = LedgerTransaction(
      id = clientTxId,
      shopId = shopId,
      customerId = customerId,
      customerName = customer.name,
      type = TransactionType.CREDIT.value,
      amountPaise = amountPaise,
      description = cleanDesc,
      quantity = quantity?.trim()?.takeIf { it.isNotEmpty() },
      notes = notes?.trim()?.takeIf { it.isNotEmpty() },
      balanceAfterPaise = newOutstanding,
      status = RecordStatus.CONFIRMED.value,
      createdByUid = actor.uid,
      createdByName = actor.name,
      createdAt = now,
    )
    _transactions.value = listOf(tx) + _transactions.value
    _customers.value = _customers.value.map { c ->
      if (c.id == customerId) {
        c.copy(
          totalCreditPaise = newTotalCredit,
          outstandingBalancePaise = newOutstanding,
          lastTransactionId = clientTxId,
          updatedAt = now,
        )
      } else c
    }

    val amountStr = CurrencyAndDateUtils.formatRupees(amountPaise)
    val balStr = CurrencyAndDateUtils.formatRupees(newOutstanding)
    val msgEn = "${actor.name} added $amountStr Udhar for ${customer.name}. Balance: $balStr."
    val msgTe = "${actor.name} ${customer.name} గారికి $amountStr అప్పు నమోదు చేశారు. మొత్తం బాకీ: $balStr."
    appendNotificationAndLog(
      shopId = shopId,
      type = "CREDIT_ADDED",
      action = "CREDIT_ADDED",
      actor = actor,
      customerId = customerId,
      customerName = customer.name,
      amountPaise = amountPaise,
      balanceAfterPaise = newOutstanding,
      msgEn = msgEn,
      msgTe = msgTe,
    )
  }

  override suspend fun receivePayment(
    shopId: String,
    customerId: String,
    amountPaise: Long,
    method: PaymentMethod,
    notes: String?,
    actorName: String,
    clientPaymentId: String,
  ): PaymentRecord = ledgerMutex.withLock {
    require(amountPaise > 0L) { "Payment amount must be positive" }
    if (_payments.value.any { it.id == clientPaymentId }) {
      throw DuplicatePaymentException("Duplicate payment submission prevented")
    }
    val actor = requireActiveActor()
    val customer = _customers.value.firstOrNull { it.id == customerId } ?: error("Customer not found")
    val balanceBefore = customer.outstandingBalancePaise
    require(amountPaise <= balanceBefore) { "Payment exceeds outstanding balance" }
    val now = Timestamp.now()
    val newTotalPaid = customer.totalPaidPaise + amountPaise
    val newOutstanding = customer.openingBalancePaise + customer.totalCreditPaise - newTotalPaid
    val receiptNo = "KK-${(System.currentTimeMillis() % 1_000_000L).toString().padStart(6, '0')}"
    val txId = "tx_$clientPaymentId"

    val payment = PaymentRecord(
      id = clientPaymentId,
      shopId = shopId,
      customerId = customerId,
      customerName = customer.name,
      amountPaise = amountPaise,
      method = method.value,
      receiptNumber = receiptNo,
      notes = notes?.trim()?.takeIf { it.isNotEmpty() },
      transactionId = txId,
      balanceBeforePaise = balanceBefore,
      balanceAfterPaise = newOutstanding,
      status = RecordStatus.CONFIRMED.value,
      receivedByUid = actor.uid,
      receivedByName = actor.name,
      createdAt = now,
    )
    val tx = LedgerTransaction(
      id = txId,
      shopId = shopId,
      customerId = customerId,
      customerName = customer.name,
      type = TransactionType.PAYMENT.value,
      amountPaise = amountPaise,
      description = "Payment Received (${method.value}) • $receiptNo",
      notes = notes?.trim()?.takeIf { it.isNotEmpty() },
      paymentMethod = method.value,
      paymentId = clientPaymentId,
      balanceAfterPaise = newOutstanding,
      status = RecordStatus.CONFIRMED.value,
      createdByUid = actor.uid,
      createdByName = actor.name,
      createdAt = now,
    )

    _payments.value = listOf(payment) + _payments.value
    _transactions.value = listOf(tx) + _transactions.value
    _customers.value = _customers.value.map { c ->
      if (c.id == customerId) {
        c.copy(
          totalPaidPaise = newTotalPaid,
          outstandingBalancePaise = newOutstanding,
          lastTransactionId = txId,
          updatedAt = now,
        )
      } else c
    }

    val amountStr = CurrencyAndDateUtils.formatRupees(amountPaise)
    val balStr = CurrencyAndDateUtils.formatRupees(newOutstanding)
    val msgEn = "Payment Received: ${customer.name} paid $amountStr (${method.value}). Updated Balance: $balStr."
    val msgTe = "చెల్లింపు నమోదు: ${customer.name} గారు $amountStr చెల్లించారు (${method.value}). మిగిలిన బాకీ: $balStr."
    appendNotificationAndLog(
      shopId = shopId,
      type = "PAYMENT_RECEIVED",
      action = "PAYMENT_RECEIVED",
      actor = actor,
      customerId = customerId,
      customerName = customer.name,
      amountPaise = amountPaise,
      balanceAfterPaise = newOutstanding,
      msgEn = msgEn,
      msgTe = msgTe,
    )
    return@withLock payment
  }

  override suspend fun reverseTransaction(
    shopId: String,
    transactionId: String,
    reason: String,
    actorName: String,
  ) {
    val cleanReason = reason.trim()
    require(cleanReason.isNotEmpty()) { "Reversal reason is required" }

    val origTx = _transactions.value.firstOrNull { it.id == transactionId } ?: error("Transaction not found")
    require(origTx.status == RecordStatus.CONFIRMED.value) { "Transaction already reversed" }
    require(origTx.type == TransactionType.CREDIT.value || origTx.type == TransactionType.PAYMENT.value) {
      "Cannot reverse a reversal record"
    }
    val customer = _customers.value.firstOrNull { it.id == origTx.customerId } ?: error("Customer not found")

    val newTotalCredit: Long
    val newTotalPaid: Long
    if (origTx.type == TransactionType.PAYMENT.value) {
      newTotalCredit = customer.totalCreditPaise
      newTotalPaid = (customer.totalPaidPaise - origTx.amountPaise).coerceAtLeast(0L)
    } else {
      newTotalCredit = customer.totalCreditPaise - origTx.amountPaise
      newTotalPaid = customer.totalPaidPaise
      require(newTotalCredit >= 0L && (customer.openingBalancePaise + newTotalCredit - newTotalPaid) >= 0L) {
        "Cannot reverse credit below already settled payments"
      }
    }
    val actor = requireActiveActor()
    val newOutstanding = customer.openingBalancePaise + newTotalCredit - newTotalPaid
    val now = Timestamp.now()
    val revTxId = "demo_rev_${UUID.randomUUID().toString().take(8)}"

    val revTx = LedgerTransaction(
      id = revTxId,
      shopId = shopId,
      customerId = origTx.customerId,
      customerName = customer.name,
      type = TransactionType.REVERSAL.value,
      amountPaise = origTx.amountPaise,
      description = "Reversal (${origTx.type}): $cleanReason",
      relatedTransactionId = transactionId,
      balanceAfterPaise = newOutstanding,
      status = RecordStatus.CONFIRMED.value,
      createdByUid = actor.uid,
      createdByName = actor.name,
      createdAt = now,
    )

    _transactions.value =
      listOf(revTx) +
        _transactions.value.map { tx ->
          if (tx.id == transactionId) {
            tx.copy(
              status = RecordStatus.REVERSED.value,
              reversedByUid = actor.uid,
              reversedByName = actor.name,
              reversalReason = cleanReason,
              reversedAt = now,
            )
          } else tx
        }

    origTx.paymentId?.let { payId ->
      _payments.value = _payments.value.map { p ->
        if (p.id == payId) {
          p.copy(
            status = RecordStatus.REVERSED.value,
            reversedByUid = actor.uid,
            reversedByName = actor.name,
            reversalReason = cleanReason,
            reversedAt = now,
          )
        } else p
      }
    }

    _customers.value = _customers.value.map { c ->
      if (c.id == customer.id) {
        c.copy(
          totalCreditPaise = newTotalCredit,
          totalPaidPaise = newTotalPaid,
          outstandingBalancePaise = newOutstanding,
          lastTransactionId = revTxId,
          updatedAt = now,
        )
      } else c
    }

    val amtStr = CurrencyAndDateUtils.formatRupees(origTx.amountPaise)
    val balStr = CurrencyAndDateUtils.formatRupees(newOutstanding)
    val msgEn = "${actor.name} reversed $amtStr ${origTx.type.lowercase()} for ${customer.name} ($cleanReason). Balance: $balStr."
    val msgTe = "${actor.name} ${customer.name} గారి $amtStr నమోదును రివర్స్ చేశారు ($cleanReason). బాకీ: $balStr."
    appendNotificationAndLog(
      shopId = shopId,
      type = if (origTx.type == TransactionType.PAYMENT.value) "PAYMENT_REVERSED" else "CREDIT_REVERSED",
      action = "TRANSACTION_REVERSED",
      actor = actor,
      customerId = customer.id,
      customerName = customer.name,
      amountPaise = origTx.amountPaise,
      balanceAfterPaise = newOutstanding,
      msgEn = msgEn,
      msgTe = msgTe,
    )
  }

  override suspend fun markNotificationRead(shopId: String, notificationId: String) {
    val uid = _activeOwnerUid.value
    _notifications.value = _notifications.value.map { n ->
      if (n.id == notificationId && !n.readByUids.contains(uid)) {
        n.copy(readByUids = n.readByUids + uid)
      } else n
    }
  }

  override suspend fun markAllNotificationsRead(shopId: String, notificationIds: List<String>) {
    val uid = _activeOwnerUid.value
    val idSet = notificationIds.toSet()
    _notifications.value = _notifications.value.map { n ->
      if (n.id in idSet && !n.readByUids.contains(uid)) {
        n.copy(readByUids = n.readByUids + uid)
      } else n
    }
  }

  private fun appendNotificationAndLog(
    shopId: String,
    type: String,
    action: String,
    actor: ShopMember,
    customerId: String? = null,
    customerName: String? = null,
    amountPaise: Long? = null,
    balanceAfterPaise: Long? = null,
    msgEn: String,
    msgTe: String,
  ) {
    val now = Timestamp.now()
    val notif = ShopNotification(
      id = "demo_notif_${UUID.randomUUID().toString().take(8)}",
      shopId = shopId,
      type = type,
      actorUid = actor.uid,
      actorName = actor.name,
      customerId = customerId,
      customerName = customerName,
      amountPaise = amountPaise,
      balanceAfterPaise = balanceAfterPaise,
      messageEn = msgEn,
      messageTe = msgTe,
      readByUids = listOf(actor.uid),
      createdAt = now,
    )
    val log = ActivityLog(
      id = "demo_log_${UUID.randomUUID().toString().take(8)}",
      shopId = shopId,
      action = action,
      actorUid = actor.uid,
      actorName = actor.name,
      customerId = customerId,
      customerName = customerName,
      amountPaise = amountPaise,
      summaryEn = msgEn,
      summaryTe = msgTe,
      createdAt = now,
    )
    _notifications.value = listOf(notif) + _notifications.value
    _activityLogs.value = listOf(log) + _activityLogs.value
  }

  companion object {
    const val DEMO_SHOP_ID = "demo_shop_1"
    const val OWNER_1_UID = "demo_owner_1"
    const val OWNER_2_UID = "demo_owner_2"
    const val OWNER_3_UID = "demo_owner_3"
  }
}
