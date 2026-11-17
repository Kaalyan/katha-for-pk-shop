package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.DuplicatePaymentException
import com.example.data.KhataRepository
import com.example.model.PaymentMethod
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class KhataRepositoryRuleTest : FirestoreEmulatorTestBase() {

  private fun uniqueEmail(prefix: String): String =
    "${prefix}_${UUID.randomUUID().toString().take(8)}@kiranatest.com"

  @Test
  fun fullThreeOwnerAccountingLifecycle_andSecurityIsolation() = runBlocking {
    val owner1Email = uniqueEmail("owner1")
    val owner2Email = uniqueEmail("owner2")
    val owner3Email = uniqueEmail("owner3")
    val strangerEmail = uniqueEmail("stranger")

    // 1. Unauthenticated caller must fail with PERMISSION_DENIED at Firestore rules and IllegalStateException in repository
    auth.signOut()
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        firestore.collection("shops").document("any_shop").collection("customers").get().await()
      }
      fail("Expected unauthenticated Firestore query to fail")
    } catch (e: Exception) {
      val fsEx = e.unwrapFirestoreException()
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }
    val unauthRepo = KhataRepository(firestore, auth)
    try {
      withTimeout(FLOW_TIMEOUT_MS) { unauthRepo.observeCustomers("any_shop").first() }
      fail("Expected unauthenticated repository call to fail")
    } catch (_: IllegalStateException) {
      // Expected auth guard
    }

    // 2. Owner 1 creates shared shop, invites Owner 2 & Owner 3, and adds Customer Ravi
    val owner1Uid = signInTestUser(owner1Email)
    val repo1 = KhataRepository(firestore, auth)
    val shopId = withTimeout(OP_TIMEOUT_MS) {
      repo1.createShop("Sri Lakshmi Kirana", "Ramesh", owner1Email, "9876543210")
    }
    withTimeout(OP_TIMEOUT_MS) {
      repo1.inviteOwner(shopId, owner2Email, "Ramesh")
      repo1.inviteOwner(shopId, owner3Email, "Ramesh")
    }

    val customerId = withTimeout(OP_TIMEOUT_MS) {
      repo1.addCustomer(
        shopId = shopId,
        name = "Ravi Kumar",
        phone = "9123456789",
        address = "Gandhi Nagar",
        openingBalancePaise = 0L,
        actorName = "Ramesh",
      )
    }

    // 3. Owner 2 accepts invite, sees Ravi immediately, and adds ₹500 Udhar (50,000 paise)
    signInTestUser(owner2Email)
    val repo2 = KhataRepository(firestore, auth)
    withTimeout(OP_TIMEOUT_MS) {
      repo2.acceptShopInvite(shopId, "Suresh", owner2Email, null)
    }
    val customersSeenByOwner2 = withTimeout(FLOW_TIMEOUT_MS) {
      repo2.observeCustomers(shopId).first { list -> list.any { it.id == customerId } }
    }
    assertEquals(1, customersSeenByOwner2.count { it.id == customerId })

    withTimeout(OP_TIMEOUT_MS) {
      repo2.addCreditTransaction(
        shopId = shopId,
        customerId = customerId,
        amountPaise = 50000L,
        description = "Rice Bag 25kg",
        quantity = "1 bag",
        notes = null,
        actorName = "Suresh",
      )
    }

    // 4. Owner 3 accepts invite, sees ₹500 balance, records ₹200 partial payment
    signInTestUser(owner3Email)
    val repo3 = KhataRepository(firestore, auth)
    withTimeout(OP_TIMEOUT_MS) {
      repo3.acceptShopInvite(shopId, "Mahesh", owner3Email, null)
    }
    val beforePartial = withTimeout(FLOW_TIMEOUT_MS) {
      repo3.observeCustomers(shopId).first { list ->
        list.firstOrNull { it.id == customerId }?.outstandingBalancePaise == 50000L
      }
    }
    assertEquals(50000L, beforePartial.first { it.id == customerId }.outstandingBalancePaise)

    val paymentId1 = "pay_${UUID.randomUUID().toString().replace("-", "")}"
    val receipt1 = withTimeout(OP_TIMEOUT_MS) {
      repo3.receivePayment(
        shopId = shopId,
        customerId = customerId,
        amountPaise = 20000L,
        method = PaymentMethod.CASH,
        notes = "Part payment",
        actorName = "Mahesh",
        clientPaymentId = paymentId1,
      )
    }
    assertEquals(30000L, receipt1.balanceAfterPaise)

    // 5. Duplicate payment with same paymentId must be prevented
    try {
      withTimeout(OP_TIMEOUT_MS) {
        repo3.receivePayment(
          shopId = shopId,
          customerId = customerId,
          amountPaise = 20000L,
          method = PaymentMethod.CASH,
          notes = "Duplicate retry",
          actorName = "Mahesh",
          clientPaymentId = paymentId1,
        )
      }
      fail("Expected DuplicatePaymentException")
    } catch (_: DuplicatePaymentException) {
      // Expected
    }

    // 6. Unauthorized stranger cannot access shop customers
    signInTestUser(strangerEmail)
    val strangerRepo = KhataRepository(firestore, auth)
    try {
      withTimeout(FLOW_TIMEOUT_MS) { strangerRepo.observeCustomers(shopId).first() }
      fail("Expected stranger query to fail with PERMISSION_DENIED")
    } catch (e: Exception) {
      val fsEx = e.unwrapFirestoreException()
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    // 7. Owner 1 sees ₹300 remaining and reverses the ₹200 payment; audit trail stays intact & balance restores to ₹500
    signInTestUser(owner1Email)
    val owner1Cust = withTimeout(FLOW_TIMEOUT_MS) {
      repo1.observeCustomers(shopId).first { list ->
        list.firstOrNull { it.id == customerId }?.outstandingBalancePaise == 30000L
      }
    }
    assertEquals(30000L, owner1Cust.first { it.id == customerId }.outstandingBalancePaise)

    val paymentTxId = checkNotNull(receipt1.transactionId)
    withTimeout(OP_TIMEOUT_MS) {
      repo1.reverseTransaction(
        shopId = shopId,
        transactionId = paymentTxId,
        reason = "Wrong entry test",
        actorName = "Ramesh",
      )
    }

    val afterReversalCust = withTimeout(FLOW_TIMEOUT_MS) {
      repo1.observeCustomers(shopId).first { list ->
        list.firstOrNull { it.id == customerId }?.outstandingBalancePaise == 50000L
      }
    }
    assertEquals(50000L, afterReversalCust.first { it.id == customerId }.outstandingBalancePaise)

    val txs = withTimeout(FLOW_TIMEOUT_MS) { repo1.observeTransactions(shopId).first() }
    assertTrue(txs.any { it.id == paymentTxId && it.status == "REVERSED" })
    assertTrue(txs.any { it.type == "REVERSAL" && it.relatedTransactionId == paymentTxId })

    // 8. Owner 1 records full ₹500 payment -> balance becomes ₹0
    val fullPayId = "pay_${UUID.randomUUID().toString().replace("-", "")}"
    val fullReceipt = withTimeout(OP_TIMEOUT_MS) {
      repo1.receivePayment(
        shopId = shopId,
        customerId = customerId,
        amountPaise = 50000L,
        method = PaymentMethod.UPI,
        notes = "Full settlement",
        actorName = "Ramesh",
        clientPaymentId = fullPayId,
      )
    }
    assertEquals(0L, fullReceipt.balanceAfterPaise)
    assertEquals(owner1Uid, fullReceipt.receivedByUid)
  }

  private companion object {
    const val OP_TIMEOUT_MS = 8000L
    const val FLOW_TIMEOUT_MS = 5000L
  }
}
