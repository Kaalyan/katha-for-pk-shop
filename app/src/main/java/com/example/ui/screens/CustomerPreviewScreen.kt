package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.Customer
import com.example.model.LedgerTransaction
import com.example.model.PaymentRecord
import com.example.ui.components.EmptyLedgerState
import com.example.ui.components.StatMetricCard
import com.example.ui.theme.KhataCrimsonDue
import com.example.ui.theme.KhataCrimsonSoft
import com.example.ui.theme.KhataEmeraldPaid
import com.example.ui.theme.KhataForestGreen
import com.example.ui.theme.KhataMintSoft
import com.example.ui.theme.KhataSaffronGold
import com.example.util.CurrencyAndDateUtils
import com.example.util.ReportExporter

@Composable
fun CustomerPreviewScreen(
  strings: AppStrings,
  language: AppLanguage,
  shopName: String,
  customers: List<Customer>,
  selectedCustomerId: String,
  transactions: List<LedgerTransaction>,
  payments: List<PaymentRecord>,
  onSelectCustomer: (String) -> Unit,
  onViewReceipt: (PaymentRecord) -> Unit,
  onSwitchToOwnerView: () -> Unit,
) {
  val context = LocalContext.current
  val activeCustomer = remember(customers, selectedCustomerId) {
    customers.firstOrNull { it.id == selectedCustomerId } ?: customers.firstOrNull()
  }

  if (activeCustomer == null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      EmptyLedgerState(title = strings.noCustomersTitle, subtitle = strings.noCustomersSubtitle)
    }
    return
  }

  val customerTxs = remember(transactions, activeCustomer.id) {
    transactions
      .filter { it.customerId == activeCustomer.id }
      .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
  }
  val paymentById = remember(payments) { payments.associateBy { it.id } }
  val hasDue = activeCustomer.outstandingBalancePaise > 0L

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("customer_preview_screen"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Customer selector strip for previewing multiple customer passbooks
    item {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = strings.customerPreviewSelectCustomer,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          customers.forEach { customer ->
            val selected = customer.id == activeCustomer.id
            FilterChip(
              selected = selected,
              onClick = { onSelectCustomer(customer.id) },
              label = {
                Text(
                  text = "${customer.name} (${CurrencyAndDateUtils.formatRupees(customer.outstandingBalancePaise)})",
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  maxLines = 1,
                )
              },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primary,
                  selectedLabelColor = Color.White,
                ),
              modifier = Modifier.minimumInteractiveComponentSize().testTag("preview_customer_${customer.id}"),
            )
          }
        }
      }
    }

    // Customer Khata Passbook Hero Card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = KhataForestGreen),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().testTag("customer_preview_hero_card"),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = strings.customerPreviewBannerTitle,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.85f),
              )
              Text(
                text = activeCustomer.name,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Phone,
                  contentDescription = null,
                  tint = Color.White.copy(alpha = 0.85f),
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text = activeCustomer.phone,
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.White.copy(alpha = 0.9f),
                )
              }
              activeCustomer.address?.takeIf { it.isNotBlank() }?.let { addr ->
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(14.dp),
                  )
                  Text(
                    text = addr,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                  )
                }
              }
            }

            Surface(
              color = if (hasDue) KhataCrimsonSoft else KhataMintSoft,
              shape = RoundedCornerShape(12.dp),
            ) {
              Text(
                text = if (hasDue) strings.customerPreviewDueBadge else strings.customerPreviewSettledBadge,
                style = MaterialTheme.typography.labelMedium,
                color = if (hasDue) KhataCrimsonDue else KhataEmeraldPaid,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }
          }

          Surface(
            color = Color.White.copy(alpha = 0.14f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = strings.remainingBalance,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
              )
              Text(
                text = CurrencyAndDateUtils.formatRupees(activeCustomer.outstandingBalancePaise),
                style = MaterialTheme.typography.headlineMedium,
                color = if (hasDue) Color(0xFFFDE68A) else Color(0xFF86EFAC),
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }

    // Account Breakdown Metrics
    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatMetricCard(
          label = strings.openingBalance,
          value = CurrencyAndDateUtils.formatRupees(activeCustomer.openingBalancePaise),
          accentColor = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f),
        )
        StatMetricCard(
          label = strings.totalCredit,
          value = CurrencyAndDateUtils.formatRupees(activeCustomer.totalCreditPaise),
          accentColor = KhataCrimsonDue,
          modifier = Modifier.weight(1f),
        )
        StatMetricCard(
          label = strings.totalPaid,
          value = CurrencyAndDateUtils.formatRupees(activeCustomer.totalPaidPaise),
          accentColor = KhataEmeraldPaid,
          modifier = Modifier.weight(1f),
        )
      }
    }

    // Read-only customer notice + switch to Owner View action
    item {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Lock,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = strings.customerPreviewReadOnlyNote,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.weight(1f),
            )
          }
          OutlinedButton(
            onClick = onSwitchToOwnerView,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("customer_preview_switch_owner_btn"),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(strings.customerPreviewSwitchToOwner, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // Export Customer Statement (PDF / CSV)
    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
          onClick = {
            val file =
              ReportExporter.exportCustomerStatementPdf(
                context = context,
                shopName = shopName,
                language = language,
                customer = activeCustomer,
                transactions = customerTxs,
              )
            ReportExporter.shareFile(context, file, "application/pdf", strings.downloadStatementPdf)
          },
          modifier = Modifier.weight(1f).height(48.dp).testTag("customer_preview_pdf_btn"),
        ) {
          Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text(strings.downloadStatementPdf, maxLines = 1)
        }
        OutlinedButton(
          onClick = {
            val file =
              ReportExporter.exportCustomerStatementCsv(
                context = context,
                shopName = shopName,
                customer = activeCustomer,
                transactions = customerTxs,
              )
            ReportExporter.shareFile(context, file, "text/csv", strings.shareStatementCsv)
          },
          modifier = Modifier.weight(1f).height(48.dp).testTag("customer_preview_csv_btn"),
        ) {
          Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text(strings.shareStatementCsv, maxLines = 1)
        }
      }
    }

    // Bills & Payment Receipts Header
    item {
      Text(
        text = strings.customerPreviewHistoryTitle,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
      )
    }

    if (customerTxs.isEmpty()) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
          Text(
            text = strings.noDueBalance,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth().padding(18.dp),
          )
        }
      }
    } else {
      items(customerTxs, key = { it.id }) { tx ->
        val isCredit = tx.type == "CREDIT"
        val isPayment = tx.type == "PAYMENT"
        val isReversed = tx.status == "REVERSED"
        val receipt = tx.paymentId?.let { paymentById[it] }

        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          shape = RoundedCornerShape(12.dp),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Top,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = tx.description + (tx.quantity?.let { " ($it)" } ?: ""),
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                  textDecoration = if (isReversed) TextDecoration.LineThrough else TextDecoration.None,
                )
                Text(
                  text = "${CurrencyAndDateUtils.formatDateTime(tx.createdAt, language)} • ${strings.createdByLabel}: ${tx.createdByName}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              Column(horizontalAlignment = Alignment.End) {
                val amountColor =
                  when {
                    isReversed -> MaterialTheme.colorScheme.onSurfaceVariant
                    isCredit -> KhataCrimsonDue
                    isPayment -> KhataEmeraldPaid
                    else -> KhataSaffronGold
                  }
                val prefix =
                  when {
                    isCredit -> "+ "
                    isPayment -> "- "
                    else -> ""
                  }
                Text(
                  text = "$prefix${CurrencyAndDateUtils.formatRupees(tx.amountPaise)}",
                  style = MaterialTheme.typography.titleMedium,
                  color = amountColor,
                  fontWeight = FontWeight.Bold,
                  textDecoration = if (isReversed) TextDecoration.LineThrough else TextDecoration.None,
                )
                Text(
                  text = "${strings.balanceCol}: ${CurrencyAndDateUtils.formatRupees(tx.balanceAfterPaise)}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            if (isReversed) {
              Text(
                text = "${strings.statusReversed}${tx.reversalReason?.let { ": $it" } ?: ""}",
                style = MaterialTheme.typography.labelSmall,
                color = KhataSaffronGold,
                fontWeight = FontWeight.SemiBold,
              )
            } else if (isPayment && receipt != null) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = KhataEmeraldPaid,
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = "${strings.receiptNumberLabel} ${receipt.receiptNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = KhataEmeraldPaid,
                    fontWeight = FontWeight.SemiBold,
                  )
                }
                TextButton(
                  onClick = { onViewReceipt(receipt) },
                  modifier =
                    Modifier.minimumInteractiveComponentSize()
                      .testTag("customer_preview_receipt_${tx.id}"),
                ) {
                  Icon(Icons.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text(strings.customerPreviewViewReceipt, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }
  }
}
