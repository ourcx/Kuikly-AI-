package com.ourcx.kuiklystock.theme

import com.tencent.kuikly.core.base.Color

/** Shared visual language for every KuiklyStock screen and component. */
object DesignTokens {
    object ColorValues {
        const val BACKGROUND = 0xFF08111FL
        const val SURFACE = 0xFF111C2EL
        const val SURFACE_ELEVATED = 0xFF17243AL
        const val BORDER = 0xFF26364FL
        const val ACCENT = 0xFF4C8DFFL
        const val ACCENT_MUTED = 0xFF18345FL
        const val TEXT_PRIMARY = 0xFFF3F7FFL
        const val TEXT_SECONDARY = 0xFF94A3B8L
        const val TEXT_MUTED = 0xFF64748BL
        const val RISE = 0xFFF05252L
        const val FALL = 0xFF22C55EL
        const val NEUTRAL = 0xFF94A3B8L
        const val TRANSPARENT = 0x00000000L
    }

    object Colors {
        val background = Color(ColorValues.BACKGROUND)
        val surface = Color(ColorValues.SURFACE)
        val surfaceElevated = Color(ColorValues.SURFACE_ELEVATED)
        val border = Color(ColorValues.BORDER)
        val accent = Color(ColorValues.ACCENT)
        val accentMuted = Color(ColorValues.ACCENT_MUTED)
        val textPrimary = Color(ColorValues.TEXT_PRIMARY)
        val textSecondary = Color(ColorValues.TEXT_SECONDARY)
        val textMuted = Color(ColorValues.TEXT_MUTED)
        val rise = Color(ColorValues.RISE)
        val fall = Color(ColorValues.FALL)
        val neutral = Color(ColorValues.NEUTRAL)
        val transparent = Color(ColorValues.TRANSPARENT)
    }

    object Typography {
        const val CAPTION = 11f
        const val BODY = 14f
        const val LABEL = 15f
        const val TITLE = 20f
        const val BRAND = 24f
    }

    object Spacing {
        const val XXS = 4f
        const val XS = 8f
        const val SM = 12f
        const val MD = 16f
        const val LG = 24f
        const val XL = 32f
    }

    object Radius {
        const val SM = 8f
        const val MD = 12f
        const val LG = 18f
        const val PILL = 999f
    }

    object Size {
        const val FILL = 1f
        const val HAIRLINE = 1f
        const val STATUS_DOT = 8f
        const val HEADER = 72f
        const val TAB_BAR = 64f
        const val TAB_INDICATOR = 3f
        const val SPARKLINE_HEIGHT = 88f
        const val SPARKLINE_BAR_MIN_HEIGHT = 6f
        const val SPARKLINE_BAR_GAP = 3f
        const val CHAT_INPUT_HEIGHT = 44f
        const val CHAT_SEND_WIDTH = 72f
    }
}
