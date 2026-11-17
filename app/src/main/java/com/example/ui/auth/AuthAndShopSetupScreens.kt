package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.localization.stringsFor
import com.example.model.AppLanguage
import com.example.model.Shop
import com.example.ui.components.LanguageTogglePill
import com.example.ui.theme.KhataAmberSoft
import com.example.ui.theme.KhataEmeraldPaid
import com.example.ui.theme.KhataInkPrimary
import com.example.ui.theme.KhataMintSoft
import com.example.ui.theme.KhataSaffronGold
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope,
) {
  if (Firebase.auth.currentUser != null) {
    onAuthSuccess()
    return
  }
  val clientId =
    try {
      context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
      onUnauthenticated()
      return
    }

  val googleIdOption =
    GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()

  val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(context, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onUnauthenticated()
      }
    } catch (_: Exception) {
      onUnauthenticated()
    }
  }
}

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {},
) {
  val clientId =
    try {
      context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
      onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
      return
    }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

  scope.launch {
    try {
      val activityContext = context as? Activity ?: context
      val result = credentialManager.getCredential(activityContext, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onAuthError("Unexpected credential type")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
      onAuthCancelled()
    } catch (e: Exception) {
      Log.e("Auth", "Google Sign-In failed", e)
      onAuthError(e.localizedMessage ?: "Sign in failed")
    }
  }
}

fun signOutUser(
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  onSignOutComplete: () -> Unit = {},
) {
  scope.launch {
    withContext(NonCancellable) {
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.w("Auth", "Failed to clear credential state: ${e.message}")
      } finally {
        Firebase.auth.signOut()
        onSignOutComplete()
      }
    }
  }
}

@Composable
fun AuthScreen(
  language: AppLanguage,
  onLanguageChange: (AppLanguage) -> Unit,
  onAuthSuccess: () -> Unit,
  onTryDemoClick: (() -> Unit)? = null,
  onTryCustomerPreviewClick: (() -> Unit)? = null,
) {
  val s = stringsFor(language)
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 20.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier =
          Modifier.widthIn(max = 520.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.AccountBalanceWallet,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = s.appTitle,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold,
            )
          }
          LanguageTogglePill(language = language, onLanguageChange = onLanguageChange)
        }

        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Image(
              painter = painterResource(id = R.drawable.img_khata_hero),
              contentDescription = s.signInTitle,
              modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(14.dp)),
              contentScale = ContentScale.Crop,
            )

            Text(
              text = s.signInTitle,
              style = MaterialTheme.typography.headlineMedium,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = s.signInSubtitle,
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Surface(
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
              shape = RoundedCornerShape(12.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Lock,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = s.signInSecurityNote,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
              }
            }

            if (errorMsg != null) {
              Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(10.dp),
              ) {
                Text(
                  text = errorMsg!!,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                  modifier = Modifier.fillMaxWidth().padding(12.dp),
                )
              }
            }

            Button(
              onClick = {
                errorMsg = null
                isLoading = true
                onGoogleSignInClicked(
                  context = context,
                  credentialManager = credentialManager,
                  onAuthSuccess = {
                    isLoading = false
                    onAuthSuccess()
                  },
                  onAuthError = { msg ->
                    isLoading = false
                    errorMsg = msg
                  },
                  scope = scope,
                  onAuthCancelled = { isLoading = false },
                )
              },
              enabled = !isLoading,
              modifier = Modifier.fillMaxWidth().height(54.dp).testTag("google_sign_in_button"),
              shape = RoundedCornerShape(14.dp),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
              if (isLoading) {
                CircularProgressIndicator(
                  modifier = Modifier.size(22.dp),
                  color = MaterialTheme.colorScheme.onPrimary,
                  strokeWidth = 2.dp,
                )
              } else {
                Text(
                  text = s.signInWithGoogle,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
              }
            }

            if (onTryDemoClick != null) {
              OutlinedButton(
                onClick = onTryDemoClick,
                enabled = !isLoading,
                modifier =
                  Modifier.fillMaxWidth()
                    .height(56.dp)
                    .testTag("demo_mode_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, KhataSaffronGold),
                colors =
                  ButtonDefaults.outlinedButtonColors(
                    containerColor = KhataAmberSoft.copy(alpha = 0.45f),
                    contentColor = KhataInkPrimary,
                  ),
              ) {
                Icon(
                  imageVector = Icons.Filled.Visibility,
                  contentDescription = null,
                  tint = KhataSaffronGold,
                  modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                  text = s.tryDemoModeBtn,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center,
                )
              }
              Text(
                text = s.tryDemoModeHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
              )
            }

            if (onTryCustomerPreviewClick != null) {
              OutlinedButton(
                onClick = onTryCustomerPreviewClick,
                enabled = !isLoading,
                modifier =
                  Modifier.fillMaxWidth()
                    .height(54.dp)
                    .testTag("customer_preview_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, KhataEmeraldPaid),
                colors =
                  ButtonDefaults.outlinedButtonColors(
                    containerColor = KhataMintSoft.copy(alpha = 0.55f),
                    contentColor = KhataInkPrimary,
                  ),
              ) {
                Icon(
                  imageVector = Icons.Filled.Person,
                  contentDescription = null,
                  tint = KhataEmeraldPaid,
                  modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                  text = s.previewCustomerViewBtn,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun ShopOnboardingScreen(
  language: AppLanguage,
  onLanguageChange: (AppLanguage) -> Unit,
  userEmail: String,
  defaultOwnerName: String,
  pendingInvites: List<Shop>,
  isSubmitting: Boolean,
  onCreateShop: (shopName: String, ownerName: String, phone: String?) -> Unit,
  onAcceptInvite: (shopId: String, ownerName: String, phone: String?) -> Unit,
  onSignOut: () -> Unit,
) {
  val s = stringsFor(language)
  var shopName by remember { mutableStateOf("") }
  var ownerName by remember(defaultOwnerName) { mutableStateOf(defaultOwnerName) }
  var ownerPhone by remember { mutableStateOf("") }

  Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier =
          Modifier.widthIn(max = 580.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = s.shopSetupTitle,
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = userEmail,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          LanguageTogglePill(language = language, onLanguageChange = onLanguageChange)
        }

        Text(
          text = s.shopSetupSubtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Pending invitations section
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.GroupAdd,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = s.pendingInvitesTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
            }
            if (pendingInvites.isEmpty()) {
              Text(
                text = s.noInvitesHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            } else {
              pendingInvites.forEach { invitedShop ->
                Surface(
                  color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                  shape = RoundedCornerShape(12.dp),
                ) {
                  Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Text(
                      text = invitedShop.name,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = invitedShop.mainOwnerEmail,
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                      onClick = {
                        onAcceptInvite(
                          invitedShop.id,
                          ownerName.ifBlank { defaultOwnerName },
                          ownerPhone.takeIf { it.isNotBlank() },
                        )
                      },
                      enabled = !isSubmitting,
                      modifier = Modifier.fillMaxWidth().height(48.dp).testTag("accept_invite_button"),
                    ) {
                      Text(s.acceptInviteButton)
                    }
                  }
                }
              }
            }
          }
        }

        // Create new shop section (Owner 1 - Main Admin)
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Storefront,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = s.createNewShop,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
            }

            OutlinedTextField(
              value = shopName,
              onValueChange = { shopName = it },
              label = { Text(s.shopNameLabel) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("shop_name_input"),
            )
            OutlinedTextField(
              value = ownerName,
              onValueChange = { ownerName = it },
              label = { Text(s.ownerNameLabel) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("owner_name_input"),
            )
            OutlinedTextField(
              value = ownerPhone,
              onValueChange = { ownerPhone = it },
              label = { Text(s.ownerPhoneLabel) },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              modifier = Modifier.fillMaxWidth(),
            )

            Button(
              onClick = {
                onCreateShop(shopName, ownerName, ownerPhone.takeIf { it.isNotBlank() })
              },
              enabled = !isSubmitting && shopName.trim().length >= 2 && ownerName.trim().isNotEmpty(),
              modifier = Modifier.fillMaxWidth().height(52.dp).testTag("create_shop_button"),
            ) {
              if (isSubmitting) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  color = MaterialTheme.colorScheme.onPrimary,
                  strokeWidth = 2.dp,
                )
              } else {
                Text(s.createShopButton, style = MaterialTheme.typography.titleMedium)
              }
            }
          }
        }

        OutlinedButton(
          onClick = onSignOut,
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("onboarding_sign_out"),
        ) {
          Icon(imageVector = Icons.Outlined.Logout, contentDescription = null)
          Spacer(Modifier.size(8.dp))
          Text(s.switchAccount)
        }
      }
    }
  }
}
