package com.example.data

import com.example.model.ActivityLog
import com.example.model.AppLanguage
import com.example.model.Customer
import com.example.model.LedgerTransaction
import com.example.model.PaymentMethod
import com.example.model.PaymentRecord
import com.example.model.Shop
import com.example.model.ShopMember
import com.example.model.ShopNotification
import com.example.model.UserProfile
import java.util.UUID
import kotlinx.coroutines.flow.Flow

interface KhataLedgerStore {
  fun observeUserShops(uid: String): Flow<List<Shop>>
  fun observePendingInvites(email: String): Flow<List<Shop>>
  fun observeShop(shopId: String): Flow<Shop?>
  fun observeMembers(shopId: String): Flow<List<ShopMember>>
  fun observeCustomers(shopId: String): Flow<List<Customer>>
  fun observeTransactions(shopId: String): Flow<List<LedgerTransaction>>
  fun observePayments(shopId: String): Flow<List<PaymentRecord>>
  fun observeNotifications(shopId: String): Flow<List<ShopNotification>>
  fun observeActivityLogs(shopId: String): Flow<List<ActivityLog>>

  suspend fun ensureUserProfile(
    name: String,
    email: String,
    language: AppLanguage,
  ): UserProfile

  suspend fun updateUserLanguage(language: AppLanguage)

  suspend fun createShop(
    shopName: String,
    ownerName: String,
    ownerEmail: String,
    ownerPhone: String?,
  ): String

  suspend fun inviteOwner(shopId: String, inviteEmail: String, actorName: String)
  suspend fun removeInvite(shopId: String, inviteEmail: String)

  suspend fun acceptShopInvite(
    shopId: String,
    memberName: String,
    memberEmail: String,
    memberPhone: String?,
  )

  suspend fun toggleOwnerActive(
    shopId: String,
    targetMember: ShopMember,
    activate: Boolean,
    actorName: String,
  )

  suspend fun addCustomer(
    shopId: String,
    name: String,
    phone: String,
    address: String?,
    openingBalancePaise: Long,
    actorName: String,
  ): String

  suspend fun updateCustomerDetails(
    shopId: String,
    customerId: String,
    newName: String,
    newPhone: String,
    newAddress: String?,
    actorName: String,
  )

  suspend fun addCreditTransaction(
    shopId: String,
    customerId: String,
    amountPaise: Long,
    description: String,
    quantity: String?,
    notes: String?,
    actorName: String,
    clientTxId: String = "tx_${UUID.randomUUID().toString().replace("-", "")}",
  )

  suspend fun receivePayment(
    shopId: String,
    customerId: String,
    amountPaise: Long,
    method: PaymentMethod,
    notes: String?,
    actorName: String,
    clientPaymentId: String = "pay_${UUID.randomUUID().toString().replace("-", "")}",
  ): PaymentRecord

  suspend fun reverseTransaction(
    shopId: String,
    transactionId: String,
    reason: String,
    actorName: String,
  )

  suspend fun markNotificationRead(shopId: String, notificationId: String)
  suspend fun markAllNotificationsRead(shopId: String, notificationIds: List<String>)
}
