package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.localization.stringsFor
import com.example.model.AppLanguage
import com.example.model.Customer
import com.example.model.LedgerTransaction
import com.example.model.PaymentRecord
import java.io.File
import java.io.FileOutputStream

object ReportExporter {

  private fun reportsDir(context: Context): File {
    val dir = File(context.cacheDir, "reports")
    if (!dir.exists()) dir.mkdirs()
    return dir
  }

  private fun uriForFile(context: Context, file: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

  fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
    val uri = uriForFile(context, file)
    val intent =
      Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
  }

  fun exportShopReportCsv(
    context: Context,
    shopName: String,
    customers: List<Customer>,
    transactions: List<LedgerTransaction>,
  ): File {
    val file = File(reportsDir(context), "kirana_khata_report_${System.currentTimeMillis()}.csv")
    val sb = StringBuilder()
    sb.appendLine("Shop,Generated At")
    sb.appendLine("\"${escapeCsv(shopName)}\",\"${CurrencyAndDateUtils.formatDateOnly(System.currentTimeMillis())}\"")
    sb.appendLine()
    sb.appendLine("Customer Name,Mobile,Opening Balance (INR),Total Credit (INR),Total Paid (INR),Outstanding Balance (INR)")
    customers.forEach { c ->
      sb.appendLine(
        listOf(
          escapeCsv(c.name),
          escapeCsv(c.phone),
          paiseToDecimal(c.openingBalancePaise),
          paiseToDecimal(c.totalCreditPaise),
          paiseToDecimal(c.totalPaidPaise),
          paiseToDecimal(c.outstandingBalancePaise),
        ).joinToString(",") { "\"$it\"" }
      )
    }
    sb.appendLine()
    sb.appendLine("Date,Customer,Type,Method,Amount (INR),Balance After (INR),Status,Owner,Description")
    transactions.forEach { tx ->
      sb.appendLine(
        listOf(
          CurrencyAndDateUtils.formatDateTime(tx.createdAt, AppLanguage.EN),
          escapeCsv(tx.customerName),
          tx.type,
          tx.paymentMethod ?: "-",
          paiseToDecimal(tx.amountPaise),
          paiseToDecimal(tx.balanceAfterPaise),
          tx.status,
          escapeCsv(tx.createdByName),
          escapeCsv(tx.description),
        ).joinToString(",") { "\"$it\"" }
      )
    }
    file.writeText(sb.toString(), Charsets.UTF_8)
    return file
  }

  fun exportCustomerStatementCsv(
    context: Context,
    shopName: String,
    customer: Customer,
    transactions: List<LedgerTransaction>,
  ): File {
    val safeName = customer.name.replace(Regex("[^a-zA-Z0-9_]"), "_").take(24)
    val file = File(reportsDir(context), "khata_${safeName}_${System.currentTimeMillis()}.csv")
    val sb = StringBuilder()
    sb.appendLine("Shop,Customer,Mobile,Opening Balance,Total Credit,Total Paid,Remaining Balance")
    sb.appendLine(
      listOf(
        escapeCsv(shopName),
        escapeCsv(customer.name),
        escapeCsv(customer.phone),
        paiseToDecimal(customer.openingBalancePaise),
        paiseToDecimal(customer.totalCreditPaise),
        paiseToDecimal(customer.totalPaidPaise),
        paiseToDecimal(customer.outstandingBalancePaise),
      ).joinToString(",") { "\"$it\"" }
    )
    sb.appendLine()
    sb.appendLine("Date,Description,Credit (INR),Payment (INR),Balance (INR),Status,Owner")
    transactions.sortedBy { it.createdAt?.toDate()?.time ?: 0L }.forEach { tx ->
      val creditStr = if (tx.type == "CREDIT") paiseToDecimal(tx.amountPaise) else ""
      val paidStr = if (tx.type == "PAYMENT") paiseToDecimal(tx.amountPaise) else ""
      sb.appendLine(
        listOf(
          CurrencyAndDateUtils.formatDateTime(tx.createdAt, AppLanguage.EN),
          escapeCsv(tx.description),
          creditStr,
          paidStr,
          paiseToDecimal(tx.balanceAfterPaise),
          tx.status,
          escapeCsv(tx.createdByName),
        ).joinToString(",") { "\"$it\"" }
      )
    }
    file.writeText(sb.toString(), Charsets.UTF_8)
    return file
  }

  internal fun buildShopReportLines(
    shopName: String,
    language: AppLanguage,
    customers: List<Customer>,
    todayCreditPaise: Long,
    todayPaymentPaise: Long,
    monthCreditPaise: Long,
    monthPaymentPaise: Long,
    totalOutstandingPaise: Long,
  ): List<String> {
    val s = stringsFor(language)
    val lines = mutableListOf<String>()
    lines += "${s.appTitle} — $shopName"
    lines += "${s.reportsTitle} (${CurrencyAndDateUtils.formatDateOnly(System.currentTimeMillis())})"
    lines += "------------------------------------------------------------"
    lines += "${s.totalCustomers}: ${customers.size}"
    lines += "${s.totalOutstanding}: ${CurrencyAndDateUtils.formatRupees(totalOutstandingPaise)}"
    lines += "${s.todayNewCredit}: ${CurrencyAndDateUtils.formatRupees(todayCreditPaise)}"
    lines += "${s.todayReceived}: ${CurrencyAndDateUtils.formatRupees(todayPaymentPaise)}"
    lines += "${s.monthlyCredit}: ${CurrencyAndDateUtils.formatRupees(monthCreditPaise)}"
    lines += "${s.monthlyPayments}: ${CurrencyAndDateUtils.formatRupees(monthPaymentPaise)}"
    lines += ""
    lines += s.customerWiseOutstanding
    lines += "------------------------------------------------------------"
    customers.sortedByDescending { it.outstandingBalancePaise }.forEach { c ->
      lines += "${c.name} (${c.phone})  |  ${s.remainingBalance}: ${CurrencyAndDateUtils.formatRupees(c.outstandingBalancePaise)}"
    }
    return lines
  }

  fun exportShopReportPdf(
    context: Context,
    shopName: String,
    language: AppLanguage,
    customers: List<Customer>,
    todayCreditPaise: Long,
    todayPaymentPaise: Long,
    monthCreditPaise: Long,
    monthPaymentPaise: Long,
    totalOutstandingPaise: Long,
  ): File {
    val lines =
      buildShopReportLines(
        shopName = shopName,
        language = language,
        customers = customers,
        todayCreditPaise = todayCreditPaise,
        todayPaymentPaise = todayPaymentPaise,
        monthCreditPaise = monthCreditPaise,
        monthPaymentPaise = monthPaymentPaise,
        totalOutstandingPaise = totalOutstandingPaise,
      )
    val file = File(reportsDir(context), "kirana_report_${System.currentTimeMillis()}.pdf")
    writePdfLines(file, lines)
    return file
  }

  internal fun buildCustomerStatementLines(
    shopName: String,
    language: AppLanguage,
    customer: Customer,
    transactions: List<LedgerTransaction>,
  ): List<String> {
    val s = stringsFor(language)
    val lines = mutableListOf<String>()
    lines += "$shopName — ${s.customerKhataTitle}"
    lines += "${s.customerNameLabel}: ${customer.name} (${customer.phone})"
    if (!customer.address.isNullOrBlank()) {
      lines += "${s.addressOptionalLabel}: ${customer.address}"
    }
    lines += "------------------------------------------------------------"
    lines += "${s.openingBalance}: ${CurrencyAndDateUtils.formatRupees(customer.openingBalancePaise)}"
    lines += "${s.totalCredit}: ${CurrencyAndDateUtils.formatRupees(customer.totalCreditPaise)}"
    lines += "${s.totalPaid}: ${CurrencyAndDateUtils.formatRupees(customer.totalPaidPaise)}"
    lines += "${s.remainingBalance}: ${CurrencyAndDateUtils.formatRupees(customer.outstandingBalancePaise)}"
    lines += "------------------------------------------------------------"
    lines += "${s.dateCol} | ${s.descriptionCol} | ${s.creditCol}/${s.paymentCol} | ${s.balanceCol}"
    transactions.sortedBy { it.createdAt?.toDate()?.time ?: 0L }.forEach { tx ->
      val dateStr = CurrencyAndDateUtils.formatShortDate(tx.createdAt, language)
      val typeTag = when (tx.type) {
        "CREDIT" -> s.creditCol
        "PAYMENT" -> s.paymentCol
        else -> s.filterReversal
      }
      val statusTag = if (tx.status == "REVERSED") " [${s.statusReversed}]" else ""
      lines += "$dateStr | ${tx.description}$statusTag | $typeTag ${CurrencyAndDateUtils.formatRupees(tx.amountPaise)} | ${s.balanceCol}: ${CurrencyAndDateUtils.formatRupees(tx.balanceAfterPaise)} (${tx.createdByName})"
    }
    return lines
  }

  fun exportCustomerStatementPdf(
    context: Context,
    shopName: String,
    language: AppLanguage,
    customer: Customer,
    transactions: List<LedgerTransaction>,
  ): File {
    val lines = buildCustomerStatementLines(shopName, language, customer, transactions)
    val file = File(reportsDir(context), "statement_${System.currentTimeMillis()}.pdf")
    writePdfLines(file, lines)
    return file
  }

  internal fun buildPaymentReceiptLines(
    shopName: String,
    language: AppLanguage,
    payment: PaymentRecord,
  ): List<String> {
    val s = stringsFor(language)
    return listOf(
      shopName,
      s.paymentReceiptTitle,
      "------------------------------------------------------------",
      "${s.receiptNumberLabel}: ${payment.receiptNumber}",
      "${s.paidByLabel}: ${payment.customerName}",
      "${s.amountToReceiveLabel}: ${CurrencyAndDateUtils.formatRupees(payment.amountPaise)}",
      "${s.paymentMethodLabel}: ${payment.method}",
      "${s.balanceBeforeLabel}: ${CurrencyAndDateUtils.formatRupees(payment.balanceBeforePaise ?: 0L)}",
      "${s.balanceAfterLabel}: ${CurrencyAndDateUtils.formatRupees(payment.balanceAfterPaise ?: 0L)}",
      "${s.receivedByLabel}: ${payment.receivedByName}",
      "${s.dateTimeLabel}: ${CurrencyAndDateUtils.formatDateTime(payment.createdAt, language)}",
      "------------------------------------------------------------",
      s.manualPaymentDisclaimer,
    )
  }

  fun exportPaymentReceiptPdf(
    context: Context,
    shopName: String,
    language: AppLanguage,
    payment: PaymentRecord,
  ): File {
    val lines = buildPaymentReceiptLines(shopName, language, payment)
    val file = File(reportsDir(context), "receipt_${payment.receiptNumber}.pdf")
    writePdfLines(file, lines)
    return file
  }

  private fun writePdfLines(file: File, lines: List<String>) {
    val pageWidth = 595
    val pageHeight = 842
    val margin = 36
    val contentWidth = pageWidth - margin * 2
    val document = PdfDocument()

    val titlePaint = TextPaint().apply {
      color = Color.parseColor("#14532D")
      textSize = 15f
      isFakeBoldText = true
      isAntiAlias = true
    }
    val bodyPaint = TextPaint().apply {
      color = Color.parseColor("#1F2937")
      textSize = 11f
      isAntiAlias = true
    }

    var pageNum = 1
    var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
    var canvas: Canvas = page.canvas
    var y = margin.toFloat()

    lines.forEachIndexed { idx, line ->
      val paint = if (idx == 0) titlePaint else bodyPaint
      val layout = StaticLayout.Builder.obtain(line, 0, line.length, paint, contentWidth)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(2f, 1.15f)
        .build()

      if (y + layout.height > pageHeight - margin) {
        document.finishPage(page)
        pageNum++
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        canvas = page.canvas
        y = margin.toFloat()
      }

      canvas.save()
      canvas.translate(margin.toFloat(), y)
      layout.draw(canvas)
      canvas.restore()
      y += layout.height + 6f
    }

    document.finishPage(page)
    FileOutputStream(file).use { out ->
      document.writeTo(out)
    }
    document.close()
  }

  private fun escapeCsv(value: String): String = value.replace("\"", "\"\"")

  private fun paiseToDecimal(paise: Long): String {
    val rupees = paise / 100
    val rem = kotlin.math.abs(paise % 100)
    return "$rupees.${rem.toString().padStart(2, '0')}"
  }
}
