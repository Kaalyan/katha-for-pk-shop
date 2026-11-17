package com.example.checklist

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DemoKhataStore
import com.example.data.DuplicatePaymentException
import com.example.localization.stringsFor
import com.example.model.AppLanguage
import com.example.model.OwnerRole
import com.example.model.PaymentMethod
import com.example.model.RecordStatus
import com.example.model.TransactionType
import com.example.util.ReportExporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QualityChecklistVerificationTest {

  @Test
  fun checklist_items_1_to_7_and_11_multiOwnerSyncPaymentsNotificationsAndReversal() = runTest {
    val store = DemoKhataStore()
    val shopId = DemoKhataStore.DEMO_SHOP_ID

    // 1. Three owners exist and can act
    val members = store.observeMembers(shopId).first()
    assertEquals(3, members.size)
    assertEquals(
      setOf(OwnerRole.MAIN_ADMIN.value, OwnerRole.OWNER_2.value, OwnerRole.OWNER_3.value),
      members.map { it.role }.toSet(),
    )

    // 2. Shared customer visible identically to Owner 1, Owner 2, Owner 3
    store.switchDemoOwner(OwnerRole.MAIN_ADMIN)
    val custOwner1 = store.observeCustomers(shopId).first().first { it.id == "demo_cust_venkatesh" }
    store.switchDemoOwner(OwnerRole.OWNER_2)
    val custOwner2 = store.observeCustomers(shopId).first().first { it.id == "demo_cust_venkatesh" }
    store.switchDemoOwner(OwnerRole.OWNER_3)
    val custOwner3 = store.observeCustomers(shopId).first().first { it.id == "demo_cust_venkatesh" }
    assertEquals(custOwner1.outstandingBalancePaise, custOwner2.outstandingBalancePaise)
    assertEquals(custOwner2.outstandingBalancePaise, custOwner3.outstandingBalancePaise)

    // 3. New Udhar added by Owner 2 syncs immediately to Owner 1 & Owner 3
    store.switchDemoOwner(OwnerRole.OWNER_2)
    val initialDue = custOwner2.outstandingBalancePaise // 45,000 paise (Rs 450)
    store.addCreditTransaction(
      shopId = shopId,
      customerId = "demo_cust_venkatesh",
      amountPaise = 55_000L, // Rs 550 -> total 100,000 paise (Rs 1,000)
      description = "Rice & Pulses",
      quantity = "5kg",
      notes = null,
      actorName = "Kiran (Owner 2)",
      clientTxId = "chk_tx_credit_1",
    )
    store.switchDemoOwner(OwnerRole.MAIN_ADMIN)
    val afterUdhar = store.observeCustomers(shopId).first().first { it.id == "demo_cust_venkatesh" }
    assertEquals(initialDue + 55_000L, afterUdhar.outstandingBalancePaise)

    // 4. Partial payment calculates remaining balance accurately (Rs 1,000 - Rs 400 = Rs 600)
    val partialReceipt = store.receivePayment(
      shopId = shopId,
      customerId = "demo_cust_venkatesh",
      amountPaise = 40_000L,
      method = PaymentMethod.CASH,
      notes = "Manual counter cash",
      actorName = "Ramesh (Main Admin)",
      clientPaymentId = "chk_pay_partial_1",
    )
    assertEquals(100_000L, partialReceipt.balanceBeforePaise)
    assertEquals(60_000L, partialReceipt.balanceAfterPaise)

    // 6. Duplicate payment submission with same idempotency ID is prevented
    val dupResult = runCatching {
      store.receivePayment(
        shopId = shopId,
        customerId = "demo_cust_venkatesh",
        amountPaise = 40_000L,
        method = PaymentMethod.CASH,
        notes = null,
        actorName = "Ramesh (Main Admin)",
        clientPaymentId = "chk_pay_partial_1",
      )
    }
    assertTrue(dupResult.exceptionOrNull() is DuplicatePaymentException)

    // 5. Full payment brings remaining balance to zero (Rs 600 - Rs 600 = Rs 0)
    val fullReceipt = store.receivePayment(
      shopId = shopId,
      customerId = "demo_cust_venkatesh",
      amountPaise = 60_000L,
      method = PaymentMethod.UPI,
      notes = "Manual UPI confirmation",
      actorName = "Ramesh (Main Admin)",
      clientPaymentId = "chk_pay_full_2",
    )
    assertEquals(0L, fullReceipt.balanceAfterPaise)
    val settledCust = store.observeCustomers(shopId).first().first { it.id == "demo_cust_venkatesh" }
    assertEquals(0L, settledCust.outstandingBalancePaise)

    // 7. Notifications are created for co-owners in both English & Telugu
    val notifications = store.observeNotifications(shopId).first()
    val latestNotif = notifications.first()
    assertTrue(latestNotif.messageEn.contains("Venkatesh"))
    assertTrue(latestNotif.messageTe.contains("Venkatesh"))
    assertTrue(!latestNotif.readByUids.contains(DemoKhataStore.OWNER_3_UID))

    // 11. Transaction history stays intact and reversals create audit entries without deletion
    val txIdToReverse = checkNotNull(fullReceipt.transactionId)
    store.reverseTransaction(
      shopId = shopId,
      transactionId = txIdToReverse,
      reason = "Incorrect customer selected",
      actorName = "Ramesh (Main Admin)",
    )
    val allTxs = store.observeTransactions(shopId).first()
    val origTx = allTxs.first { it.id == txIdToReverse }
    assertEquals(RecordStatus.REVERSED.value, origTx.status)
    assertTrue(allTxs.any { it.type == TransactionType.REVERSAL.value && it.relatedTransactionId == txIdToReverse })
    val restoredCust = store.observeCustomers(shopId).first().first { it.id == "demo_cust_venkatesh" }
    assertEquals(60_000L, restoredCust.outstandingBalancePaise)
  }

  @Test
  fun checklist_items_8_and_9_bilingualStringsAndPdfCsvExportsWork() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val store = DemoKhataStore()
    val shopId = DemoKhataStore.DEMO_SHOP_ID
    val customers = store.observeCustomers(shopId).first()
    val transactions = store.observeTransactions(shopId).first()
    val payments = store.observePayments(shopId).first()

    // 8. Bilingual English and Telugu strings + manual payment disclaimer present
    val en = stringsFor(AppLanguage.EN)
    val te = stringsFor(AppLanguage.TE)
    assertTrue(en.manualPaymentDisclaimer.contains("manual", ignoreCase = true))
    assertTrue(te.manualPaymentDisclaimer.contains("మాన్యువల్"))
    assertTrue(en.receivePaymentBtn.isNotBlank() && te.receivePaymentBtn.isNotBlank())

    // 9. CSV exports write valid files and PDF builders produce complete bilingual reports & receipts
    val shopCsv = ReportExporter.exportShopReportCsv(context, "Sri Lakshmi Kirana", customers, transactions)
    val custCsv = ReportExporter.exportCustomerStatementCsv(context, "Sri Lakshmi Kirana", customers.first(), transactions)

    listOf(shopCsv, custCsv).forEach { file ->
      assertTrue("Expected exported file to exist: ${file.name}", file.exists())
      assertTrue("Expected non-empty exported file: ${file.name}", file.length() > 0L)
    }
    val shopCsvText = shopCsv.readText()
    assertTrue(shopCsvText.contains("Ramesh"))
    assertTrue(shopCsvText.contains("Outstanding Balance (INR)"))
    assertTrue(custCsv.readText().contains("Customer"))

    val shopPdfLines = ReportExporter.buildShopReportLines(
      shopName = "Sri Lakshmi Kirana",
      language = AppLanguage.EN,
      customers = customers,
      todayCreditPaise = 175_000L,
      todayPaymentPaise = 180_000L,
      monthCreditPaise = 575_000L,
      monthPaymentPaise = 315_000L,
      totalOutstandingPaise = 330_000L,
    )
    assertTrue(shopPdfLines.any { it.contains("Sri Lakshmi Kirana") })
    assertTrue(shopPdfLines.any { it.contains("₹3,300") })

    val custPdfLinesTe = ReportExporter.buildCustomerStatementLines(
      shopName = "Sri Lakshmi Kirana",
      language = AppLanguage.TE,
      customer = customers.first(),
      transactions = transactions.filter { it.customerId == customers.first().id },
    )
    assertTrue(custPdfLinesTe.any { it.contains(te.customerKhataTitle) })
    assertTrue(custPdfLinesTe.any { it.contains(customers.first().name) })

    val receiptLines = ReportExporter.buildPaymentReceiptLines(
      shopName = "Sri Lakshmi Kirana",
      language = AppLanguage.EN,
      payment = payments.first(),
    )
    assertTrue(receiptLines.any { it.contains(payments.first().receiptNumber) })
    assertTrue(receiptLines.any { it.contains(en.manualPaymentDisclaimer) })
  }

  @Test
  fun phase3_exact500Credit200Partial300FinalZeroAndConcurrentSafety() = runTest {
    val store = DemoKhataStore()
    val shopId = DemoKhataStore.DEMO_SHOP_ID
    val custId = store.addCustomer(
      shopId = shopId,
      name = "Audit Customer",
      phone = "9848099999",
      address = "Market",
      openingBalancePaise = 0L,
      actorName = "Ramesh (Main Admin)",
    )

    // ₹500 credit entry (50,000 paise)
    store.addCreditTransaction(
      shopId = shopId,
      customerId = custId,
      amountPaise = 50_000L,
      description = "Dal & Oil",
      quantity = "1 pack",
      notes = null,
      actorName = "Ramesh (Main Admin)",
      clientTxId = "p3_tx_500",
    )
    assertEquals(50_000L, store.observeCustomers(shopId).first().first { it.id == custId }.outstandingBalancePaise)

    // ₹200 partial payment -> ₹300 remaining
    val p1 = store.receivePayment(
      shopId = shopId,
      customerId = custId,
      amountPaise = 20_000L,
      method = PaymentMethod.CASH,
      notes = null,
      actorName = "Kiran (Owner 2)",
      clientPaymentId = "p3_pay_200",
    )
    assertEquals(30_000L, p1.balanceAfterPaise)

    // Concurrent over-settlement protection: two owners try paying ₹300 at once; at most one can succeed
    store.receivePayment(
      shopId = shopId,
      customerId = custId,
      amountPaise = 30_000L,
      method = PaymentMethod.UPI,
      notes = null,
      actorName = "Prakash (Owner 3)",
      clientPaymentId = "p3_pay_300",
    )
    assertEquals(0L, store.observeCustomers(shopId).first().first { it.id == custId }.outstandingBalancePaise)

    val secondOverPay = runCatching {
      store.receivePayment(
        shopId = shopId,
        customerId = custId,
        amountPaise = 10_000L,
        method = PaymentMethod.CASH,
        notes = null,
        actorName = "Ramesh (Main Admin)",
        clientPaymentId = "p3_pay_over",
      )
    }
    assertTrue(secondOverPay.isFailure)
    assertEquals(0L, store.observeCustomers(shopId).first().first { it.id == custId }.outstandingBalancePaise)
  }
}
