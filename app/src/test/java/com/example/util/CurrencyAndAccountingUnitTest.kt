package com.example.util

import com.example.localization.stringsFor
import com.example.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyAndAccountingUnitTest {

  @Test
  fun parseRupeesToPaise_exactBigDecimalParsing() {
    assertEquals(50000L, CurrencyAndDateUtils.parseRupeesToPaise("500"))
    assertEquals(50050L, CurrencyAndDateUtils.parseRupeesToPaise("500.50"))
    assertEquals(12500000L, CurrencyAndDateUtils.parseRupeesToPaise("1,25,000"))
    assertNull(CurrencyAndDateUtils.parseRupeesToPaise("-10"))
    assertNull(CurrencyAndDateUtils.parseRupeesToPaise("10.123"))
    assertNull(CurrencyAndDateUtils.parseRupeesToPaise("abc"))
  }

  @Test
  fun formatRupees_indianGrouping() {
    assertEquals("₹0", CurrencyAndDateUtils.formatRupees(0L))
    assertEquals("₹500", CurrencyAndDateUtils.formatRupees(50000L))
    assertEquals("₹500.50", CurrencyAndDateUtils.formatRupees(50050L))
    assertEquals("₹1,25,000", CurrencyAndDateUtils.formatRupees(12500000L))
  }

  @Test
  fun bilingualStrings_provideBothTeluguAndEnglish() {
    val en = stringsFor(AppLanguage.EN)
    val te = stringsFor(AppLanguage.TE)
    assertTrue(en.receivePaymentBtn.isNotBlank())
    assertTrue(te.receivePaymentBtn.isNotBlank())
    assertTrue(en.navHome != te.navHome)
    assertTrue(te.udharSuccessTemplate("రవి", "₹500").contains("₹500"))
  }
}
