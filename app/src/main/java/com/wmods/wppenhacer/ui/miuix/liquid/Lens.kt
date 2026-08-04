// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.wmods.wppenhacer.ui.miuix.liquid

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastCoerceAtMost
import top.yukonga.miuix.kmp.blur.BackdropEffectScope
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.runtimeShaderEffect

/** Rounded-rectangle refraction lens adapted from the compose-miuix demo. */
internal fun BackdropEffectScope.lens(
    refractionHeight: Float,
    refractionAmount: Float,
    depthEffect: Boolean = false,
    chromaticAberration: Float = 0f,
) {
    if (!isRuntimeShaderSupported() || refractionHeight <= 0f || refractionAmount <= 0f) return
    if (padding < refractionAmount) padding = refractionAmount
    val radii = roundedRectCornerRadii() ?: return
    val dispersed = chromaticAberration > 0f
    val scale = downscaleFactor.coerceAtLeast(1).toFloat()
    runtimeShaderEffect(
        key = if (dispersed) "WaEnhancerLiquidLensDispersion" else "WaEnhancerLiquidLens",
        shaderString = if (dispersed) refractionWithDispersionShader else refractionShader,
        uniformShaderName = "content",
    ) {
        setFloatUniform("size", size.width / scale, size.height / scale)
        setFloatUniform("offset", -padding / scale, -padding / scale)
        setFloatUniform("cornerRadii", FloatArray(radii.size) { radii[it] / scale })
        setFloatUniform("refractionHeight", refractionHeight / scale)
        setFloatUniform("refractionAmount", -refractionAmount / scale)
        setFloatUniform("depthEffect", if (depthEffect) 1f else 0f)
        if (dispersed) setFloatUniform("chromaticAberration", chromaticAberration)
    }
}

private fun BackdropEffectScope.roundedRectCornerRadii(): FloatArray? {
    val corners = shape as? CornerBasedShape ?: return null
    val maximum = size.minDimension / 2f
    val ltr = layoutDirection == LayoutDirection.Ltr
    val topLeft = if (ltr) corners.topStart.toPx(size, this) else corners.topEnd.toPx(size, this)
    val topRight = if (ltr) corners.topEnd.toPx(size, this) else corners.topStart.toPx(size, this)
    val bottomRight = if (ltr) corners.bottomEnd.toPx(size, this) else corners.bottomStart.toPx(size, this)
    val bottomLeft = if (ltr) corners.bottomStart.toPx(size, this) else corners.bottomEnd.toPx(size, this)
    return floatArrayOf(topLeft, topRight, bottomRight, bottomLeft).map {
        it.fastCoerceAtMost(maximum)
    }.toFloatArray()
}

private const val roundedRectSdf = """
float radiusAt(float2 coord, float4 radii) {
    if (coord.x >= 0.0) { if (coord.y <= 0.0) return radii.y; else return radii.z; }
    else { if (coord.y <= 0.0) return radii.x; else return radii.w; }
}
float sdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    return length(max(cornerCoord, 0.0)) - radius + min(max(cornerCoord.x, cornerCoord.y), 0.0);
}
float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) return sign(coord) * normalize(max(cornerCoord, 0.0));
    float gradX = step(cornerCoord.y, cornerCoord.x);
    return sign(coord) * float2(gradX, 1.0 - gradX);
}
"""

private const val refractionShader = """
uniform shader content;
uniform float2 size; uniform float2 offset; uniform float4 cornerRadii;
uniform float refractionHeight; uniform float refractionAmount; uniform float depthEffect;
$roundedRectSdf
float circleMap(float x) { return 1.0 - sqrt(1.0 - x * x); }
half4 main(float2 coord) {
    float2 halfSize = size * 0.5; float2 centered = (coord + offset) - halfSize;
    float radius = radiusAt(centered, cornerRadii); float sd = sdRoundedRect(centered, halfSize, radius);
    if (-sd >= refractionHeight) return content.eval(coord); sd = min(sd, 0.0);
    float d = circleMap(1.0 - -sd / refractionHeight) * refractionAmount;
    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
    float2 grad = normalize(gradSdRoundedRect(centered, halfSize, gradRadius) + depthEffect * normalize(centered));
    return content.eval(coord + d * grad);
}
"""

private const val refractionWithDispersionShader = """
uniform shader content;
uniform float2 size; uniform float2 offset; uniform float4 cornerRadii;
uniform float refractionHeight; uniform float refractionAmount; uniform float depthEffect;
uniform float chromaticAberration;
$roundedRectSdf
float circleMap(float x) { return 1.0 - sqrt(1.0 - x * x); }
half4 main(float2 coord) {
    float2 halfSize = size * 0.5; float2 centered = (coord + offset) - halfSize;
    float radius = radiusAt(centered, cornerRadii); float sd = sdRoundedRect(centered, halfSize, radius);
    if (-sd >= refractionHeight) return content.eval(coord); sd = min(sd, 0.0);
    float d = circleMap(1.0 - -sd / refractionHeight) * refractionAmount;
    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
    float2 grad = normalize(gradSdRoundedRect(centered, halfSize, gradRadius) + depthEffect * normalize(centered));
    float2 refracted = coord + d * grad;
    float dispersion = chromaticAberration * ((centered.x * centered.y) / (halfSize.x * halfSize.y));
    float2 delta = d * grad * dispersion; half4 color = half4(0.0);
    half4 red = content.eval(refracted + delta); color.r += red.r / 3.5; color.a += red.a / 7.0;
    half4 orange = content.eval(refracted + delta * 0.6667); color.r += orange.r / 3.5; color.g += orange.g / 7.0; color.a += orange.a / 7.0;
    half4 yellow = content.eval(refracted + delta * 0.3333); color.r += yellow.r / 3.5; color.g += yellow.g / 3.5; color.a += yellow.a / 7.0;
    half4 green = content.eval(refracted); color.g += green.g / 3.5; color.a += green.a / 7.0;
    half4 cyan = content.eval(refracted - delta * 0.3333); color.g += cyan.g / 3.5; color.b += cyan.b / 3.0; color.a += cyan.a / 7.0;
    half4 blue = content.eval(refracted - delta * 0.6667); color.b += blue.b / 3.0; color.a += blue.a / 7.0;
    half4 purple = content.eval(refracted - delta); color.r += purple.r / 7.0; color.b += purple.b / 3.0; color.a += purple.a / 7.0;
    return color;
}
"""
