package com.example.data

import com.example.model.OwnerRole
import com.example.model.PaymentMethod
import com.example.model.RecordStatus
import com.example.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoKhataStoreUnitTest {

  @Test
  fun preSeededDataContainsCustomersOwnersAndTransactions() = runTest {
    val store = DemoKhataStore()
    val customers = store.observeCustomers(DemoKhataStore.DEMO_SHOP_ID).first()
    val members = store.observeMembers(DemoKhataStore.DEMO_SHOP_ID).first()
    val transactions = store.observeTransactions(DemoKhataStore.DEMO_SHOP_ID).first()

    assertEquals(4, customers.size)
    assertTrue(customers.any { it.name == "Ramesh" && it.outstandingBalancePaise == 125_000L })
    assertTrue(customers.any { it.name == "Suresh" && it.outstandingBalancePaise == 160_000L })
    assertTrue(customers.any { it.name == "Lakshmi" && it.outstandingBalancePaise == 0L })
    assertTrue(customers.any { it.name == "Venkatesh" && it.outstandingBalancePaise == 45_000L })
    assertEquals(3, members.size)
    assertEquals(8, transactions.size)
  }

  @Test
  fun addCreditAndReceivePaymentUpdatesBalancePreventsDuplicatesAndAllowsReversal() = runTest {
    val store = DemoKhataStore()
    store.switchDemoOwner(OwnerRole.OWNER_2)
    assertEquals(DemoKhataStore.OWNER_2_UID, store.activeOwnerUid.value)

    store.addCreditTransaction(
      shopId = DemoKhataStore.DEMO_SHOP_ID,
      customerId = "demo_cust_ramesh",
      amountPaise = 25_000L,
      description = "Cooking Oil 2L",
      quantity = "2L",
      notes = null,
      actorName = "Kiran (Owner 2)",
      clientTxId = "demo_tx_new_1",
    )

    val afterCredit = store.observeCustomers(DemoKhataStore.DEMO_SHOP_ID).first()
      .first { it.id == "demo_cust_ramesh" }
    assertEquals(150_000L, afterCredit.outstandingBalancePaise)

    val receipt = store.receivePayment(
      shopId = DemoKhataStore.DEMO_SHOP_ID,
      customerId = "demo_cust_ramesh",
      amountPaise = 50_000L,
      method = PaymentMethod.UPI,
      notes = "GPay received",
      actorName = "Kiran (Owner 2)",
      clientPaymentId = "demo_pay_new_1",
    )
    assertEquals(150_000L, receipt.balanceBeforePaise)
    assertEquals(100_000L, receipt.balanceAfterPaise)
    assertEquals("Kiran (Owner 2)", receipt.receivedByName)

    val duplicateFailure = runCatching {
      store.receivePayment(
        shopId = DemoKhataStore.DEMO_SHOP_ID,
        customerId = "demo_cust_ramesh",
        amountPaise = 50_000L,
        method = PaymentMethod.UPI,
        notes = null,
        actorName = "Kiran (Owner 2)",
        clientPaymentId = "demo_pay_new_1",
      )
    }
    assertTrue(duplicateFailure.exceptionOrNull() is DuplicatePaymentException)

    val txId = checkNotNull(receipt.transactionId)
    store.reverseTransaction(
      shopId = DemoKhataStore.DEMO_SHOP_ID,
      transactionId = txId,
      reason = "Entered wrong customer",
      actorName = "Kiran (Owner 2)",
    )
    val afterReversal = store.observeCustomers(DemoKhataStore.DEMO_SHOP_ID).first()
      .first { it.id == "demo_cust_ramesh" }
    assertEquals(150_000L, afterReversal.outstandingBalancePaise)

    val txs = store.observeTransactions(DemoKhataStore.DEMO_SHOP_ID).first()
    assertEquals(RecordStatus.REVERSED.value, txs.first { it.id == txId }.status)
    assertTrue(txs.any { it.type == TransactionType.REVERSAL.value && it.relatedTransactionId == txId })
  }

  @Test
  fun roleSwitchRestrictsAdminOnlyOwnerToggleInDemoMode() = runTest {
    val store = DemoKhataStore()
    val owner2 = store.observeMembers(DemoKhataStore.DEMO_SHOP_ID).first()
      .first { it.uid == DemoKhataStore.OWNER_2_UID }

    store.switchDemoOwner(OwnerRole.OWNER_3)
    val nonAdminAttempt = runCatching {
      store.toggleOwnerActive(DemoKhataStore.DEMO_SHOP_ID, owner2, false, "Prakash (Owner 3)")
    }
    assertTrue(nonAdminAttempt.isFailure)

    store.switchDemoOwner(OwnerRole.MAIN_ADMIN)
    store.toggleOwnerActive(DemoKhataStore.DEMO_SHOP_ID, owner2, false, "Ramesh (Main Admin)")
    val updatedOwner2 = store.observeMembers(DemoKhataStore.DEMO_SHOP_ID).first()
      .first { it.uid == DemoKhataStore.OWNER_2_UID }
    assertEquals(false, updatedOwner2.active)
    assertEquals(false, store.isCurrentActorDeactivated.value)

    store.switchDemoOwner(OwnerRole.OWNER_2)
    assertTrue(store.isCurrentActorDeactivated.value)
    val blockedCredit = runCatching {
      store.addCreditTransaction(
        shopId = DemoKhataStore.DEMO_SHOP_ID,
        customerId = "demo_cust_ramesh",
        amountPaise = 10_000L,
        description = "Sugar",
        quantity = null,
        notes = null,
        actorName = "Kiran (Owner 2)",
      )
    }
    assertTrue(blockedCredit.isFailure)

    store.switchDemoOwner(OwnerRole.MAIN_ADMIN)
    assertEquals(false, store.isCurrentActorDeactivated.value)
    store.toggleOwnerActive(DemoKhataStore.DEMO_SHOP_ID, updatedOwner2, true, "Ramesh (Main Admin)")
    store.switchDemoOwner(OwnerRole.OWNER_2)
    assertEquals(false, store.isCurrentActorDeactivated.value)
  }

  @Test
  fun customerPreviewMode_initializesAndSwitchesCleanlyWithOwnerRoles() {
    val store = DemoKhataStore(initialCustomerView = true)
    assertTrue(store.isCustomerPreview.value)
    assertEquals("demo_cust_ramesh", store.previewCustomerId.value)

    store.selectPreviewCustomer("demo_cust_suresh")
    assertEquals("demo_cust_suresh", store.previewCustomerId.value)

    store.switchDemoOwner(OwnerRole.OWNER_2)
    assertEquals(false, store.isCustomerPreview.value)
    assertEquals(DemoKhataStore.OWNER_2_UID, store.activeOwnerUid.value)

    store.setCustomerPreviewMode(true)
    assertTrue(store.isCustomerPreview.value)
  }
}
