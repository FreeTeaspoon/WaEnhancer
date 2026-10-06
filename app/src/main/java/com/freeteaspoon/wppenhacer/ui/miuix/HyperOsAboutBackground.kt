/* Adapted from Language Selector 1220669f, Apache-2.0. */
package com.freeteaspoon.wppenhacer.ui.miuix

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.freeteaspoon.wppenhacer.ui.miuix.hyperos.BgEffectConfig
import com.freeteaspoon.wppenhacer.ui.miuix.hyperos.BgEffectPainter
import com.freeteaspoon.wppenhacer.ui.miuix.hyperos.DeviceType
import com.freeteaspoon.wppenhacer.ui.miuix.hyperos.bgEffectDraw
import kotlin.math.floor

@Composable
fun HyperOsAboutBackground(
    modifier: Modifier = Modifier,
    backdropModifier: Modifier = Modifier,
    dynamicBackground: Boolean = true,
    alpha: () -> Float = { 1f },
    content: @Composable BoxScope.() -> Unit
) {
    val shadersSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        isRuntimeShaderSupported() &&
        isRenderEffectSupported()
    if (!shadersSupported) {
        Box(modifier = modifier, content = content)
        return
    }
    Box(modifier = modifier) {
        Os3ShaderBackground(
            dynamicBackground = dynamicBackground,
            backdropModifier = backdropModifier,
            alpha = alpha
        )
        content()
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun Os3ShaderBackground(
    dynamicBackground: Boolean,
    backdropModifier: Modifier,
    alpha: () -> Float
) {
    val isDark = LocalManagerDarkMode.current
    val surface = MiuixTheme.colorScheme.surface
    val painter = remember { BgEffectPainter(isOs3 = true) }
    val preset = remember(isDark) {
        BgEffectConfig.get(DeviceType.PHONE, isDark = isDark, isOs3 = true)
    }
    val colorStage = remember { Animatable(0f) }
    val currentAlpha by rememberUpdatedState(alpha)

    LaunchedEffect(dynamicBackground, preset) {
        if (!dynamicBackground) return@LaunchedEffect
        var targetStage = floor(colorStage.value) + 1f
        while (isActive) {
            snapshotFlow { currentAlpha() > 0f }.first { it }
            delay((preset.colorInterpPeriod * 500).toLong())
            colorStage.animateTo(
                targetValue = targetStage,
                animationSpec = spring(dampingRatio = 0.9f, stiffness = 35f)
            )
            targetStage += 1f
        }
    }

    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .then(backdropModifier)
            .bgEffectDraw(
                painter = painter,
                preset = preset,
                deviceType = DeviceType.PHONE,
                isDarkTheme = isDark,
                surface = surface,
                effectBackground = true,
                isFullSize = true,
                playing = dynamicBackground,
                colorStage = { colorStage.value },
                alpha = alpha
            )
    )
}
