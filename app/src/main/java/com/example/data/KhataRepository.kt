package com.example.data

import android.content.Context
import com.example.R
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
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DuplicatePaymentException(message: String) : IllegalStateException(message)

class KhataRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) : KhataLedgerStore {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  private fun <T> observeQuery(
    path: String,
    query: Query,
    mapper: (com.google.firebase.firestore.QuerySnapshot) -> List<T>,
  ): Flow<List<T>> = flow {
    val uid = requireUserId()
    emitAll(
      query.snapshots().map(mapper).catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<com.google.firebase.firestore.FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
          handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
        throw error
      }
    )
  }

  override fun observeUserShops(uid: String): Flow<List<Shop>> =
    observeQuery(
      path = "shops",
      query = db.collection("shops").whereArrayContains("memberUids", uid),
    ) { snap ->
      snap.toObjects(Shop::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    }

  override fun observePendingInvites(email: String): Flow<List<Shop>> {
    val normalized = email.trim().lowercase(Locale.ROOT)
    if (normalized.isBlank()) return flow { emit(emptyList()) }
    return observeQuery(
      path = "shops",
      query = db.collection("shops").whereArrayContains("invitedEmails", normalized),
    ) { snap ->
      snap.toObjects(Shop::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    }
  }

  override fun observeShop(shopId: String): Flow<Shop?> = flow {
    val uid = requireUserId()
    val docRef = db.collection("shops").document(shopId)
    emitAll(
      docRef.snapshots().map { snap ->
        snap.toObject(Shop::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }.catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<com.google.firebase.firestore.FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
          handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
        }
        throw error
      }
    )
  }

  override fun observeMembers(shopId: String): Flow<List<ShopMember>> =
    observeQuery(
      path = "shops/$shopId/members",
      query = db.collection("shops").document(shopId).collection("members"),
    ) { snap ->
      snap.toObjects(ShopMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedBy { it.role }
    }

  override fun observeCustomers(shopId: String): Flow<List<Customer>> =
    observeQuery(
      path = "shops/$shopId/customers",
      query = db.collection("shops").document(shopId).collection("customers"),
    ) { snap ->
      snap.toObjects(Customer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedByDescending { it.updatedAt?.toDate()?.time ?: 0L }
    }

  override fun observeTransactions(shopId: String): Flow<List<LedgerTransaction>> =
    observeQuery(
      path = "shops/$shopId/transactions",
      query = db.collection("shops").document(shopId).collection("transactions"),
    ) { snap ->
      snap.toObjects(LedgerTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
    }

  override fun observePayments(shopId: String): Flow<List<PaymentRecord>> =
    observeQuery(
      path = "shops/$shopId/payments",
      query = db.collection("shops").document(shopId).collection("payments"),
    ) { snap ->
      snap.toObjects(PaymentRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
    }

  override fun observeNotifications(shopId: String): Flow<List<ShopNotification>> =
    observeQuery(
      path = "shops/$shopId/notifications",
      query = db.collection("shops").document(shopId).collection("notifications"),
    ) { snap ->
      snap.toObjects(ShopNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
    }

  override fun observeActivityLogs(shopId: String): Flow<List<ActivityLog>> =
    observeQuery(
      path = "shops/$shopId/activityLogs",
      query = db.collection("shops").document(shopId).collection("activityLogs"),
    ) { snap ->
      snap.toObjects(ActivityLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
    }

  override suspend fun ensureUserProfile(
    name: String,
    email: String,
    language: AppLanguage,
  ): UserProfile {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    return try {
      val existing = docRef.get().await()
      if (existing.exists()) {
        existing.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          ?: UserProfile(uid = uid, name = name, email = email, language = language.code)
      } else {
        val safeName = name.trim().ifBlank { email.substringBefore("@").ifBlank { "Shop Owner" } }
        val normalizedEmail = email.trim().lowercase(Locale.ROOT).ifBlank { "$uid@owner.local" }
        val payload = mapOf(
          "uid" to uid,
          "name" to safeName,
          "email" to normalizedEmail,
          "language" to language.code,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.set(payload).await()
        UserProfile(id = uid, uid = uid, name = safeName, email = normalizedEmail, language = language.code)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  override suspend fun updateUserLanguage(language: AppLanguage) {
    val uid = auth.currentUser?.uid ?: return
    val docRef = db.collection("users").document(uid)
    try {
      docRef.update(
        mapOf(
          "language" to language.code,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
    }
  }

  override suspend fun createShop(
    shopName: String,
    ownerName: String,
    ownerEmail: String,
    ownerPhone: String?,
  ): String {
    val uid = requireUserId()
    val cleanShopName = shopName.trim()
    require(cleanShopName.length >= 2) { "Shop name is too short" }
    val normalizedEmail = ownerEmail.trim().lowercase(Locale.ROOT).ifBlank { "$uid@owner.local" }
    val safeOwnerName = ownerName.trim().ifBlank { normalizedEmail.substringBefore("@") }

    val shopRef = db.collection("shops").document()
    val memberRef = shopRef.collection("members").document(uid)
    val logRef = shopRef.collection("activityLogs").document()

    val shopPayload = mapOf(
      "name" to cleanShopName,
      "mainOwnerUid" to uid,
      "mainOwnerEmail" to normalizedEmail,
      "memberUids" to listOf(uid),
      "invitedEmails" to emptyList<String>(),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    val memberPayload = buildMap<String, Any> {
      put("shopId", shopRef.id)
      put("uid", uid)
      put("name", safeOwnerName)
      put("email", normalizedEmail)
      if (!ownerPhone.isNullOrBlank()) put("phone", ownerPhone.trim())
      put("role", OwnerRole.MAIN_ADMIN.value)
      put("active", true)
      put("createdAt", FieldValue.serverTimestamp())
      put("updatedAt", FieldValue.serverTimestamp())
    }

    val logPayload = mapOf(
      "shopId" to shopRef.id,
      "action" to "SHOP_CREATED",
      "actorUid" to uid,
      "actorName" to safeOwnerName,
      "summaryEn" to "$safeOwnerName created shared ledger '$cleanShopName'.",
      "summaryTe" to "$safeOwnerName '$cleanShopName' ఉమ్మడి ఖాతాను ప్రారంభించారు.",
      "createdAt" to FieldValue.serverTimestamp(),
    )

    return try {
      db.runBatch { batch ->
        batch.set(shopRef, shopPayload)
        batch.set(memberRef, memberPayload)
        batch.set(logRef, logPayload)
      }.await()
      shopRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, shopRef.path)
      throw e
    }
  }

  override suspend fun inviteOwner(shopId: String, inviteEmail: String, actorName: String) {
    val uid = requireUserId()
    val normalizedEmail = inviteEmail.trim().lowercase(Locale.ROOT)
    require(normalizedEmail.contains("@") && normalizedEmail.contains(".")) { "Invalid email address" }

    val shopRef = db.collection("shops").document(shopId)
    val logRef = shopRef.collection("activityLogs").document()
    try {
      db.runTransaction { tx ->
        val snap = tx.get(shopRef)
        val shop = snap.toObject(Shop::class.java) ?: error("Shop not found")
        require(shop.mainOwnerUid == uid) { "Only Main Admin can invite owners" }
        val activeCount = shop.memberUids.size
        val invited = shop.invitedEmails.toMutableList()
        require(!invited.contains(normalizedEmail)) { "Email already invited" }
        require(activeCount + invited.size < 3) { "Maximum 3 authorized owners allowed" }
        invited.add(normalizedEmail)
        tx.update(
          shopRef,
          mapOf(
            "invitedEmails" to invited,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        tx.set(
          logRef,
          mapOf(
            "shopId" to shopId,
            "action" to "OWNER_INVITED",
            "actorUid" to uid,
            "actorName" to actorName,
            "summaryEn" to "$actorName invited $normalizedEmail as co-owner.",
            "summaryTe" to "$actorName సహ-ఓనర్‌గా $normalizedEmail ను ఆహ్వానించారు.",
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
      }.await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, shopRef.path)
      throw e
    }
  }

  override suspend fun removeInvite(shopId: String, inviteEmail: String) {
    val uid = requireUserId()
    val normalizedEmail = inviteEmail.trim().lowercase(Locale.ROOT)
    val shopRef = db.collection("shops").document(shopId)
    try {
      db.runTransaction { tx ->
        val shop = tx.get(shopRef).toObject(Shop::class.java) ?: error("Shop not found")
        require(shop.mainOwnerUid == uid) { "Only Main Admin can remove invites" }
        val updated = shop.invitedEmails.filterNot { it == normalizedEmail }
        tx.update(
          shopRef,
          mapOf(
            "invitedEmails" to updated,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }.await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, shopRef.path)
      throw e
    }
  }

  override suspend fun acceptShopInvite(
    shopId: String,
    memberName: String,
    memberEmail: String,
    memberPhone: String?,
  ) {
    val uid = requireUserId()
    val normalizedEmail = memberEmail.trim().lowercase(Locale.ROOT)
    val safeName = memberName.trim().ifBlank { normalizedEmail.substringBefore("@") }
    val shopRef = db.collection("shops").document(shopId)
    val memberRef = shopRef.collection("members").document(uid)
    val notifRef = shopRef.collection("notifications").document()
    val logRef = shopRef.collection("activityLogs").document()

    try {
      db.runTransaction { tx ->
        val shopSnap = tx.get(shopRef)
        val existingMemberSnap = tx.get(memberRef)
        val shop = shopSnap.toObject(Shop::class.java) ?: error("Shop not found")
        require(shop.invitedEmails.contains(normalizedEmail)) { "Invitation not found" }
        require(shop.memberUids.size < 3) { "Shop already has 3 owners" }
        require(!shop.memberUids.contains(uid)) { "Already a member" }

        val updatedMembers = shop.memberUids + uid
        val updatedInvites = shop.invitedEmails.filterNot { it == normalizedEmail }
        val assignedRole =
          if (shop.memberUids.size == 1) OwnerRole.OWNER_2.value else OwnerRole.OWNER_3.value

        tx.update(
          shopRef,
          mapOf(
            "memberUids" to updatedMembers,
            "invitedEmails" to updatedInvites,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )

        if (existingMemberSnap.exists()) {
          tx.update(
            memberRef,
            mapOf(
              "name" to safeName,
              "active" to true,
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        } else {
          val memberPayload = buildMap<String, Any> {
            put("shopId", shopId)
            put("uid", uid)
            put("name", safeName)
            put("email", normalizedEmail)
            if (!memberPhone.isNullOrBlank()) put("phone", memberPhone.trim())
            put("role", assignedRole)
            put("active", true)
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
          }
          tx.set(memberRef, memberPayload)
        }

        val msgEn = "$safeName joined the shared shop ledger."
        val msgTe = "$safeName ఉమ్మడి షాపు ఖాతాలో చేరారు."
        tx.set(
          notifRef,
          mapOf(
            "shopId" to shopId,
            "type" to "OWNER_JOINED",
            "actorUid" to uid,
            "actorName" to safeName,
            "messageEn" to msgEn,
            "messageTe" to msgTe,
            "readByUids" to listOf(uid),
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
        tx.set(
          logRef,
          mapOf(
            "shopId" to shopId,
            "action" to "OWNER_JOINED",
            "actorUid" to uid,
            "actorName" to safeName,
            "summaryEn" to msgEn,
            "summaryTe" to msgTe,
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
      }.await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, shopRef.path)
      throw e
    }
  }

  override suspend fun toggleOwnerActive(
    shopId: String,
    targetMember: ShopMember,
    activate: Boolean,
    actorName: String,
  ) {
    val uid = requireUserId()
    val shopRef = db.collection("shops").document(shopId)
    val memberRef = shopRef.collection("members").document(targetMember.uid)
    val notifRef = shopRef.collection("notifications").document()
    val logRef = shopRef.collection("activityLogs").document()

    try {
      db.runTransaction { tx ->
        val shop = tx.get(shopRef).toObject(Shop::class.java) ?: error("Shop not found")
        require(shop.mainOwnerUid == uid) { "Only Main Admin can manage owner access" }
        require(targetMember.uid != uid) { "Cannot deactivate Main Admin" }

        val updatedMemberUids =
          if (activate) {
            if (shop.memberUids.contains(targetMember.uid)) shop.memberUids
            else {
              require(shop.memberUids.size < 3) { "Shop already has 3 active owners" }
              shop.memberUids + targetMember.uid
            }
          } else {
            shop.memberUids.filterNot { it == targetMember.uid }
          }

        tx.update(
          shopRef,
          mapOf(
            "memberUids" to updatedMemberUids,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        tx.update(
          memberRef,
          mapOf(
            "active" to activate,
            "role" to targetMember.role,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )

        val msgEn =
          if (activate) "$actorName reactivated ${targetMember.name}'s access."
          else "$actorName deactivated ${targetMember.name}'s access."
        val msgTe =
          if (activate) "$actorName ${targetMember.name} యాక్సెస్‌ను మళ్ళీ ప్రారంభించారు."
          else "$actorName ${targetMember.name} యాక్సెస్‌ను నిలిపివేశారు."

        tx.set(
          notifRef,
          mapOf(
            "shopId" to shopId,
            "type" to "OWNER_UPDATED",
            "actorUid" to uid,
            "actorName" to actorName,
            "messageEn" to msgEn,
            "messageTe" to msgTe,
            "readByUids" to listOf(uid),
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
        tx.set(
          logRef,
          mapOf(
            "shopId" to shopId,
            "action" to if (activate) "OWNER_REACTIVATED" else "OWNER_DEACTIVATED",
            "actorUid" to uid,
            "actorName" to actorName,
            "summaryEn" to msgEn,
            "summaryTe" to msgTe,
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
      }.await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, shopRef.path)
      throw e
    }
  }

  override suspend fun addCustomer(
    shopId: String,
    name: String,
    phone: String,
    address: String?,
    openingBalancePaise: Long,
    actorName: String,
  ): String {
    val uid = requireUserId()
    val cleanName = name.trim()
    val cleanPhone = phone.trim()
    require(cleanName.isNotEmpty() && cleanPhone.length >= 5) { "Valid customer name and phone required" }
    require(openingBalancePaise >= 0L) { "Opening balance cannot be negative" }

    val shopRef = db.collection("shops").document(shopId)
    val custRef = shopRef.collection("customers").document()
    val notifRef = shopRef.collection("notifications").document()
    val logRef = shopRef.collection("activityLogs").document()

    val custPayload = buildMap<String, Any> {
      put("shopId", shopId)
      put("name", cleanName)
      put("phone", cleanPhone)
      if (!address.isNullOrBlank()) put("address", address.trim())
      put("openingBalancePaise", openingBalancePaise)
      put("totalCreditPaise", 0L)
      put("totalPaidPaise", 0L)
      put("outstandingBalancePaise", openingBalancePaise)
      put("createdByUid", uid)
      put("createdByName", actorName)
      put("createdAt", FieldValue.serverTimestamp())
      put("updatedAt", FieldValue.serverTimestamp())
    }

    val msgEn = "New Customer Added: $cleanName (by $actorName)."
    val msgTe = "కొత్త కస్టమర్ చేర్చబడ్డారు: $cleanName ($actorName)."

    return try {
      db.runBatch { batch ->
        batch.set(custRef, custPayload)
        batch.set(
          notifRef,
          mapOf(
            "shopId" to shopId,
            "type" to "CUSTOMER_ADDED",
            "actorUid" to uid,
            "actorName" to actorName,
            "customerId" to custRef.id,
            "customerName" to cleanName,
            "amountPaise" to openingBalancePaise,
            "balanceAfterPaise" to openingBalancePaise,
            "messageEn" to msgEn,
            "messageTe" to msgTe,
            "readByUids" to listOf(uid),
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
        batch.set(
          logRef,
          mapOf(
            "shopId" to shopId,
            "action" to "CUSTOMER_ADDED",
            "actorUid" to uid,
            "actorName" to actorName,
            "customerId" to custRef.id,
            "customerName" to cleanName,
            "amountPaise" to openingBalancePaise,
            "summaryEn" to msgEn,
            "summaryTe" to msgTe,
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
      }.await()
      custRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, custRef.path)
      throw e
    }
  }

  override suspend fun updateCustomerDetails(
    shopId: String,
    customerId: String,
    newName: String,
    newPhone: String,
    newAddress: String?,
    actorName: String,
  ) {
    val uid = requireUserId()
    val cleanName = newName.trim()
    val cleanPhone = newPhone.trim()
    require(cleanName.isNotEmpty() && cleanPhone.length >= 5) { "Valid customer name and phone required" }

    val shopRef = db.collection("shops").document(shopId)
    val custRef = shopRef.collection("customers").document(customerId)
    val notifRef = shopRef.collection("notifications").document()
    val logRef = shopRef.collection("activityLogs").document()

    val updateMap = mutableMapOf<String, Any>(
      "name" to cleanName,
      "phone" to cleanPhone,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    if (!newAddress.isNullOrBlank()) {
      updateMap["address"] = newAddress.trim()
    } else {
      updateMap["address"] = ""
    }

    val msgEn = "$actorName updated customer details for $cleanName."
    val msgTe = "$actorName $cleanName కస్టమర్ వివరాలు మార్చారు."

    try {
      db.runBatch { batch ->
        batch.update(custRef, updateMap)
        batch.set(
          notifRef,
          mapOf(
            "shopId" to shopId,
            "type" to "CUSTOMER_UPDATED",
            "actorUid" to uid,
            "actorName" to actorName,
            "customerId" to customerId,
            "customerName" to cleanName,
            "messageEn" to msgEn,
            "messageTe" to msgTe,
            "readByUids" to listOf(uid),
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
        batch.set(
          logRef,
          mapOf(
            "shopId" to shopId,
            "action" to "CUSTOMER_UPDATED",
            "actorUid" to uid,
            "actorName" to actorName,
            "customerId" to customerId,
            "customerName" to cleanName,
            "summaryEn" to msgEn,
            "summaryTe" to msgTe,
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
      }.await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, custRef.path)
      throw e
    }
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
  ) {
    val uid = requireUserId()
    require(amountPaise > 0L) { "Credit amount must be positive" }
    val cleanDesc = description.trim()
    require(cleanDesc.isNotEmpty()) { "Product or description is required" }

    val shopRef = db.collection("shops").document(shopId)
    val custRef = shopRef.collection("customers").document(customerId)
    val txRef = shopRef.collection("transactions").document(clientTxId)
    val notifRef = shopRef.collection("notifications").document()
    val logRef = shopRef.collection("activityLogs").document()

    try {
      db.runTransaction { tx ->
        val existingTx = tx.get(txRef)
        if (existingTx.exists()) {
          return@runTransaction
        }
        val custSnap = tx.get(custRef)
        val customer = custSnap.toObject(Customer::class.java) ?: error("Customer not found")

        val newTotalCredit = customer.totalCreditPaise + amountPaise
        val newOutstanding = customer.openingBalancePaise + newTotalCredit - customer.totalPaidPaise

        val txPayload = buildMap<String, Any> {
          put("shopId", shopId)
          put("customerId", customerId)
          put("customerName", customer.name)
          put("type", TransactionType.CREDIT.value)
          put("amountPaise", amountPaise)
          put("description", cleanDesc)
          if (!quantity.isNullOrBlank()) put("quantity", quantity.trim())
          if (!notes.isNullOrBlank()) put("notes", notes.trim())
          put("balanceAfterPaise", newOutstanding)
          put("status", RecordStatus.CONFIRMED.value)
          put("createdByUid", uid)
          put("createdByName", actorName)
          put("createdAt", FieldValue.serverTimestamp())
        }

        val amountStr = CurrencyAndDateUtils.formatRupees(amountPaise)
        val balStr = CurrencyAndDateUtils.formatRupees(newOutstanding)
        val msgEn = "$actorName added $amountStr Udhar for ${customer.name}. Balance: $balStr."
        val msgTe = "$actorName ${customer.name} గారికి $amountStr అప్పు నమోదు చేశారు. మొత్తం బాకీ: $balStr."

        tx.set(txRef, txPayload)
        tx.update(
          custRef,
          mapOf(
            "totalCreditPaise" to newTotalCredit,
            "outstandingBalancePaise" to newOutstanding,
            "lastTransactionId" to txRef.id,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        tx.set(
          notifRef,
          mapOf(
            "shopId" to shopId,
            "type" to "CREDIT_ADDED",
            "actorUid" to uid,
            "actorName" to actorName,
            "customerId" to customerId,
            "customerName" to customer.name,
            "amountPaise" to amountPaise,
            "balanceAfterPaise" to newOutstanding,
            "messageEn" to msgEn,
            "messageTe" to msgTe,
            "readByUids" to listOf(uid),
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
        tx.set(
          logRef,
          mapOf(
            "shopId" to shopId,
            "action" to "CREDIT_ADDED",
            "actorUid" to uid,
            "actorName" to actorName,
            "customerId" to customerId,
            "customerName" to customer.name,
            "amountPaise" to amountPaise,
            "summaryEn" to msgEn,
            "summaryTe" to msgTe,
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
      }.await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, txRef.path)
      throw e
    }
  }

  override suspend fun receivePayment(
    shopId: String,
    customerId: String,
    amountPaise: Long,
    method: PaymentMethod,
    notes: String?,
    actorName: String,
    clientPaymentId: String,
  ): PaymentRecord {
    val uid = requireUserId()
    require(amountPaise > 0L) { "Payment amount must be positive" }

    val shopRef = db.collection("shops").document(shopId)
    val custRef = shopRef.collection("customers").document(customerId)
    val payRef = shopRef.collection("payments").document(clientPaymentId)
    val txRef = shopRef.collection("transactions").document("tx_$clientPaymentId")
    val notifRef = shopRef.collection("notifications").document()
    val logRef = shopRef.collection("activityLogs").document()

    return try {
      db.runTransaction { tx ->
        val existingPay = tx.get(payRef)
        if (existingPay.exists()) {
          throw DuplicatePaymentException("Duplicate payment submission prevented")
        }
        val custSnap = tx.get(custRef)
        val customer = custSnap.toObject(Customer::class.java) ?: error("Customer not found")

        val balanceBefore = customer.outstandingBalancePaise
        require(amountPaise <= balanceBefore) {
          "Payment exceeds outstanding balance"
        }
        val newTotalPaid = customer.totalPaidPaise + amountPaise
        val newOutstanding = customer.openingBalancePaise + customer.totalCreditPaise - newTotalPaid
        val receiptNo = "KK-${(System.currentTimeMillis() % 1000000L).toString().padStart(6, '0')}"

        val paymentPayload = buildMap<String, Any> {
          put("shopId", shopId)
          put("customerId", customerId)
          put("customerName", customer.name)
          put("amountPaise", amountPaise)
          put("method", method.value)
          put("receiptNumber", receiptNo)
          if (!notes.isNullOrBlank()) put("notes", notes.trim())
          put("transactionId", txRef.id)
          put("balanceBeforePaise", balanceBefore)
          put("balanceAfterPaise", newOutstanding)
          put("status", RecordStatus.CONFIRMED.value)
          put("receivedByUid", uid)
          put("receivedByName", actorName)
          put("createdAt", FieldValue.serverTimestamp())
        }

        val txPayload = buildMap<String, Any> {
          put("shopId", shopId)
          put("customerId", customerId)
          put("customerName", customer.name)
          put("type", TransactionType.PAYMENT.value)
          put("amountPaise", amountPaise)
          put("description", "Payment Received (${method.value}) • $receiptNo")
          if (!notes.isNullOrBlank()) put("notes", notes.trim())
          put("paymentMethod", method.value)
          put("paymentId", payRef.id)
          put("balanceAfterPaise", newOutstanding)
          put("status", RecordStatus.CONFIRMED.value)
          put("createdByUid", uid)
          put("createdByName", actorName)
          put("createdAt", FieldValue.serverTimestamp())
        }

        val amountStr = CurrencyAndDateUtils.formatRupees(amountPaise)
        val balStr = CurrencyAndDateUtils.formatRupees(newOutstanding)
        val msgEn = "Payment Received: ${customer.name} paid $amountStr (${method.value}). Updated Balance: $balStr."
        val msgTe = "చెల్లింపు నమోదు: ${customer.name} గారు $amountStr చెల్లించారు (${method.value}). మిగిలిన బాకీ: $balStr."

        tx.set(payRef, paymentPayload)
        tx.set(txRef, txPayload)
        tx.update(
          custRef,
          mapOf(
            "totalPaidPaise" to newTotalPaid,
            "outstandingBalancePaise" to newOutstanding,
            "lastTransactionId" to txRef.id,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        tx.set(
          notifRef,
          mapOf(
            "shopId" to shopId,
            "type" to "PAYMENT_RECEIVED",
            "actorUid" to uid,
            "actorName" to actorName,
            "customerId" to customerId,
            "customerName" to customer.name,
            "amountPaise" to amountPaise,
            "balanceAfterPaise" to newOutstanding,
            "messageEn" to msgEn,
            "messageTe" to msgTe,
            "readByUids" to listOf(uid),
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
        tx.set(
          logRef,
          mapOf(
            "shopId" to shopId,
            "action" to "PAYMENT_RECEIVED",
            "actorUid" to uid,
            "actorName" to actorName,
            "customerId" to customerId,
            "customerName" to customer.name,
            "amountPaise" to amountPaise,
            "summaryEn" to msgEn,
            "summaryTe" to msgTe,
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )

        PaymentRecord(
          id = payRef.id,
          shopId = shopId,
          customerId = customerId,
          customerName = customer.name,
          amountPaise = amountPaise,
          method = method.value,
          receiptNumber = receiptNo,
          notes = notes?.trim()?.takeIf { it.isNotEmpty() },
          transactionId = txRef.id,
          balanceBeforePaise = balanceBefore,
          balanceAfterPaise = newOutstanding,
          status = RecordStatus.CONFIRMED.value,
          receivedByUid = uid,
          receivedByName = actorName,
        )
      }.await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, payRef.path)
      throw e
    }
  }

  override suspend fun reverseTransaction(
    shopId: String,
    transactionId: String,
    reason: String,
    actorName: String,
  ) {
    val uid = requireUserId()
    val cleanReason = reason.trim()
    require(cleanReason.isNotEmpty()) { "Reversal reason is required" }

    val shopRef = db.collection("shops").document(shopId)
    val txRef = shopRef.collection("transactions").document(transactionId)
    val revTxRef = shopRef.collection("transactions").document()
    val notifRef = shopRef.collection("notifications").document()
    val logRef = shopRef.collection("activityLogs").document()

    try {
      db.runTransaction { tx ->
        val origSnap = tx.get(txRef)
        val origTx = origSnap.toObject(LedgerTransaction::class.java) ?: error("Transaction not found")
        require(origTx.status == RecordStatus.CONFIRMED.value) { "Transaction already reversed" }
        require(origTx.type == TransactionType.CREDIT.value || origTx.type == TransactionType.PAYMENT.value) {
          "Cannot reverse a reversal record"
        }

        val custRef = shopRef.collection("customers").document(origTx.customerId)
        val customer = tx.get(custRef).toObject(Customer::class.java) ?: error("Customer not found")

        val paymentRef =
          origTx.paymentId?.takeIf { it.isNotBlank() }?.let { shopRef.collection("payments").document(it) }
        val paymentSnap = paymentRef?.let { tx.get(it) }

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
        val newOutstanding = customer.openingBalancePaise + newTotalCredit - newTotalPaid

        tx.update(
          txRef,
          mapOf(
            "status" to RecordStatus.REVERSED.value,
            "reversedByUid" to uid,
            "reversedByName" to actorName,
            "reversalReason" to cleanReason,
            "reversedAt" to FieldValue.serverTimestamp(),
          ),
        )

        if (paymentRef != null && paymentSnap?.exists() == true) {
          tx.update(
            paymentRef,
            mapOf(
              "status" to RecordStatus.REVERSED.value,
              "reversedByUid" to uid,
              "reversedByName" to actorName,
              "reversalReason" to cleanReason,
              "reversedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }

        tx.set(
          revTxRef,
          mapOf(
            "shopId" to shopId,
            "customerId" to origTx.customerId,
            "customerName" to customer.name,
            "type" to TransactionType.REVERSAL.value,
            "amountPaise" to origTx.amountPaise,
            "description" to "Reversal (${origTx.type}): $cleanReason",
            "relatedTransactionId" to transactionId,
            "balanceAfterPaise" to newOutstanding,
            "status" to RecordStatus.CONFIRMED.value,
            "createdByUid" to uid,
            "createdByName" to actorName,
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )

        tx.update(
          custRef,
          mapOf(
            "totalCreditPaise" to newTotalCredit,
            "totalPaidPaise" to newTotalPaid,
            "outstandingBalancePaise" to newOutstanding,
            "lastTransactionId" to revTxRef.id,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )

        val amtStr = CurrencyAndDateUtils.formatRupees(origTx.amountPaise)
        val balStr = CurrencyAndDateUtils.formatRupees(newOutstanding)
        val msgEn = "$actorName reversed $amtStr ${origTx.type.lowercase()} for ${customer.name} ($cleanReason). Balance: $balStr."
        val msgTe = "$actorName ${customer.name} గారి $amtStr నమోదును రివర్స్ చేశారు ($cleanReason). బాకీ: $balStr."

        tx.set(
          notifRef,
          mapOf(
            "shopId" to shopId,
            "type" to if (origTx.type == TransactionType.PAYMENT.value) "PAYMENT_REVERSED" else "CREDIT_REVERSED",
            "actorUid" to uid,
            "actorName" to actorName,
            "customerId" to origTx.customerId,
            "customerName" to customer.name,
            "amountPaise" to origTx.amountPaise,
            "balanceAfterPaise" to newOutstanding,
            "messageEn" to msgEn,
            "messageTe" to msgTe,
            "readByUids" to listOf(uid),
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
        tx.set(
          logRef,
          mapOf(
            "shopId" to shopId,
            "action" to "TRANSACTION_REVERSED",
            "actorUid" to uid,
            "actorName" to actorName,
            "customerId" to origTx.customerId,
            "customerName" to customer.name,
            "amountPaise" to origTx.amountPaise,
            "summaryEn" to msgEn,
            "summaryTe" to msgTe,
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
      }.await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, revTxRef.path)
      throw e
    }
  }

  override suspend fun markNotificationRead(shopId: String, notificationId: String) {
    val uid = requireUserId()
    val notifRef = db.collection("shops").document(shopId).collection("notifications").document(notificationId)
    try {
      notifRef.update("readByUids", FieldValue.arrayUnion(uid)).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, notifRef.path)
    }
  }

  override suspend fun markAllNotificationsRead(shopId: String, notificationIds: List<String>) {
    val uid = requireUserId()
    if (notificationIds.isEmpty()) return
    try {
      db.runBatch { batch ->
        notificationIds.take(400).forEach { id ->
          val ref = db.collection("shops").document(shopId).collection("notifications").document(id)
          batch.update(ref, "readByUids", FieldValue.arrayUnion(uid))
        }
      }.await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "shops/$shopId/notifications")
    }
  }
}
