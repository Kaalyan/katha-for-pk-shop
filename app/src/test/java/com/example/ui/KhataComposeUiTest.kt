package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.example.localization.stringsFor
import com.example.model.AppLanguage
import com.example.model.Customer
import com.example.model.LedgerTransaction
import com.example.model.OwnerRole
import com.example.model.PaymentRecord
import com.example.ui.auth.AuthScreen
import com.example.ui.components.DemoModeBanner
import com.example.ui.screens.CustomerPreviewScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.theme.KiranaKhataTheme
import com.example.ui.viewmodel.DashboardStats
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@LooperMode(LooperMode.Mode.PAUSED)
class KhataComposeUiTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun homeDashboard_displaysBilingualTotalsAndActionButtons() {
    val stats =
      DashboardStats(
        totalCustomers = 4,
        totalOutstandingPaise = 125000L,
        totalCreditPaise = 200000L,
        totalPaidPaise = 75000L,
        todayCreditPaise = 50000L,
        todayPaymentPaise = 20000L,
        todayTransactionCount = 3,
      )

    var paidClicked = false
    composeRule.setContent {
      KiranaKhataTheme(language = AppLanguage.EN) {
        HomeDashboardScreen(
          strings = stringsFor(AppLanguage.EN),
          language = AppLanguage.EN,
          stats = stats,
          recentTransactions = emptyList(),
          recentActivities = emptyList(),
          unreadNotifications = 2,
          onAddCustomerClick = {},
          onAddUdharClick = {},
          onReceivePaymentClick = { paidClicked = true },
          onViewKhataBookClick = {},
          onNotificationsClick = {},
          onViewAllActivityClick = {},
          onOpenCustomerKhata = {},
        )
      }
    }

    composeRule.waitForIdle()
    composeRule.onNodeWithText("₹1,250").assertIsDisplayed()
    composeRule.onNodeWithTag("home_receive_payment_btn").assertIsDisplayed().performClick()
    assert(paidClicked)
  }

  @Test
  fun homeDashboard_teluguModeDisplaysTeluguLabels() {
    val teStrings = stringsFor(AppLanguage.TE)
    composeRule.setContent {
      KiranaKhataTheme(language = AppLanguage.TE) {
        HomeDashboardScreen(
          strings = teStrings,
          language = AppLanguage.TE,
          stats = DashboardStats(totalCustomers = 2, totalOutstandingPaise = 50000L),
          recentTransactions = emptyList(),
          recentActivities = emptyList(),
          unreadNotifications = 0,
          onAddCustomerClick = {},
          onAddUdharClick = {},
          onReceivePaymentClick = {},
          onViewKhataBookClick = {},
          onNotificationsClick = {},
          onViewAllActivityClick = {},
          onOpenCustomerKhata = {},
        )
      }
    }

    composeRule.waitForIdle()
    composeRule.onNodeWithText(teStrings.receivePaymentBtn).assertIsDisplayed()
    composeRule.onNodeWithText("₹500").assertIsDisplayed()
  }

  @Test
  fun authScreen_displaysTryDemoAndCustomerPreviewButtons() {
    val strings = stringsFor(AppLanguage.EN)
    var demoClicked = false
    var customerPreviewClicked = false

    composeRule.setContent {
      KiranaKhataTheme(language = AppLanguage.EN) {
        AuthScreen(
          language = AppLanguage.EN,
          onLanguageChange = {},
          onAuthSuccess = {},
          onTryDemoClick = { demoClicked = true },
          onTryCustomerPreviewClick = { customerPreviewClicked = true },
        )
      }
    }

    composeRule.waitForIdle()
    composeRule.onNodeWithTag("demo_mode_button").performScrollTo().assertIsDisplayed().performClick()
    assert(demoClicked)
    composeRule.onNodeWithText(strings.tryDemoModeBtn).assertIsDisplayed()
    composeRule.onNodeWithTag("customer_preview_button").performScrollTo().assertIsDisplayed().performClick()
    assert(customerPreviewClicked)
    composeRule.onNodeWithText(strings.previewCustomerViewBtn).assertIsDisplayed()
  }

  @Test
  fun demoModeBanner_displaysRoleToggleAndCustomerViewAndExitButton() {
    val strings = stringsFor(AppLanguage.EN)
    var selectedRole = OwnerRole.MAIN_ADMIN
    var customerPreviewSelected = false
    var exitClicked = false

    composeRule.setContent {
      KiranaKhataTheme(language = AppLanguage.EN) {
        DemoModeBanner(
          strings = strings,
          activeRole = selectedRole,
          onSwitchRole = { selectedRole = it },
          onExitDemo = { exitClicked = true },
          isCustomerPreview = customerPreviewSelected,
          onSelectCustomerPreview = { customerPreviewSelected = true },
        )
      }
    }

    composeRule.waitForIdle()
    composeRule.onNodeWithText(strings.demoModeBadge).assertIsDisplayed()
    composeRule.onNodeWithTag("demo_role_owner_2").performClick()
    assert(selectedRole == OwnerRole.OWNER_2)
    composeRule.onNodeWithTag("demo_role_customer_view").performClick()
    assert(customerPreviewSelected)
    composeRule.onNodeWithTag("exit_demo_button").performClick()
    assert(exitClicked)
  }

  @Test
  fun customerPreviewScreen_displaysCustomerPassbookAndSwitchesCustomers() {
    val strings = stringsFor(AppLanguage.EN)
    val customers = listOf(
      Customer(
        id = "c1",
        name = "Ramesh",
        phone = "9848011223",
        openingBalancePaise = 50_000L,
        totalCreditPaise = 100_000L,
        totalPaidPaise = 25_000L,
        outstandingBalancePaise = 125_000L,
      ),
      Customer(
        id = "c2",
        name = "Lakshmi",
        phone = "9989177889",
        openingBalancePaise = 0L,
        totalCreditPaise = 50_000L,
        totalPaidPaise = 50_000L,
        outstandingBalancePaise = 0L,
      ),
    )
    val receipt = PaymentRecord(
      id = "p1",
      customerId = "c1",
      customerName = "Ramesh",
      amountPaise = 25_000L,
      method = "CASH",
      receiptNumber = "KK-104201",
      transactionId = "t1",
      balanceBeforePaise = 150_000L,
      balanceAfterPaise = 125_000L,
      receivedByName = "Kiran",
    )
    val txs = listOf(
      LedgerTransaction(
        id = "t1",
        customerId = "c1",
        customerName = "Ramesh",
        type = "PAYMENT",
        amountPaise = 25_000L,
        description = "Payment Received (CASH)",
        paymentId = "p1",
        balanceAfterPaise = 125_000L,
        status = "CONFIRMED",
        createdByName = "Kiran",
      )
    )
    var selectedCustomer by mutableStateOf("c1")
    var viewedReceipt: PaymentRecord? = null
    var switchedToOwner = false

    composeRule.setContent {
      KiranaKhataTheme(language = AppLanguage.EN) {
        CustomerPreviewScreen(
          strings = strings,
          language = AppLanguage.EN,
          shopName = "Sri Lakshmi Kirana (Demo)",
          customers = customers,
          selectedCustomerId = selectedCustomer,
          transactions = txs,
          payments = listOf(receipt),
          onSelectCustomer = { selectedCustomer = it },
          onViewReceipt = { viewedReceipt = it },
          onSwitchToOwnerView = { switchedToOwner = true },
        )
      }
    }

    composeRule.waitForIdle()
    composeRule.onNodeWithTag("customer_preview_hero_card").assertIsDisplayed()
    composeRule.onNodeWithText("₹1,250").assertIsDisplayed()
    composeRule.onNodeWithTag("customer_preview_screen")
      .performScrollToNode(hasTestTag("customer_preview_receipt_t1"))
    composeRule.onNodeWithTag("customer_preview_receipt_t1").performClick()
    assert(viewedReceipt?.id == "p1")
    composeRule.onNodeWithTag("customer_preview_screen")
      .performScrollToNode(hasTestTag("customer_preview_switch_owner_btn"))
    composeRule.onNodeWithTag("customer_preview_switch_owner_btn").performClick()
    assert(switchedToOwner)
    composeRule.onNodeWithTag("customer_preview_screen")
      .performScrollToNode(hasTestTag("preview_customer_c2"))
    composeRule.onNodeWithTag("preview_customer_c2").performClick()
    composeRule.waitForIdle()
    assert(selectedCustomer == "c2")
    composeRule.onNodeWithText(strings.customerPreviewSettledBadge).assertIsDisplayed()
  }

  @Test
  fun homeDashboard_darkThemeRendersHeroAndQuickActions() {
    val strings = stringsFor(AppLanguage.EN)
    composeRule.setContent {
      KiranaKhataTheme(language = AppLanguage.EN, darkTheme = true) {
        HomeDashboardScreen(
          strings = strings,
          language = AppLanguage.EN,
          stats = DashboardStats(totalCustomers = 4, totalOutstandingPaise = 330_000L),
          recentTransactions = emptyList(),
          recentActivities = emptyList(),
          unreadNotifications = 1,
          onAddCustomerClick = {},
          onAddUdharClick = {},
          onReceivePaymentClick = {},
          onViewKhataBookClick = {},
          onNotificationsClick = {},
          onViewAllActivityClick = {},
          onOpenCustomerKhata = {},
        )
      }
    }

    composeRule.waitForIdle()
    composeRule.onNodeWithTag("dashboard_summary_card").assertIsDisplayed()
    composeRule.onNodeWithText("₹3,300").assertIsDisplayed()
    composeRule.onNodeWithTag("home_dashboard_list")
      .performScrollToNode(hasTestTag("home_receive_payment_btn"))
    composeRule.onNodeWithTag("home_receive_payment_btn").assertIsDisplayed()
    composeRule.onNodeWithTag("home_dashboard_list")
      .performScrollToNode(hasTestTag("home_add_udhar_btn"))
    composeRule.onNodeWithTag("home_add_udhar_btn").assertIsDisplayed()
  }
}
