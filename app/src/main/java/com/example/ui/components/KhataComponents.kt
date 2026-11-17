package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.OwnerRole
import com.example.model.PaymentMethod
import com.example.ui.theme.KhataAmberSoft
import com.example.ui.theme.KhataCrimsonDue
import com.example.ui.theme.KhataEmeraldPaid
import com.example.ui.theme.KhataInkPrimary
import com.example.ui.theme.KhataMintSoft
import com.example.ui.theme.KhataSaffronGold

@Composable
fun LanguageTogglePill(
  language: AppLanguage,
  onLanguageChange: (AppLanguage) -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(24.dp))
        .clickable {
          onLanguageChange(if (language == AppLanguage.EN) AppLanguage.TE else AppLanguage.EN)
        }
        .testTag("language_toggle_pill"),
    color = MaterialTheme.colorScheme.secondaryContainer,
    shape = RoundedCornerShape(24.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = Icons.Filled.Translate,
        contentDescription = "Switch Language",
        modifier = Modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.onSecondaryContainer,
      )
      Text(
        text = if (language == AppLanguage.EN) "తెలుగు" else "English",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
fun ConnectivityStatusBar(
  isOnline: Boolean,
  strings: AppStrings,
  modifier: Modifier = Modifier,
) {
  if (!isOnline) {
    Surface(
      modifier = modifier.fillMaxWidth().testTag("offline_status_banner"),
      color = KhataCrimsonDue,
      contentColor = Color.White,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(imageVector = Icons.Filled.CloudOff, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(
          text = strings.offlineBanner,
          style = MaterialTheme.typography.labelLarge,
        )
      }
    }
  }
}

@Composable
fun LiveSyncBadge(isOnline: Boolean, strings: AppStrings) {
  Surface(
    color = if (isOnline) KhataMintSoft else MaterialTheme.colorScheme.errorContainer,
    shape = RoundedCornerShape(16.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = if (isOnline) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
        contentDescription = null,
        modifier = Modifier.size(14.dp),
        tint = if (isOnline) KhataEmeraldPaid else KhataCrimsonDue,
      )
      Text(
        text = if (isOnline) strings.onlineLabel else "Offline",
        style = MaterialTheme.typography.labelSmall,
        color = if (isOnline) KhataEmeraldPaid else KhataCrimsonDue,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
fun StatMetricCard(
  label: String,
  value: String,
  accentColor: Color,
  containerColor: Color = MaterialTheme.colorScheme.surface,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(containerColor = containerColor),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    shape = RoundedCornerShape(16.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        color = accentColor,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun QuickActionCardButton(
  title: String,
  icon: ImageVector,
  containerColor: Color,
  contentColor: Color,
  testTag: String,
  badgeCount: Int = 0,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .height(64.dp)
        .clip(RoundedCornerShape(16.dp))
        .clickable(onClick = onClick)
        .testTag(testTag),
    color = containerColor,
    contentColor = contentColor,
    shape = RoundedCornerShape(16.dp),
    shadowElevation = 2.dp,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp))
      Spacer(Modifier.width(8.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      if (badgeCount > 0) {
        Spacer(Modifier.width(6.dp))
        Box(
          modifier =
            Modifier.size(22.dp)
              .clip(CircleShape)
              .background(KhataCrimsonDue),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (badgeCount > 9) "9+" else badgeCount.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

@Composable
fun PaymentMethodSelector(
  selected: PaymentMethod,
  strings: AppStrings,
  onSelect: (PaymentMethod) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    val options = listOf(
      PaymentMethod.CASH to strings.cashMethod,
      PaymentMethod.UPI to strings.upiMethod,
      PaymentMethod.OTHER to strings.otherMethod,
    )
    options.forEach { (method, label) ->
      val isSelected = selected == method
      Surface(
        modifier =
          Modifier.weight(1f)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect(method) }
            .testTag("payment_method_${method.value.lowercase()}"),
        color =
          if (isSelected) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.surfaceVariant,
        contentColor =
          if (isSelected) MaterialTheme.colorScheme.onPrimary
          else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(12.dp),
      ) {
        Box(
          modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
          )
        }
      }
    }
  }
}

@Composable
fun EmptyLedgerState(
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Image(
      painter = painterResource(id = R.drawable.img_khata_empty),
      contentDescription = title,
      modifier = Modifier.size(160.dp).clip(RoundedCornerShape(16.dp)),
      contentScale = ContentScale.Crop,
    )
    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.primary,
      textAlign = TextAlign.Center,
    )
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
fun DemoModeBanner(
  strings: AppStrings,
  activeRole: OwnerRole,
  onSwitchRole: (OwnerRole) -> Unit,
  onExitDemo: () -> Unit,
  modifier: Modifier = Modifier,
  isCustomerPreview: Boolean = false,
  onSelectCustomerPreview: () -> Unit = {},
) {
  Surface(
    modifier = modifier.fillMaxWidth().testTag("demo_mode_banner"),
    color = KhataAmberSoft,
    contentColor = KhataInkPrimary,
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 6.dp, top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          color = KhataSaffronGold.copy(alpha = 0.16f),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, KhataSaffronGold.copy(alpha = 0.45f)),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Visibility,
              contentDescription = null,
              tint = KhataSaffronGold,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = strings.demoModeBadge,
              style = MaterialTheme.typography.labelMedium,
              color = KhataSaffronGold,
              fontWeight = FontWeight.Bold,
            )
          }
        }

        TextButton(
          onClick = onExitDemo,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("exit_demo_button"),
        ) {
          Icon(
            imageVector = Icons.Outlined.Logout,
            contentDescription = null,
            tint = KhataCrimsonDue,
            modifier = Modifier.size(16.dp),
          )
          Spacer(Modifier.width(4.dp))
          Text(
            text = strings.exitDemoBtn,
            style = MaterialTheme.typography.labelLarge,
            color = KhataCrimsonDue,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      Row(
        modifier =
          Modifier.fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 12.dp, end = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = strings.demoRoleSwitchLabel,
          style = MaterialTheme.typography.labelMedium,
          color = KhataInkPrimary,
          fontWeight = FontWeight.SemiBold,
        )
        val roleOptions =
          listOf(
            Triple(OwnerRole.MAIN_ADMIN, strings.demoMainAdminShort, "demo_role_main_admin"),
            Triple(OwnerRole.OWNER_2, strings.demoOwner2Short, "demo_role_owner_2"),
            Triple(OwnerRole.OWNER_3, strings.demoOwner3Short, "demo_role_owner_3"),
          )
        roleOptions.forEach { (role, label, tag) ->
          val selected = !isCustomerPreview && activeRole == role
          FilterChip(
            selected = selected,
            onClick = { onSwitchRole(role) },
            label = {
              Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
              )
            },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = KhataSaffronGold,
                selectedLabelColor = Color.White,
                containerColor = Color.White.copy(alpha = 0.72f),
                labelColor = KhataInkPrimary,
              ),
            modifier = Modifier.minimumInteractiveComponentSize().testTag(tag),
          )
        }
        FilterChip(
          selected = isCustomerPreview,
          onClick = onSelectCustomerPreview,
          label = {
            Text(
              text = strings.demoCustomerViewShort,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (isCustomerPreview) FontWeight.Bold else FontWeight.Medium,
              maxLines = 1,
            )
          },
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = KhataEmeraldPaid,
              selectedLabelColor = Color.White,
              containerColor = Color.White.copy(alpha = 0.72f),
              labelColor = KhataInkPrimary,
            ),
          modifier = Modifier.minimumInteractiveComponentSize().testTag("demo_role_customer_view"),
        )
      }
      HorizontalDivider(color = KhataSaffronGold.copy(alpha = 0.35f))
    }
  }
}
