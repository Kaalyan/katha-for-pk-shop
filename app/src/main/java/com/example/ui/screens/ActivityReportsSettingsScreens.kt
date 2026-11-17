package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.model.ActivityLog
import com.example.model.AppLanguage
import com.example.model.Customer
import com.example.model.LedgerTransaction
import com.example.model.OwnerRole
import com.example.model.Shop
import com.example.model.ShopMember
import com.example.model.ShopNotification
import com.example.ui.components.StatMetricCard
import com.example.ui.theme.KhataAmberSoft
import com.example.ui.theme.KhataCrimsonDue
import com.example.ui.theme.KhataCrimsonSoft
import com.example.ui.theme.KhataEmeraldPaid
import com.example.ui.theme.KhataInkPrimary
import com.example.ui.theme.KhataMintSoft
import com.example.ui.theme.KhataSaffronGold
import com.example.ui.viewmodel.DashboardStats
import com.example.util.CurrencyAndDateUtils
import com.example.util.ReportExporter

enum class DateFilterOption { ALL, TODAY, YESTERDAY, LAST_7_DAYS, THIS_MONTH, CUSTOM }
enum class TypeFilterOption { ALL, CREDIT, PAYMENT, REVERSAL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhataTransactionsAndReportsScreen(
  strings: AppStrings,
  language: AppLanguage,
  shopName: String,
  stats: DashboardStats,
  customers: List<Customer>,
  transactions: List<LedgerTransaction>,
  onOpenCustomerKhata: (String) -> Unit,
  onReverseTransaction: (LedgerTransaction) -> Unit,
) {
  val context = LocalContext.current
  var dateFilter by remember { mutableStateOf(DateFilterOption.ALL) }
  var typeFilter by remember { mutableStateOf(TypeFilterOption.ALL) }
  var showDateRangePicker by remember { mutableStateOf(false) }
  var customStartMillis by remember { mutableStateOf<Long?>(null) }
  var customEndMillis by remember { mutableStateOf<Long?>(null) }
  var showReportDetails by remember { mutableStateOf(false) }

  val filteredTransactions = remember(transactions, dateFilter, typeFilter, customStartMillis, customEndMillis) {
    transactions.filter { tx ->
      val dateOk =
        when (dateFilter) {
          DateFilterOption.ALL -> true
          DateFilterOption.TODAY -> CurrencyAndDateUtils.isToday(tx.createdAt)
          DateFilterOption.YESTERDAY -> CurrencyAndDateUtils.isYesterday(tx.createdAt)
          DateFilterOption.LAST_7_DAYS -> CurrencyAndDateUtils.isWithinLastDays(tx.createdAt, 7)
          DateFilterOption.THIS_MONTH -> CurrencyAndDateUtils.isThisMonth(tx.createdAt)
          DateFilterOption.CUSTOM -> {
            val sMs = customStartMillis
            val eMs = customEndMillis ?: sMs
            if (sMs != null && eMs != null) {
              CurrencyAndDateUtils.isBetweenDates(tx.createdAt, sMs, eMs)
            } else true
          }
        }
      val typeOk =
        when (typeFilter) {
          TypeFilterOption.ALL -> true
          TypeFilterOption.CREDIT -> tx.type == "CREDIT"
          TypeFilterOption.PAYMENT -> tx.type == "PAYMENT"
          TypeFilterOption.REVERSAL -> tx.type == "REVERSAL"
        }
      dateOk && typeOk
    }
  }

  val ownerTotals = remember(transactions) {
    transactions
      .filter { it.status == "CONFIRMED" }
      .groupBy { it.createdByName }
      .mapValues { (_, list) -> list.size to list.sumOf { it.amountPaise } }
  }

  if (showDateRangePicker) {
    val pickerState = rememberDateRangePickerState()
    DatePickerDialog(
      onDismissRequest = { showDateRangePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            customStartMillis = pickerState.selectedStartDateMillis
            customEndMillis = pickerState.selectedEndDateMillis ?: pickerState.selectedStartDateMillis
            dateFilter = DateFilterOption.CUSTOM
            showDateRangePicker = false
          }
        ) {
          Text(strings.confirm)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDateRangePicker = false }) { Text(strings.cancel) }
      },
    ) {
      DateRangePicker(state = pickerState)
    }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Reports & Export Card
    item {
      Card(
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
            Text(
              text = strings.reportsTitle,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = { showReportDetails = !showReportDetails }) {
              Text(if (showReportDetails) strings.done else strings.viewAll)
            }
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatMetricCard(
              label = strings.monthlyCredit,
              value = CurrencyAndDateUtils.formatRupees(stats.monthCreditPaise),
              accentColor = KhataCrimsonDue,
              modifier = Modifier.weight(1f),
            )
            StatMetricCard(
              label = strings.monthlyPayments,
              value = CurrencyAndDateUtils.formatRupees(stats.monthPaymentPaise),
              accentColor = KhataEmeraldPaid,
              modifier = Modifier.weight(1f),
            )
          }

          if (showReportDetails) {
            HorizontalDivider()
            Text(strings.customerWiseOutstanding, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            customers.filter { it.outstandingBalancePaise > 0 }.sortedByDescending { it.outstandingBalancePaise }.take(8).forEach { c ->
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(c.name, style = MaterialTheme.typography.bodySmall)
                Text(
                  CurrencyAndDateUtils.formatRupees(c.outstandingBalancePaise),
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Bold,
                  color = KhataCrimsonDue,
                )
              }
            }
            if (ownerTotals.isNotEmpty()) {
              HorizontalDivider()
              Text(strings.ownerWiseActivity, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
              ownerTotals.forEach { (owner, pair) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text("$owner (${pair.first})", style = MaterialTheme.typography.bodySmall)
                  Text(CurrencyAndDateUtils.formatRupees(pair.second), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
              }
            }
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = {
                val file =
                  ReportExporter.exportShopReportPdf(
                    context = context,
                    shopName = shopName,
                    language = language,
                    customers = customers,
                    todayCreditPaise = stats.todayCreditPaise,
                    todayPaymentPaise = stats.todayPaymentPaise,
                    monthCreditPaise = stats.monthCreditPaise,
                    monthPaymentPaise = stats.monthPaymentPaise,
                    totalOutstandingPaise = stats.totalOutstandingPaise,
                  )
                ReportExporter.shareFile(context, file, "application/pdf", strings.exportPdfBtn)
              },
              modifier = Modifier.weight(1f).height(48.dp).testTag("export_shop_pdf_btn"),
            ) {
              Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text(strings.exportPdfBtn, maxLines = 1)
            }
            OutlinedButton(
              onClick = {
                val file = ReportExporter.exportShopReportCsv(context, shopName, customers, filteredTransactions)
                ReportExporter.shareFile(context, file, "text/csv", strings.exportCsvBtn)
              },
              modifier = Modifier.weight(1f).height(48.dp).testTag("export_shop_csv_btn"),
            ) {
              Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text(strings.exportCsvBtn, maxLines = 1)
            }
          }
        }
      }
    }

    // Date Filters
    item {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = dateFilter == DateFilterOption.ALL,
            onClick = { dateFilter = DateFilterOption.ALL },
            label = { Text(strings.filterAll) },
          )
          FilterChip(
            selected = dateFilter == DateFilterOption.TODAY,
            onClick = { dateFilter = DateFilterOption.TODAY },
            label = { Text(strings.filterToday) },
          )
          FilterChip(
            selected = dateFilter == DateFilterOption.YESTERDAY,
            onClick = { dateFilter = DateFilterOption.YESTERDAY },
            label = { Text(strings.filterYesterday) },
          )
          FilterChip(
            selected = dateFilter == DateFilterOption.LAST_7_DAYS,
            onClick = { dateFilter = DateFilterOption.LAST_7_DAYS },
            label = { Text(strings.filterLast7Days) },
          )
          FilterChip(
            selected = dateFilter == DateFilterOption.THIS_MONTH,
            onClick = { dateFilter = DateFilterOption.THIS_MONTH },
            label = { Text(strings.filterThisMonth) },
          )
          FilterChip(
            selected = dateFilter == DateFilterOption.CUSTOM,
            onClick = { showDateRangePicker = true },
            leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) },
            label = {
              Text(
                if (dateFilter == DateFilterOption.CUSTOM && customStartMillis != null) {
                  CurrencyAndDateUtils.formatDateOnly(customStartMillis!!)
                } else strings.filterCustomDate
              )
            },
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = typeFilter == TypeFilterOption.ALL,
            onClick = { typeFilter = TypeFilterOption.ALL },
            label = { Text(strings.filterAll) },
          )
          FilterChip(
            selected = typeFilter == TypeFilterOption.CREDIT,
            onClick = { typeFilter = TypeFilterOption.CREDIT },
            label = { Text(strings.filterCredit) },
          )
          FilterChip(
            selected = typeFilter == TypeFilterOption.PAYMENT,
            onClick = { typeFilter = TypeFilterOption.PAYMENT },
            label = { Text(strings.filterPayment) },
          )
          FilterChip(
            selected = typeFilter == TypeFilterOption.REVERSAL,
            onClick = { typeFilter = TypeFilterOption.REVERSAL },
            label = { Text(strings.filterReversal) },
          )
        }
      }
    }

    items(filteredTransactions, key = { it.id }) { tx ->
      TransactionCompactCard(
        tx = tx,
        strings = strings,
        language = language,
        onClick = { onOpenCustomerKhata(tx.customerId) },
        onReverseClick = { onReverseTransaction(tx) },
      )
    }
  }
}

@Composable
fun ActivityAndNotificationsScreen(
  strings: AppStrings,
  language: AppLanguage,
  currentUserId: String,
  notifications: List<ShopNotification>,
  activityLogs: List<ActivityLog>,
  initialShowNotifications: Boolean,
  onMarkRead: (String) -> Unit,
  onMarkAllRead: (List<ShopNotification>) -> Unit,
  onOpenCustomerKhata: (String) -> Unit,
) {
  var showNotificationsTab by remember(initialShowNotifications) {
    mutableStateOf(initialShowNotifications)
  }

  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      FilterChip(
        selected = showNotificationsTab,
        onClick = { showNotificationsTab = true },
        leadingIcon = { Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp)) },
        label = { Text(strings.notificationsTitle) },
        modifier = Modifier.weight(1f),
      )
      FilterChip(
        selected = !showNotificationsTab,
        onClick = { showNotificationsTab = false },
        label = { Text(strings.activityTabAuditLogs) },
        modifier = Modifier.weight(1f),
      )
    }

    if (showNotificationsTab) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.End,
      ) {
        TextButton(onClick = { onMarkAllRead(notifications) }) {
          Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text(strings.markAllRead)
        }
      }

      if (notifications.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
          Text(strings.noNotificationsText, style = MaterialTheme.typography.bodyMedium)
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(notifications, key = { it.id }) { notif ->
            val isUnread = notif.actorUid != currentUserId && !notif.readByUids.contains(currentUserId)
            Card(
              modifier =
                Modifier.fillMaxWidth().clickable {
                  if (isUnread) onMarkRead(notif.id)
                  notif.customerId?.takeIf { it.isNotBlank() }?.let(onOpenCustomerKhata)
                },
              colors =
                CardDefaults.cardColors(
                  containerColor =
                    if (isUnread) KhataMintSoft.copy(alpha = 0.55f)
                    else MaterialTheme.colorScheme.surface
                ),
              border =
                BorderStroke(
                  1.dp,
                  if (isUnread) KhataEmeraldPaid else MaterialTheme.colorScheme.outline,
                ),
            ) {
              Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = if (language == AppLanguage.TE) notif.messageTe else notif.messageEn,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                  text = "${notif.actorName} • ${CurrencyAndDateUtils.formatShortDate(notif.createdAt, language)}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(activityLogs, key = { it.id }) { log ->
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = if (language == AppLanguage.TE) log.summaryTe else log.summaryEn,
                style = MaterialTheme.typography.bodyMedium,
              )
              Text(
                text = "${log.actorName} • ${CurrencyAndDateUtils.formatDateTime(log.createdAt, language)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun SettingsScreen(
  strings: AppStrings,
  language: AppLanguage,
  shop: Shop,
  members: List<ShopMember>,
  currentUserId: String,
  isSubmitting: Boolean,
  onLanguageChange: (AppLanguage) -> Unit,
  onInviteOwner: (String) -> Unit,
  onRemoveInvite: (String) -> Unit,
  onToggleOwnerActive: (ShopMember, Boolean) -> Unit,
  onSignOut: () -> Unit,
  isDemoMode: Boolean = false,
  activeDemoRole: OwnerRole = OwnerRole.MAIN_ADMIN,
  onSwitchDemoRole: (OwnerRole) -> Unit = {},
  onSelectCustomerPreview: () -> Unit = {},
) {
  var inviteEmail by remember { mutableStateOf("") }
  val isMainAdmin = shop.mainOwnerUid == currentUserId
  val totalReservedSlots = shop.memberUids.size + shop.invitedEmails.size

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    if (isDemoMode) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = KhataAmberSoft.copy(alpha = 0.55f)),
          border = BorderStroke(1.5.dp, KhataSaffronGold),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Visibility,
                contentDescription = null,
                tint = KhataSaffronGold,
              )
              Text(
                text = strings.demoSettingsCardTitle,
                style = MaterialTheme.typography.titleMedium,
                color = KhataInkPrimary,
                fontWeight = FontWeight.Bold,
              )
            }
            Text(
              text = strings.demoSettingsCardBody,
              style = MaterialTheme.typography.bodySmall,
              color = KhataInkPrimary.copy(alpha = 0.85f),
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              val roles =
                listOf(
                  Triple(OwnerRole.MAIN_ADMIN, strings.demoMainAdminShort, "settings_demo_role_main_admin"),
                  Triple(OwnerRole.OWNER_2, strings.demoOwner2Short, "settings_demo_role_owner_2"),
                  Triple(OwnerRole.OWNER_3, strings.demoOwner3Short, "settings_demo_role_owner_3"),
                )
              roles.forEach { (role, label, tag) ->
                val selected = activeDemoRole == role
                Button(
                  onClick = { onSwitchDemoRole(role) },
                  modifier = Modifier.weight(1f).height(48.dp).testTag(tag),
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = if (selected) KhataSaffronGold else Color.White,
                      contentColor = if (selected) Color.White else KhataInkPrimary,
                    ),
                ) {
                  Text(label, maxLines = 1, fontWeight = FontWeight.Bold)
                }
              }
            }
            OutlinedButton(
              onClick = onSelectCustomerPreview,
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("settings_demo_role_customer_view"),
              border = BorderStroke(1.dp, KhataEmeraldPaid),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = KhataEmeraldPaid),
            ) {
              Text(strings.demoCustomerViewShort, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // Language Switcher Card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(strings.settingsLanguageTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppLanguage.entries.forEach { lang ->
              val selected = language == lang
              Button(
                onClick = { onLanguageChange(lang) },
                modifier = Modifier.weight(1f).height(48.dp).testTag("settings_lang_${lang.code}"),
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor =
                      if (selected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor =
                      if (selected) Color.White
                      else MaterialTheme.colorScheme.onSurfaceVariant,
                  ),
              ) {
                if (selected) {
                  Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(6.dp))
                }
                Text(lang.displayName, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    // 3-Owner Management Card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(strings.settingsOwnersTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
          Text(strings.maxOwnersNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

          members.forEach { member ->
            val roleLabel =
              when (OwnerRole.fromValue(member.role)) {
                OwnerRole.MAIN_ADMIN -> strings.mainAdminBadge
                OwnerRole.OWNER_2 -> strings.owner2Badge
                OwnerRole.OWNER_3 -> strings.owner3Badge
              }
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
              shape = RoundedCornerShape(12.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(member.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                  Text(member.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(6.dp)) {
                      Text(
                        roleLabel,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      )
                    }
                    Surface(
                      color = if (member.active) KhataMintSoft else KhataCrimsonSoft,
                      shape = RoundedCornerShape(6.dp),
                    ) {
                      Text(
                        if (member.active) strings.activeBadge else strings.deactivatedBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (member.active) KhataEmeraldPaid else KhataCrimsonDue,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      )
                    }
                  }
                }
                if (isMainAdmin && member.uid != currentUserId) {
                  TextButton(onClick = { onToggleOwnerActive(member, !member.active) }) {
                    Text(
                      text = if (member.active) strings.deactivateOwnerBtn else strings.reactivateOwnerBtn,
                      color = if (member.active) KhataCrimsonDue else KhataEmeraldPaid,
                    )
                  }
                }
              }
            }
          }

          // Pending invites
          shop.invitedEmails.forEach { pendingEmail ->
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(pendingEmail, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                if (isMainAdmin) {
                  TextButton(onClick = { onRemoveInvite(pendingEmail) }) {
                    Text(strings.removeInviteBtn, color = KhataCrimsonDue)
                  }
                }
              }
            }
          }

          if (isMainAdmin && totalReservedSlots < 3) {
            HorizontalDivider()
            OutlinedTextField(
              value = inviteEmail,
              onValueChange = { inviteEmail = it },
              label = { Text(strings.inviteOwnerLabel) },
              placeholder = { Text(strings.inviteOwnerHint) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("invite_owner_email_input"),
            )
            Button(
              onClick = {
                onInviteOwner(inviteEmail)
                inviteEmail = ""
              },
              enabled = !isSubmitting && inviteEmail.contains("@"),
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("send_invite_btn"),
            ) {
              Icon(Icons.Filled.PersonAdd, contentDescription = null)
              Spacer(Modifier.width(8.dp))
              Text(strings.sendInviteBtn)
            }
          }
        }
      }
    }

    // FCM / Background Push Documentation Card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(strings.fcmSetupGuideTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          Text(
            strings.fcmSetupGuideBody,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    // Sign Out
    item {
      OutlinedButton(
        onClick = onSignOut,
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("settings_sign_out_btn"),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = KhataCrimsonDue),
      ) {
        Icon(Icons.Outlined.Logout, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(
          text = if (isDemoMode) strings.exitDemoSignInBtn else strings.signOutBtn,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}
