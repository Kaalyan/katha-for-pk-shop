package com.example.util

import com.example.model.AppLanguage
import com.google.firebase.Timestamp
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object CurrencyAndDateUtils {
  private val indianLocale = Locale("en", "IN")

  fun formatRupees(paise: Long): String {
    val isNegative = paise < 0
    val absPaise = kotlin.math.abs(paise)
    val rupees = absPaise / 100
    val remPaise = (absPaise % 100).toInt()
    val formattedInt = formatIndianGrouping(rupees)
    val prefix = if (isNegative) "-₹" else "₹"
    return if (remPaise == 0) {
      "$prefix$formattedInt"
    } else {
      "$prefix$formattedInt.${remPaise.toString().padStart(2, '0')}"
    }
  }

  private fun formatIndianGrouping(value: Long): String {
    val digits = value.toString()
    if (digits.length <= 3) return digits
    val lastThree = digits.takeLast(3)
    val remaining = digits.dropLast(3)
    val groupedPrefix = remaining.reversed().chunked(2).joinToString(",").reversed()
    return "$groupedPrefix,$lastThree"
  }

  /**
   * Parses decimal rupee string like "500" or "500.50" into exact integer paise
   * without floating-point rounding drift.
   */
  fun parseRupeesToPaise(input: String): Long? {
    val cleaned = input.trim().replace(",", "").replace("₹", "")
    if (cleaned.isEmpty()) return null
    return try {
      val bd = BigDecimal(cleaned)
      if (bd.scale() > 2) return null
      val paise = bd.multiply(BigDecimal(100)).longValueExact()
      if (paise < 0) null else paise
    } catch (_: Exception) {
      null
    }
  }

  fun formatDateTime(timestamp: Timestamp?, language: AppLanguage): String {
    val date = timestamp?.toDate() ?: return if (language == AppLanguage.TE) "ఇప్పుడే" else "Just now"
    val locale = if (language == AppLanguage.TE) Locale("te", "IN") else indianLocale
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", locale)
    return sdf.format(date)
  }

  fun formatShortDate(timestamp: Timestamp?, language: AppLanguage): String {
    val date = timestamp?.toDate() ?: return if (language == AppLanguage.TE) "ఈరోజు" else "Today"
    val locale = if (language == AppLanguage.TE) Locale("te", "IN") else indianLocale
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", locale)
    return sdf.format(date)
  }

  fun formatDateOnly(millis: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", indianLocale)
    return sdf.format(java.util.Date(millis))
  }

  fun isToday(timestamp: Timestamp?): Boolean {
    val date = timestamp?.toDate() ?: return true
    val target = Calendar.getInstance().apply { time = date }
    val now = Calendar.getInstance()
    return target.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
      target.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
  }

  fun isYesterday(timestamp: Timestamp?): Boolean {
    val date = timestamp?.toDate() ?: return false
    val target = Calendar.getInstance().apply { time = date }
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return target.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
      target.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
  }

  fun isWithinLastDays(timestamp: Timestamp?, days: Int): Boolean {
    val date = timestamp?.toDate() ?: return true
    val cutoff = Calendar.getInstance().apply {
      add(Calendar.DAY_OF_YEAR, -(days - 1))
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return date.time >= cutoff
  }

  fun isThisMonth(timestamp: Timestamp?): Boolean {
    val date = timestamp?.toDate() ?: return true
    val target = Calendar.getInstance().apply { time = date }
    val now = Calendar.getInstance()
    return target.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
      target.get(Calendar.MONTH) == now.get(Calendar.MONTH)
  }

  fun isBetweenDates(timestamp: Timestamp?, startMillis: Long, endMillis: Long): Boolean {
    val timeMs = timestamp?.toDate()?.time ?: return false
    val startCal = Calendar.getInstance().apply {
      timeInMillis = startMillis
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val endCal = Calendar.getInstance().apply {
      timeInMillis = endMillis
      set(Calendar.HOUR_OF_DAY, 23)
      set(Calendar.MINUTE, 59)
      set(Calendar.SECOND, 59)
      set(Calendar.MILLISECOND, 999)
    }
    return timeMs in startCal.timeInMillis..endCal.timeInMillis
  }
}
