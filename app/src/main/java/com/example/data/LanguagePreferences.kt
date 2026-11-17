package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.khataDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "kirana_khata_prefs")

class LanguagePreferences(private val context: Context) {
  private val languageKey = stringPreferencesKey("app_language")
  private val activeShopKey = stringPreferencesKey("active_shop_id")

  val languageFlow: Flow<AppLanguage> =
    context.khataDataStore.data.map { prefs ->
      AppLanguage.fromCode(prefs[languageKey])
    }

  val activeShopIdFlow: Flow<String?> =
    context.khataDataStore.data.map { prefs -> prefs[activeShopKey] }

  suspend fun setLanguage(language: AppLanguage) {
    context.khataDataStore.edit { prefs ->
      prefs[languageKey] = language.code
    }
  }

  suspend fun setActiveShopId(shopId: String?) {
    context.khataDataStore.edit { prefs ->
      if (shopId.isNullOrBlank()) {
        prefs.remove(activeShopKey)
      } else {
        prefs[activeShopKey] = shopId
      }
    }
  }
}
