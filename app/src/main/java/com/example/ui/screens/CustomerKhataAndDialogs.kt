package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.Customer
import com.example.model.LedgerTransaction
import com.example.model.PaymentMethod
import com.example.model.PaymentRecord
import com.example.ui.components.PaymentMethodSelector
import com.example.ui.components.StatMetricCard
import com.example.ui.theme.KhataAmberSoft
import com.example.ui.theme.KhataCrimsonDue
import com.example.ui.theme.KhataCrimsonSoft
import com.example.ui.theme.KhataEmeraldPaid
import com.example.ui.theme.KhataForestGreen
import com.example.ui.theme.KhataMintSoft
import com.example.ui.theme.KhataSaffronGold
import com.example.util.CurrencyAndDateUtils
import com.example.util.ReportExporter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerKhataDetailScreen(
  strings: AppStrings,
  language: AppLanguage,
  shopName: String,
  customer: Customer,
  transactions: List<LedgerTransaction>,
  onBack: () -> Unit,
  onEditCustomer: () -> Unit,
  onAddUdhar: () -> Unit,
  onReceivePayment: () -> Unit,
  onReverseTransaction: (LedgerTransaction) -> Unit,
) {
  BackHandler(onBack = onBack)
  val context = LocalContext.current
  val customerTxs = remember(transactions, customer.id) {
    transactions
      .filter { it.customerId == customer.id }
      .sortedBy { it.createdAt?.toDate()?.time ?: Long.MAX_VALUE }
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(customer.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(customer.phone, style = MaterialTheme.typography.bodySmall)
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("khata_detail_back_btn")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = onEditCustomer, modifier = Modifier.testTag("khata_edit_customer_btn")) {
            Icon(Icons.Filled.Edit, contentDescription = strings.editBtn)
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = KhataForestGreen,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White,
          ),
      )
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // 4-Box Ledger Summary
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatMetricCard(
              label = strings.openingBalance,
              value = CurrencyAndDateUtils.formatRupees(customer.openingBalancePaise),
              accentColor = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.weight(1f),
            )
            StatMetricCard(
              label = strings.totalCredit,
              value = CurrencyAndDateUtils.formatRupees(customer.totalCreditPaise),
              accentColor = KhataCrimsonDue,
              modifier = Modifier.weight(1f),
            )
          }
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatMetricCard(
              label = strings.totalPaid,
              value = CurrencyAndDateUtils.formatRupees(customer.totalPaidPaise),
              accentColor = KhataEmeraldPaid,
              modifier = Modifier.weight(1f),
            )
            StatMetricCard(
              label = strings.remainingBalance,
              value = CurrencyAndDateUtils.formatRupees(customer.outstandingBalancePaise),
              accentColor =
                if (customer.outstandingBalancePaise > 0) KhataCrimsonDue else KhataEmeraldPaid,
              containerColor =
                if (customer.outstandingBalancePaise > 0) KhataCrimsonSoft else KhataMintSoft,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      // Big Action Buttons
      item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedButton(
            onClick = onAddUdhar,
            modifier = Modifier.weight(1f).height(52.dp).testTag("detail_add_udhar_btn"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(Icons.Filled.AddCard, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(strings.addUdharBtn, fontWeight = FontWeight.Bold)
          }
          Button(
            onClick = onReceivePayment,
            enabled = customer.outstandingBalancePaise > 0L,
            modifier = Modifier.weight(1f).height(52.dp).testTag("detail_mark_paid_btn"),
            shape = RoundedCornerShape(14.dp),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = KhataEmeraldPaid,
                contentColor = Color.White,
              ),
          ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(strings.markAsPaidBtn, fontWeight = FontWeight.Bold)
          }
        }
      }

      // Statement Export Buttons (PDF + CSV)
      item {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedButton(
            onClick = {
              val file =
                ReportExporter.exportCustomerStatementPdf(
                  context = context,
                  shopName = shopName,
                  language = language,
                  customer = customer,
                  transactions = customerTxs,
                )
              ReportExporter.shareFile(context, file, "application/pdf", strings.downloadStatementPdf)
            },
            modifier = Modifier.weight(1f).height(48.dp).testTag("export_customer_pdf_btn"),
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
                  customer = customer,
                  transactions = customerTxs,
                )
              ReportExporter.shareFile(context, file, "text/csv", strings.shareStatementCsv)
            },
            modifier = Modifier.weight(1f).height(48.dp).testTag("export_customer_csv_btn"),
          ) {
            Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(strings.shareStatementCsv, maxLines = 1)
          }
        }
      }

      // Traditional Khata Ledger Table Header
      item {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "${strings.dateCol} / ${strings.descriptionCol}",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.weight(1.6f),
            )
            Text(
              text = "${strings.creditCol} / ${strings.paymentCol}",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.weight(1f),
            )
            Text(
              text = strings.balanceCol,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
            )
          }
        }
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
          LedgerRowItem(
            tx = tx,
            strings = strings,
            language = language,
            onReverse = { onReverseTransaction(tx) },
          )
        }
      }

      item {
        Text(
          text = strings.deleteBlockedNote,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 8.dp),
        )
      }
    }
  }
}

@Composable
private fun LedgerRowItem(
  tx: LedgerTransaction,
  strings: AppStrings,
  language: AppLanguage,
  onReverse: () -> Unit,
) {
  val isCredit = tx.type == "CREDIT"
  val isPayment = tx.type == "PAYMENT"
  val isReversed = tx.status == "REVERSED"

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    shape = RoundedCornerShape(10.dp),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1.6f)) {
          Text(
            text = tx.description + (tx.quantity?.let { " ($it)" } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text = "${CurrencyAndDateUtils.formatShortDate(tx.createdAt, language)} • ${tx.createdByName}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
          Text(
            text = CurrencyAndDateUtils.formatRupees(tx.amountPaise),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color =
              when {
                isReversed -> MaterialTheme.colorScheme.onSurfaceVariant
                isPayment -> KhataEmeraldPaid
                isCredit -> KhataCrimsonDue
                else -> KhataSaffronGold
              },
          )
          Text(
            text =
              when (tx.type) {
                "CREDIT" -> strings.creditCol
                "PAYMENT" -> strings.paymentCol
                else -> strings.filterReversal
              },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(Modifier.width(10.dp))
        Text(
          text = CurrencyAndDateUtils.formatRupees(tx.balanceAfterPaise),
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
        )
      }

      if (isReversed && !tx.reversalReason.isNullOrBlank()) {
        Text(
          text = "${strings.statusReversed}: ${tx.reversalReason}",
          style = MaterialTheme.typography.labelSmall,
          color = KhataCrimsonDue,
        )
      } else if (!isReversed && (isCredit || isPayment)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = onReverse) {
            Text(strings.reverseEntryBtn, style = MaterialTheme.typography.labelMedium, color = KhataSaffronGold)
          }
        }
      }
    }
  }
}

@Composable
fun AddEditCustomerDialog(
  strings: AppStrings,
  existingCustomers: List<Customer>,
  editingCustomer: Customer?,
  isSubmitting: Boolean,
  onDismiss: () -> Unit,
  onSaveNew: (name: String, phone: String, address: String?, openingPaise: Long) -> Unit,
  onUpdateExisting: (id: String, name: String, phone: String, address: String?) -> Unit,
) {
  var name by remember(editingCustomer) { mutableStateOf(editingCustomer?.name ?: "") }
  var phone by remember(editingCustomer) { mutableStateOf(editingCustomer?.phone ?: "") }
  var address by remember(editingCustomer) { mutableStateOf(editingCustomer?.address ?: "") }
  var openingBalanceText by remember { mutableStateOf("0") }
  var errorText by remember { mutableStateOf<String?>(null) }

  val duplicatePhoneMatch = remember(phone, existingCustomers, editingCustomer) {
    val cleanPhone = phone.trim()
    cleanPhone.length >= 6 &&
      existingCustomers.any { it.phone.trim() == cleanPhone && it.id != editingCustomer?.id }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (editingCustomer == null) strings.addCustomerTitle else strings.editCustomerTitle,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(strings.customerNameLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("customer_name_input"),
        )
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text(strings.mobileNumberLabel) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          modifier = Modifier.fillMaxWidth().testTag("customer_phone_input"),
        )

        if (duplicatePhoneMatch) {
          Surface(color = KhataAmberSoft, shape = RoundedCornerShape(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(Icons.Filled.Warning, contentDescription = null, tint = KhataSaffronGold)
              Text(
                text = strings.duplicatePhoneWarning,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF451A03),
              )
            }
          }
        }

        OutlinedTextField(
          value = address,
          onValueChange = { address = it },
          label = { Text(strings.addressOptionalLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("customer_address_input"),
        )

        if (editingCustomer == null) {
          OutlinedTextField(
            value = openingBalanceText,
            onValueChange = { openingBalanceText = it },
            label = { Text(strings.openingBalanceLabel) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().testTag("customer_opening_input"),
          )
        }

        if (errorText != null) {
          Text(
            text = errorText!!,
            style = MaterialTheme.typography.bodySmall,
            color = KhataCrimsonDue,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.trim().isEmpty() || phone.trim().length < 5) {
            errorText = strings.invalidCustomerError
            return@Button
          }
          if (editingCustomer == null) {
            val paise =
              if (openingBalanceText.isBlank()) 0L
              else CurrencyAndDateUtils.parseRupeesToPaise(openingBalanceText)
            if (paise == null || paise < 0L) {
              errorText = strings.invalidAmountError
              return@Button
            }
            onSaveNew(name, phone, address.takeIf { it.isNotBlank() }, paise)
          } else {
            onUpdateExisting(editingCustomer.id, name, phone, address.takeIf { it.isNotBlank() })
          }
        },
        enabled = !isSubmitting,
        modifier = Modifier.testTag("save_customer_confirm_btn"),
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
          Text(if (editingCustomer == null) strings.saveCustomer else strings.updateCustomer)
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(strings.cancel) }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUdharDialog(
  strings: AppStrings,
  customers: List<Customer>,
  preselectedCustomer: Customer?,
  isSubmitting: Boolean,
  onDismiss: () -> Unit,
  onSaveCredit: (customer: Customer, amountPaise: Long, desc: String, qty: String?, notes: String?) -> Unit,
) {
  var selectedCustomer by remember(preselectedCustomer, customers) {
    mutableStateOf(preselectedCustomer ?: customers.firstOrNull())
  }
  var dropdownExpanded by remember { mutableStateOf(false) }
  var description by remember { mutableStateOf("") }
  var quantity by remember { mutableStateOf("") }
  var amountText by remember { mutableStateOf("") }
  var notes by remember { mutableStateOf("") }
  var errorText by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(strings.addUdharTitle, fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ExposedDropdownMenuBox(
          expanded = dropdownExpanded,
          onExpandedChange = { dropdownExpanded = !dropdownExpanded },
        ) {
          OutlinedTextField(
            value = selectedCustomer?.let { "${it.name} (${it.phone})" } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(strings.selectCustomerLabel) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
            modifier =
              Modifier.fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .testTag("udhar_customer_dropdown"),
          )
          ExposedDropdownMenu(
            expanded = dropdownExpanded,
            onDismissRequest = { dropdownExpanded = false },
          ) {
            customers.forEach { c ->
              DropdownMenuItem(
                text = { Text("${c.name} • ${CurrencyAndDateUtils.formatRupees(c.outstandingBalancePaise)}") },
                onClick = {
                  selectedCustomer = c
                  dropdownExpanded = false
                },
              )
            }
          }
        }

        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it },
          label = { Text(strings.amountRupeesLabel) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth().testTag("udhar_amount_input"),
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text(strings.productDescriptionLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("udhar_description_input"),
        )
        OutlinedTextField(
          value = quantity,
          onValueChange = { quantity = it },
          label = { Text(strings.quantityOptionalLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text(strings.notesOptionalLabel) },
          modifier = Modifier.fillMaxWidth(),
        )

        if (errorText != null) {
          Text(errorText!!, color = KhataCrimsonDue, style = MaterialTheme.typography.bodySmall)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val cust = selectedCustomer
          val amountPaise = CurrencyAndDateUtils.parseRupeesToPaise(amountText)
          when {
            cust == null -> errorText = strings.selectCustomerLabel
            amountPaise == null || amountPaise <= 0L -> errorText = strings.invalidAmountError
            description.trim().isEmpty() -> errorText = strings.productDescriptionLabel
            else ->
              onSaveCredit(
                cust,
                amountPaise,
                description.trim(),
                quantity.takeIf { it.isNotBlank() },
                notes.takeIf { it.isNotBlank() },
              )
          }
        },
        enabled = !isSubmitting && customers.isNotEmpty(),
        modifier = Modifier.testTag("save_udhar_confirm_btn"),
      ) {
        if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        else Text(strings.saveUdharBtn)
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceivePaymentDialog(
  strings: AppStrings,
  customersWithDue: List<Customer>,
  preselectedCustomer: Customer?,
  isSubmitting: Boolean,
  isOnline: Boolean,
  onDismiss: () -> Unit,
  onConfirmPayment: (customer: Customer, amountPaise: Long, method: PaymentMethod, notes: String?, paymentId: String) -> Unit,
) {
  var selectedCustomer by remember(preselectedCustomer, customersWithDue) {
    mutableStateOf(preselectedCustomer ?: customersWithDue.firstOrNull())
  }
  var dropdownExpanded by remember { mutableStateOf(false) }
  var amountText by remember(selectedCustomer) {
    val due = selectedCustomer?.outstandingBalancePaise ?: 0L
    mutableStateOf(if (due > 0L) (due / 100.0).let { if (due % 100L == 0L) (due / 100L).toString() else String.format("%.2f", it) } else "")
  }
  var method by remember { mutableStateOf(PaymentMethod.CASH) }
  var notes by remember { mutableStateOf("") }
  var errorText by remember { mutableStateOf<String?>(null) }

  // Deterministic idempotency key per opened payment confirmation flow
  val paymentAttemptId = remember(selectedCustomer?.id) {
    "pay_${UUID.randomUUID().toString().replace("-", "")}"
  }

  val parsedAmountPaise = CurrencyAndDateUtils.parseRupeesToPaise(amountText) ?: 0L
  val currentDue = selectedCustomer?.outstandingBalancePaise ?: 0L
  val projectedRemaining = (currentDue - parsedAmountPaise).coerceAtLeast(0L)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = KhataEmeraldPaid)
        Text(strings.receivePaymentTitle, fontWeight = FontWeight.Bold, color = KhataEmeraldPaid)
      }
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (customersWithDue.isEmpty()) {
          Text(strings.noDueBalance, style = MaterialTheme.typography.bodyMedium)
        } else {
          ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = { dropdownExpanded = !dropdownExpanded },
          ) {
            OutlinedTextField(
              value = selectedCustomer?.let { "${it.name} (${CurrencyAndDateUtils.formatRupees(it.outstandingBalancePaise)})" } ?: "",
              onValueChange = {},
              readOnly = true,
              label = { Text(strings.selectCustomerLabel) },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
              modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
              expanded = dropdownExpanded,
              onDismissRequest = { dropdownExpanded = false },
            ) {
              customersWithDue.forEach { c ->
                DropdownMenuItem(
                  text = { Text("${c.name} • ${CurrencyAndDateUtils.formatRupees(c.outstandingBalancePaise)}") },
                  onClick = {
                    selectedCustomer = c
                    dropdownExpanded = false
                  },
                )
              }
            }
          }

          Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column {
                Text(strings.outstandingBalanceLabel, style = MaterialTheme.typography.labelSmall)
                Text(
                  text = CurrencyAndDateUtils.formatRupees(currentDue),
                  style = MaterialTheme.typography.titleMedium,
                  color = KhataCrimsonDue,
                  fontWeight = FontWeight.Bold,
                )
              }
              Column(horizontalAlignment = Alignment.End) {
                Text(strings.remainingBalance, style = MaterialTheme.typography.labelSmall)
                Text(
                  text = CurrencyAndDateUtils.formatRupees(projectedRemaining),
                  style = MaterialTheme.typography.titleMedium,
                  color = KhataEmeraldPaid,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedTextField(
              value = amountText,
              onValueChange = { amountText = it },
              label = { Text(strings.amountToReceiveLabel) },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              modifier = Modifier.weight(1f).testTag("payment_amount_input"),
            )
            OutlinedButton(
              onClick = {
                amountText =
                  if (currentDue % 100L == 0L) (currentDue / 100L).toString()
                  else String.format("%.2f", currentDue / 100.0)
              },
              modifier = Modifier.height(54.dp),
            ) {
              Text(strings.fullPaymentChip)
            }
          }

          Text(strings.paymentMethodLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
          PaymentMethodSelector(selected = method, strings = strings, onSelect = { method = it })

          OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(strings.notesOptionalLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )

          Surface(color = KhataAmberSoft, shape = RoundedCornerShape(10.dp)) {
            Text(
              text = strings.manualPaymentDisclaimer,
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF451A03),
              modifier = Modifier.fillMaxWidth().padding(10.dp),
            )
          }

          if (!isOnline) {
            Text(strings.offlineActionBlocked, color = KhataCrimsonDue, style = MaterialTheme.typography.bodySmall)
          }
          if (errorText != null) {
            Text(errorText!!, color = KhataCrimsonDue, style = MaterialTheme.typography.bodySmall)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val cust = selectedCustomer
          val paise = CurrencyAndDateUtils.parseRupeesToPaise(amountText)
          when {
            cust == null -> errorText = strings.selectCustomerLabel
            !isOnline -> errorText = strings.offlineActionBlocked
            paise == null || paise <= 0L -> errorText = strings.invalidAmountError
            paise > cust.outstandingBalancePaise -> errorText = strings.paymentExceedsBalanceError
            else -> onConfirmPayment(cust, paise, method, notes.takeIf { it.isNotBlank() }, paymentAttemptId)
          }
        },
        enabled = !isSubmitting && isOnline && customersWithDue.isNotEmpty(),
        colors = ButtonDefaults.buttonColors(containerColor = KhataEmeraldPaid, contentColor = Color.White),
        modifier = Modifier.testTag("confirm_payment_btn"),
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
          Text(strings.confirmPaymentBtn, fontWeight = FontWeight.Bold)
        }
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } },
  )
}

@Composable
fun PaymentReceiptDialog(
  strings: AppStrings,
  language: AppLanguage,
  shopName: String,
  receipt: PaymentRecord,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = KhataEmeraldPaid)
        Text(strings.paymentReceiptTitle, fontWeight = FontWeight.Bold, color = KhataEmeraldPaid)
      }
    },
    text = {
      Surface(
        color = KhataMintSoft.copy(alpha = 0.45f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, KhataEmeraldPaid.copy(alpha = 0.4f)),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(shopName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          HorizontalDivider()
          ReceiptLine(strings.receiptNumberLabel, receipt.receiptNumber)
          ReceiptLine(strings.paidByLabel, receipt.customerName)
          ReceiptLine(strings.amountToReceiveLabel, CurrencyAndDateUtils.formatRupees(receipt.amountPaise))
          ReceiptLine(strings.paymentMethodLabel, receipt.method)
          ReceiptLine(strings.balanceBeforeLabel, CurrencyAndDateUtils.formatRupees(receipt.balanceBeforePaise ?: 0L))
          ReceiptLine(strings.balanceAfterLabel, CurrencyAndDateUtils.formatRupees(receipt.balanceAfterPaise ?: 0L))
          ReceiptLine(strings.receivedByLabel, receipt.receivedByName)
          ReceiptLine(strings.dateTimeLabel, CurrencyAndDateUtils.formatDateTime(receipt.createdAt, language))
          HorizontalDivider()
          Text(
            text = strings.manualPaymentDisclaimer,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss, modifier = Modifier.testTag("receipt_done_btn")) {
        Text(strings.done)
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = {
          val file = ReportExporter.exportPaymentReceiptPdf(context, shopName, language, receipt)
          ReportExporter.shareFile(context, file, "application/pdf", strings.printShareReceipt)
        },
      ) {
        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(strings.printShareReceipt)
      }
    },
  )
}

@Composable
private fun ReceiptLine(label: String, value: String) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
  }
}

@Composable
fun ReverseTransactionDialog(
  strings: AppStrings,
  transaction: LedgerTransaction,
  isSubmitting: Boolean,
  onDismiss: () -> Unit,
  onConfirmReversal: (reason: String) -> Unit,
) {
  var reason by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(strings.reverseDialogTitle, fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "${transaction.customerName} • ${CurrencyAndDateUtils.formatRupees(transaction.amountPaise)} (${transaction.description})",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = strings.deleteBlockedNote,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = reason,
          onValueChange = { reason = it },
          label = { Text(strings.reverseReasonLabel) },
          modifier = Modifier.fillMaxWidth().testTag("reversal_reason_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirmReversal(reason.trim()) },
        enabled = !isSubmitting && reason.trim().isNotEmpty(),
        colors = ButtonDefaults.buttonColors(containerColor = KhataSaffronGold),
        modifier = Modifier.testTag("confirm_reversal_btn"),
      ) {
        Text(strings.confirmReversalBtn)
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } },
  )
}
