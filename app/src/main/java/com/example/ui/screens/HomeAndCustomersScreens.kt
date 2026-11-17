package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.model.ActivityLog
import com.example.model.AppLanguage
import com.example.model.Customer
import com.example.model.LedgerTransaction
import com.example.ui.components.EmptyLedgerState
import com.example.ui.components.QuickActionCardButton
import com.example.ui.components.StatMetricCard
import com.example.ui.theme.KhataCrimsonDue
import com.example.ui.theme.KhataCrimsonSoft
import com.example.ui.theme.KhataEmeraldPaid
import com.example.ui.theme.KhataForestGreen
import com.example.ui.theme.KhataMintSoft
import com.example.ui.theme.KhataSaffronGold
import com.example.ui.viewmodel.DashboardStats
import com.example.util.CurrencyAndDateUtils

@Composable
fun HomeDashboardScreen(
  strings: AppStrings,
  language: AppLanguage,
  stats: DashboardStats,
  recentTransactions: List<LedgerTransaction>,
  recentActivities: List<ActivityLog>,
  unreadNotifications: Int,
  onAddCustomerClick: () -> Unit,
  onAddUdharClick: () -> Unit,
  onReceivePaymentClick: () -> Unit,
  onViewKhataBookClick: () -> Unit,
  onNotificationsClick: () -> Unit,
  onViewAllActivityClick: () -> Unit,
  onOpenCustomerKhata: (String) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("home_dashboard_list"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Traditional Khata Book Ledger Summary Hero Card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = KhataForestGreen),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().testTag("dashboard_summary_card"),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                text = strings.totalOutstanding,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.85f),
              )
              Text(
                text = CurrencyAndDateUtils.formatRupees(stats.totalOutstandingPaise),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
              )
            }
            Surface(
              color = Color.White.copy(alpha = 0.16f),
              shape = RoundedCornerShape(14.dp),
            ) {
              Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.End,
              ) {
                Text(
                  text = strings.totalCustomers,
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White.copy(alpha = 0.85f),
                )
                Text(
                  text = stats.totalCustomers.toString(),
                  style = MaterialTheme.typography.titleLarge,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Surface(
              modifier = Modifier.weight(1f),
              color = Color.White.copy(alpha = 0.14f),
              shape = RoundedCornerShape(12.dp),
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(
                  text = strings.totalUdhar,
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White.copy(alpha = 0.85f),
                )
                Text(
                  text = CurrencyAndDateUtils.formatRupees(stats.totalCreditPaise),
                  style = MaterialTheme.typography.titleMedium,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
            Surface(
              modifier = Modifier.weight(1f),
              color = Color.White.copy(alpha = 0.14f),
              shape = RoundedCornerShape(12.dp),
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(
                  text = strings.totalReceived,
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White.copy(alpha = 0.85f),
                )
                Text(
                  text = CurrencyAndDateUtils.formatRupees(stats.totalPaidPaise),
                  style = MaterialTheme.typography.titleMedium,
                  color = Color(0xFF86EFAC),
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      }
    }

    // Today's Snapshot Row
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        StatMetricCard(
          label = strings.todayNewCredit,
          value = CurrencyAndDateUtils.formatRupees(stats.todayCreditPaise),
          accentColor = KhataCrimsonDue,
          modifier = Modifier.weight(1f),
        )
        StatMetricCard(
          label = strings.todayReceived,
          value = CurrencyAndDateUtils.formatRupees(stats.todayPaymentPaise),
          accentColor = KhataEmeraldPaid,
          modifier = Modifier.weight(1f),
        )
        StatMetricCard(
          label = strings.todayTransactions,
          value = stats.todayTransactionCount.toString(),
          accentColor = KhataSaffronGold,
          modifier = Modifier.weight(0.8f),
        )
      }
    }

    // 5 Prominent Shop Owner Quick Action Buttons
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = strings.quickActions,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )

        // Prominent Green MARK AS PAID / Receive Payment button
        QuickActionCardButton(
          title = strings.receivePaymentBtn,
          icon = Icons.Filled.CheckCircle,
          containerColor = KhataEmeraldPaid,
          contentColor = Color.White,
          testTag = "home_receive_payment_btn",
          onClick = onReceivePaymentClick,
          modifier = Modifier.fillMaxWidth(),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          QuickActionCardButton(
            title = strings.addUdharBtn,
            icon = Icons.Filled.AddCard,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            testTag = "home_add_udhar_btn",
            onClick = onAddUdharClick,
            modifier = Modifier.weight(1f),
          )
          QuickActionCardButton(
            title = strings.addCustomerBtn,
            icon = Icons.Filled.PersonAdd,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            testTag = "home_add_customer_btn",
            onClick = onAddCustomerClick,
            modifier = Modifier.weight(1f),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          QuickActionCardButton(
            title = strings.viewKhataBookBtn,
            icon = Icons.AutoMirrored.Filled.MenuBook,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            testTag = "home_view_khata_btn",
            onClick = onViewKhataBookClick,
            modifier = Modifier.weight(1f),
          )
          QuickActionCardButton(
            title = strings.notificationsBtn,
            icon = Icons.Filled.Notifications,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            testTag = "home_notifications_btn",
            badgeCount = unreadNotifications,
            onClick = onNotificationsClick,
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    // Recent Transactions
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = strings.recentTransactions,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )
        TextButton(onClick = onViewKhataBookClick) {
          Text(strings.viewAll)
        }
      }
    }

    if (recentTransactions.isEmpty()) {
      item {
        EmptyLedgerState(
          title = strings.noCustomersTitle,
          subtitle = strings.noCustomersSubtitle,
        )
      }
    } else {
      items(recentTransactions.take(5), key = { it.id }) { tx ->
        TransactionCompactCard(
          tx = tx,
          strings = strings,
          language = language,
          onClick = { onOpenCustomerKhata(tx.customerId) },
        )
      }
    }

    // Recent Activities
    if (recentActivities.isNotEmpty()) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = strings.recentActivities,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
          )
          TextButton(onClick = onViewAllActivityClick) {
            Text(strings.viewAll)
          }
        }
      }

      items(recentActivities.take(4), key = { it.id }) { log ->
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(
              text = if (language == AppLanguage.TE) log.summaryTe else log.summaryEn,
              style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
              text = CurrencyAndDateUtils.formatShortDate(log.createdAt, language),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@Composable
fun CustomersListScreen(
  strings: AppStrings,
  customers: List<Customer>,
  onAddCustomerClick: () -> Unit,
  onOpenCustomerKhata: (Customer) -> Unit,
  onAddUdharForCustomer: (Customer) -> Unit,
  onReceivePaymentForCustomer: (Customer) -> Unit,
) {
  var searchQuery by remember { mutableStateOf("") }
  val filtered = remember(customers, searchQuery) {
    val q = searchQuery.trim().lowercase()
    if (q.isEmpty()) customers
    else customers.filter {
      it.name.lowercase().contains(q) || it.phone.lowercase().contains(q)
    }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text(strings.searchCustomersHint, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.weight(1f).testTag("customer_search_input"),
      )
      Button(
        onClick = onAddCustomerClick,
        modifier = Modifier.height(54.dp).testTag("customers_add_button"),
        shape = RoundedCornerShape(14.dp),
      ) {
        Icon(Icons.Filled.PersonAdd, contentDescription = strings.addCustomerBtn)
      }
    }

    if (filtered.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyLedgerState(title = strings.noCustomersTitle, subtitle = strings.noCustomersSubtitle)
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(filtered, key = { it.id }) { customer ->
          CustomerCardItem(
            customer = customer,
            strings = strings,
            onOpenKhata = { onOpenCustomerKhata(customer) },
            onAddUdhar = { onAddUdharForCustomer(customer) },
            onMarkPaid = { onReceivePaymentForCustomer(customer) },
          )
        }
      }
    }
  }
}

@Composable
private fun CustomerCardItem(
  customer: Customer,
  strings: AppStrings,
  onOpenKhata: () -> Unit,
  onAddUdhar: () -> Unit,
  onMarkPaid: () -> Unit,
) {
  val hasDue = customer.outstandingBalancePaise > 0L
  Card(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .clickable(onClick = onOpenKhata)
        .testTag("customer_card_${customer.id}"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = customer.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Phone,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = customer.phone,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!customer.address.isNullOrBlank()) {
              Text(
                text = " • ${customer.address}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
        Surface(
          color = if (hasDue) KhataCrimsonSoft else KhataMintSoft,
          shape = RoundedCornerShape(12.dp),
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.End,
          ) {
            Text(
              text = strings.remainingBalance,
              style = MaterialTheme.typography.labelSmall,
              color = if (hasDue) KhataCrimsonDue else KhataEmeraldPaid,
            )
            Text(
              text = CurrencyAndDateUtils.formatRupees(customer.outstandingBalancePaise),
              style = MaterialTheme.typography.titleMedium,
              color = if (hasDue) KhataCrimsonDue else KhataEmeraldPaid,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = onAddUdhar,
          modifier = Modifier.weight(1f).height(48.dp),
          shape = RoundedCornerShape(12.dp),
        ) {
          Text(strings.addUdharBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Button(
          onClick = onMarkPaid,
          enabled = hasDue,
          modifier = Modifier.weight(1f).height(48.dp),
          shape = RoundedCornerShape(12.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = KhataEmeraldPaid,
              contentColor = Color.White,
            ),
        ) {
          Text(strings.markAsPaidBtn, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
      }
    }
  }
}

@Composable
fun TransactionCompactCard(
  tx: LedgerTransaction,
  strings: AppStrings,
  language: AppLanguage,
  onClick: () -> Unit,
  onReverseClick: (() -> Unit)? = null,
) {
  val isCredit = tx.type == "CREDIT"
  val isPayment = tx.type == "PAYMENT"
  val isReversed = tx.status == "REVERSED"

  val amountColor =
    when {
      isReversed -> MaterialTheme.colorScheme.onSurfaceVariant
      isPayment -> KhataEmeraldPaid
      isCredit -> KhataCrimsonDue
      else -> KhataSaffronGold
    }

  Card(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .clickable(onClick = onClick),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = tx.customerName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = tx.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "${if (isPayment) "-" else "+"}${CurrencyAndDateUtils.formatRupees(tx.amountPaise)}",
            style = MaterialTheme.typography.titleMedium,
            color = amountColor,
            fontWeight = FontWeight.Bold,
          )
          Surface(
            color =
              if (isReversed) MaterialTheme.colorScheme.errorContainer
              else if (isPayment) KhataMintSoft
              else KhataCrimsonSoft,
            shape = RoundedCornerShape(8.dp),
          ) {
            Text(
              text = if (isReversed) strings.statusReversed else strings.statusConfirmed,
              style = MaterialTheme.typography.labelSmall,
              color =
                if (isReversed) KhataCrimsonDue
                else if (isPayment) KhataEmeraldPaid
                else KhataCrimsonDue,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text =
            "${CurrencyAndDateUtils.formatShortDate(tx.createdAt, language)} • ${strings.createdByLabel}: ${tx.createdByName}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.weight(1f),
        )
        if (onReverseClick != null && !isReversed && (isCredit || isPayment)) {
          TextButton(onClick = onReverseClick) {
            Text(
              text = strings.reverseEntryBtn,
              style = MaterialTheme.typography.labelMedium,
              color = KhataSaffronGold,
            )
          }
        }
      }
    }
  }
}
