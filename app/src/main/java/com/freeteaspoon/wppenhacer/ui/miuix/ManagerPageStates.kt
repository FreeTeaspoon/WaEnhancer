/* Shared page geometry follows HyperLPA e613bb84, GPL-3.0. */
package com.freeteaspoon.wppenhacer.ui.miuix

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.freeteaspoon.wppenhacer.R
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

internal enum class ManagerPageStateKind { LOADING, EMPTY, ERROR, CONTENT }

@Composable
internal fun ManagerPageStateHost(
    state: ManagerPageStateKind,
    title: String,
    message: String = "",
    modifier: Modifier = Modifier,
    stateModifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    AnimatedContent(
        targetState = state,
        modifier = modifier,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "manager-page-state",
    ) { target ->
        if (target == ManagerPageStateKind.CONTENT) {
            Box(Modifier.fillMaxSize()) { content() }
        } else {
            Box(
                Modifier.fillMaxSize().then(stateModifier)
                    .padding(horizontal = 28.dp, vertical = if (target == ManagerPageStateKind.LOADING) 56.dp else 0.dp),
                contentAlignment = androidx.compose.ui.BiasAlignment(0f, -0.12f),
            ) {
                Column(Modifier.widthIn(max = 320.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Column(
                        Modifier.semantics(mergeDescendants = true) {
                            contentDescription = listOf(title, message).filter(String::isNotBlank).joinToString(". ")
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (target == ManagerPageStateKind.LOADING) {
                            InfiniteProgressIndicator(size = 20.dp)
                            Spacer(Modifier.height(10.dp))
                            Text(title, style = MiuixTheme.textStyles.body1, fontWeight = FontWeight.Normal, textAlign = TextAlign.Center)
                        } else {
                            Icon(if (target == ManagerPageStateKind.EMPTY) MiuixIcons.Notes else MiuixIcons.Refresh,
                                null, Modifier.size(36.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.7f))
                            Spacer(Modifier.height(18.dp))
                            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                                color = MiuixTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                            if (message.isNotBlank()) {
                                Spacer(Modifier.height(7.dp))
                                Text(message, fontSize = 13.sp, fontWeight = FontWeight.Normal,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary, textAlign = TextAlign.Center)
                            }
                        }
                    }
                    if (target == ManagerPageStateKind.ERROR && onRetry != null) {
                        Spacer(Modifier.height(10.dp))
                        TextButton(stringResource(R.string.manager_try_again), onRetry,
                            colors = ButtonDefaults.textButtonColors(
                                color = androidx.compose.ui.graphics.Color.Transparent,
                                disabledColor = androidx.compose.ui.graphics.Color.Transparent,
                                textColor = MiuixTheme.colorScheme.primary),
                            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            textStyle = MiuixTheme.textStyles.footnote1.copy(fontSize = 13.sp, fontWeight = FontWeight.Normal))
                    }
                }
            }
        }
    }
}

@Composable
internal fun ManagerTipCard(text: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 12.dp, bottom = 6.dp),
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(0.dp),
        pressFeedbackType = PressFeedbackType.None,
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}
