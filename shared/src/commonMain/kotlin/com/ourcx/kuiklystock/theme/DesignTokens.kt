package com.ourcx.kuiklystock.theme

import com.tencent.kuikly.core.base.Color

/** Shared visual language for every KuiklyStock screen and component. */
object DesignTokens {
    object ColorValues {
        const val PRIMARY = 0xFF0D0C22L
        const val PRIMARY_SOFT = 0xFF3B395CL
        const val SECONDARY = 0xFFFFFFFFL
        const val SURFACE_BASE = 0xFF060318L
        const val SURFACE_ALT = 0xFF0D0C22L
        const val SURFACE_ELEVATED = 0xFF151338L
        const val ON_PRIMARY = 0xFFFFFFFFL
        const val ON_SECONDARY = 0xFF060318L
        const val ON_SURFACE = 0xFFFFFFFFL
        const val ON_SURFACE_MUTED = 0xFFBEB9CAL
        const val BORDER_SUBTLE = 0xFF2A2745L
        const val BORDER_STRONG = 0xFF3F3A66L
        const val ACCENT_PRIMARY = 0xFFFF3366L
        const val ACCENT_SECONDARY = 0xFFFF6347L
        const val ACCENT_TERTIARY = 0xFF00E5FFL
        const val SUCCESS = 0xFF19C37DL
        const val DANGER = 0xFFFF3366L
        const val INFO = 0xFF00B3FFL
        const val TRANSPARENT = 0x00000000L
    }

    object Colors {
        val primary = Color(ColorValues.PRIMARY)
        val primarySoft = Color(ColorValues.PRIMARY_SOFT)
        val secondary = Color(ColorValues.SECONDARY)
        val surfaceBase = Color(ColorValues.SURFACE_BASE)
        val surfaceAlt = Color(ColorValues.SURFACE_ALT)
        val surfaceElevated = Color(ColorValues.SURFACE_ELEVATED)
        val onPrimary = Color(ColorValues.ON_PRIMARY)
        val onSecondary = Color(ColorValues.ON_SECONDARY)
        val onSurface = Color(ColorValues.ON_SURFACE)
        val onSurfaceMuted = Color(ColorValues.ON_SURFACE_MUTED)
        val borderSubtle = Color(ColorValues.BORDER_SUBTLE)
        val borderStrong = Color(ColorValues.BORDER_STRONG)
        val accentPrimary = Color(ColorValues.ACCENT_PRIMARY)
        val accentSecondary = Color(ColorValues.ACCENT_SECONDARY)
        val accentTertiary = Color(ColorValues.ACCENT_TERTIARY)
        val success = Color(ColorValues.SUCCESS)
        val danger = Color(ColorValues.DANGER)
        val info = Color(ColorValues.INFO)
        val transparent = Color(ColorValues.TRANSPARENT)
    }

    object Typography {
        const val H1 = 32f
        const val H2 = 24f
        const val H3 = 20f
        const val H4 = 18f
        const val BODY_LARGE = 16f
        const val BODY = 14f
        const val CAPTION = 12f
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
        const val SM = 4f
        const val MD = 8f
        const val LG = 16f
        const val XL = 24f
        const val FULL = 999f
    }

    object Size {
        const val FILL = 1f
        const val HAIRLINE = 1f
        /** Main content gutter. Kept explicit so page columns share the design's 16 px grid. */
        const val PAGE_GUTTER = 16f
        const val STATUS_DOT = 8f
        const val HEADER = 64f
        const val TAB_BAR = 64f
        const val TAB_INDICATOR = 3f
        const val SPARKLINE_HEIGHT = 88f
        const val SPARKLINE_BAR_MIN_HEIGHT = 6f
        const val SPARKLINE_BAR_GAP = 3f
        const val CHAT_INPUT_HEIGHT = 44f
        const val CHAT_SEND_WIDTH = 72f
        const val MARKET_PRICE_COLUMN = 132f
        const val MARKET_ACTION_HEIGHT = 36f
    }
}
