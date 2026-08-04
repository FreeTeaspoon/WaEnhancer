/*
 * Home composition adapted from Ujhhgtg/WeKit's HomePager.
 * Copyright its contributors; GPL-3.0-or-later.
 */
package com.wmods.wppenhacer.ui.miuix

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wmods.wppenhacer.BuildConfig
import com.wmods.wppenhacer.R
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource

@Composable
internal fun WeKitStyleHomeDashboard(
    moduleActive: Boolean,
    enabledCount: Int,
    totalCount: Int,
    whatsappVersion: String?,
    businessVersion: String?,
    onOpenFeatures: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val activeColor = when {
                MiuixTheme.isDynamicColor -> MiuixTheme.colorScheme.secondaryContainer
                isSystemInDarkTheme() -> Color(0xFF1A3825)
                else -> Color(0xFFDFFAE4)
            }
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                colors = CardDefaults.defaultColors(color = activeColor),
                showIndication = true,
                pressFeedbackType = PressFeedbackType.Tilt,
                onClick = onOpenFeatures,
            ) {
                Box(Modifier.fillMaxSize()) {
                    if (moduleActive) {
                        Icon(
                            painter = painterResource(R.drawable.ic_round_check_circle_24),
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.BottomEnd).offset(38.dp, 45.dp).size(170.dp),
                            tint = Color(0xFF36D167),
                        )
                    } else {
                        Icon(
                            imageVector = MiuixIcons.Info,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.BottomEnd).offset(38.dp, 45.dp).size(170.dp),
                            tint = MiuixTheme.colorScheme.error,
                        )
                    }
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = if (moduleActive) stringResource(R.string.manager_activated)
                            else stringResource(R.string.manager_not_activated),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(BuildConfig.VERSION_NAME, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                CountCard(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    label = stringResource(R.string.manager_enabled_features),
                    value = enabledCount.toString(),
                    onClick = onOpenFeatures,
                )
                Spacer(Modifier.height(12.dp))
                CountCard(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    label = stringResource(R.string.manager_all_features),
                    value = totalCount.toString(),
                    onClick = onOpenFeatures,
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                HomeInfoText(stringResource(R.string.manager_module_version), "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                HomeInfoText(stringResource(R.string.manager_device_model), "${Build.MANUFACTURER} ${Build.MODEL}")
                HomeInfoText(stringResource(R.string.manager_android_version), "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                HomeInfoText(
                    stringResource(R.string.manager_whatsapp),
                    whatsappVersion ?: stringResource(R.string.manager_not_running),
                )
                HomeInfoText(
                    stringResource(R.string.manager_whatsapp_business),
                    businessVersion ?: stringResource(R.string.manager_not_running),
                    bottomPadding = 0.dp,
                )
            }
        }
    }
}

@Composable
private fun CountCard(modifier: Modifier, label: String, value: String, onClick: () -> Unit) {
    Card(
        modifier = modifier,
        insideMargin = PaddingValues(16.dp),
        showIndication = true,
        pressFeedbackType = PressFeedbackType.Tilt,
        onClick = onClick,
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(
                text = label,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Text(
                text = value,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun HomeInfoText(title: String, content: String, bottomPadding: androidx.compose.ui.unit.Dp = 24.dp) {
    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    Text(
        text = content,
        fontSize = 14.sp,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(top = 2.dp, bottom = bottomPadding),
    )
}
