package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AppLanguage(val code: String, val displayName: String) {
  EN("en", "English"),
  TE("te", "తెలుగు");

  companion object {
    fun fromCode(code: String?): AppLanguage =
      entries.firstOrNull { it.code == code } ?: EN
  }
}

enum class OwnerRole(val value: String) {
  MAIN_ADMIN("MAIN_ADMIN"),
  OWNER_2("OWNER_2"),
  OWNER_3("OWNER_3");

  companion object {
    fun fromValue(value: String?): OwnerRole =
      entries.firstOrNull { it.value == value } ?: OWNER_2
  }
}

enum class TransactionType(val value: String) {
  CREDIT("CREDIT"),
  PAYMENT("PAYMENT"),
  REVERSAL("REVERSAL");

  companion object {
    fun fromValue(value: String?): TransactionType =
      entries.firstOrNull { it.value == value } ?: CREDIT
  }
}

enum class PaymentMethod(val value: String) {
  CASH("CASH"),
  UPI("UPI"),
  OTHER("OTHER");

  companion object {
    fun fromValue(value: String?): PaymentMethod =
      entries.firstOrNull { it.value == value } ?: CASH
  }
}

enum class RecordStatus(val value: String) {
  CONFIRMED("CONFIRMED"),
  REVERSED("REVERSED");

  companion object {
    fun fromValue(value: String?): RecordStatus =
      entries.firstOrNull { it.value == value } ?: CONFIRMED
  }
}

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val uid: String = "",
  val name: String = "",
  val email: String = "",
  val language: String = AppLanguage.EN.code,
  val activeShopId: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Shop(
  @DocumentId val id: String = "",
  val name: String = "",
  val mainOwnerUid: String = "",
  val mainOwnerEmail: String = "",
  val memberUids: List<String> = emptyList(),
  val invitedEmails: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ShopMember(
  @DocumentId val id: String = "",
  val shopId: String = "",
  val uid: String = "",
  val name: String = "",
  val email: String = "",
  val phone: String? = null,
  val role: String = OwnerRole.OWNER_2.value,
  val active: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Customer(
  @DocumentId val id: String = "",
  val shopId: String = "",
  val name: String = "",
  val phone: String = "",
  val address: String? = null,
  val openingBalancePaise: Long = 0L,
  val totalCreditPaise: Long = 0L,
  val totalPaidPaise: Long = 0L,
  val outstandingBalancePaise: Long = 0L,
  val lastTransactionId: String? = null,
  val createdByUid: String = "",
  val createdByName: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class LedgerTransaction(
  @DocumentId val id: String = "",
  val shopId: String = "",
  val customerId: String = "",
  val customerName: String = "",
  val type: String = TransactionType.CREDIT.value,
  val amountPaise: Long = 0L,
  val description: String = "",
  val quantity: String? = null,
  val notes: String? = null,
  val paymentMethod: String? = null,
  val paymentId: String? = null,
  val relatedTransactionId: String? = null,
  val balanceAfterPaise: Long = 0L,
  val status: String = RecordStatus.CONFIRMED.value,
  val createdByUid: String = "",
  val createdByName: String = "",
  val reversedByUid: String? = null,
  val reversedByName: String? = null,
  val reversalReason: String? = null,
  @ServerTimestamp val reversedAt: Timestamp? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class PaymentRecord(
  @DocumentId val id: String = "",
  val shopId: String = "",
  val customerId: String = "",
  val customerName: String = "",
  val amountPaise: Long = 0L,
  val method: String = PaymentMethod.CASH.value,
  val receiptNumber: String = "",
  val notes: String? = null,
  val transactionId: String? = null,
  val balanceBeforePaise: Long? = null,
  val balanceAfterPaise: Long? = null,
  val status: String = RecordStatus.CONFIRMED.value,
  val receivedByUid: String = "",
  val receivedByName: String = "",
  val reversedByUid: String? = null,
  val reversedByName: String? = null,
  val reversalReason: String? = null,
  @ServerTimestamp val reversedAt: Timestamp? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ShopNotification(
  @DocumentId val id: String = "",
  val shopId: String = "",
  val type: String = "",
  val actorUid: String = "",
  val actorName: String = "",
  val customerId: String? = null,
  val customerName: String? = null,
  val amountPaise: Long? = null,
  val balanceAfterPaise: Long? = null,
  val messageEn: String = "",
  val messageTe: String = "",
  val readByUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ActivityLog(
  @DocumentId val id: String = "",
  val shopId: String = "",
  val action: String = "",
  val actorUid: String = "",
  val actorName: String = "",
  val customerId: String? = null,
  val customerName: String? = null,
  val amountPaise: Long? = null,
  val summaryEn: String = "",
  val summaryTe: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)
