package com.outerjoyn.sdk

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Stripe-inspired appearance configuration for OuterJoyn SDK widgets.
 *
 * Maps 1:1 to CSS custom properties injected into the WebView content.
 */
data class OuterJoynAppearance(
    val colors: OJColors = OJColors(),
    val typography: OJTypography = OJTypography(),
    val shapes: OJShapes = OJShapes()
)

data class OJColors(
    val primary: Color = Color(0xFF6B21A8),
    val accent: Color = Color(0xFFA855F7),
    val background: Color = Color.White,
    val surface: Color = Color(0xFFF5F5F5),
    val text: Color = Color(0xFF111827),
    val textMuted: Color = Color(0xFF6B7280),
    val border: Color = Color(0xFFE5E7EB),
    val success: Color = Color(0xFF16A34A),
    val error: Color = Color(0xFFDC2626),
    val sliderFill: Color = Color(0xFF6B21A8),
    val sliderTrack: Color = Color(0xFFE5E7EB)
)

data class OJTypography(
    val fontResId: Int? = null // custom font resource ID
)

data class OJShapes(
    val cornerRadiusDp: Float = 12f,
    val borderWidthDp: Float = 1f,
    val shadowElevationDp: Float = 4f
)

/**
 * CompositionLocal for providing OuterJoyn appearance to all composables.
 */
val LocalOJAppearance = staticCompositionLocalOf { OuterJoynAppearance() }

/**
 * Convert appearance to CSS custom properties string for WebView injection.
 */
fun OuterJoynAppearance.toCSSVariables(): String {
    val vars = mutableListOf<String>()
    vars.add("--oj-primary: ${colors.primary.toHex()}")
    vars.add("--oj-accent: ${colors.accent.toHex()}")
    vars.add("--oj-bg: ${colors.background.toHex()}")
    vars.add("--oj-surface: ${colors.surface.toHex()}")
    vars.add("--oj-text: ${colors.text.toHex()}")
    vars.add("--oj-text-muted: ${colors.textMuted.toHex()}")
    vars.add("--oj-border: ${colors.border.toHex()}")
    vars.add("--oj-success: ${colors.success.toHex()}")
    vars.add("--oj-error: ${colors.error.toHex()}")
    vars.add("--oj-radius: ${shapes.cornerRadiusDp.toInt()}px")
    return vars.joinToString(";\n") + ";"
}

private fun Color.toHex(): String {
    val r = (red * 255).toInt()
    val g = (green * 255).toInt()
    val b = (blue * 255).toInt()
    return String.format("#%02X%02X%02X", r, g, b)
}
