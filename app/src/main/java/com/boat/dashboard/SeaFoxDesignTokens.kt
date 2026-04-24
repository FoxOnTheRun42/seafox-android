package com.seafox.nmea_dashboard

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object SeaFoxDesignTokens {
    object Color {
        val deepOcean = Color(0xFF061018)
        val obsidian = Color(0xFF0A1118)
        val graphite = Color(0xFF111B24)
        val graphiteRaised = Color(0xFF172532)
        val mist = Color(0xFFF2F6F7)
        val porcelain = Color(0xFFF8FAF8)
        val ink = Color(0xFF101820)
        val cyan = Color(0xFF64D9FF)
        val cyanSoft = Color(0xFF9DEBFF)
        val brass = Color(0xFFD7B46A)
        val coral = Color(0xFFFF6B5F)
        val emerald = Color(0xFF47E0A0)
        val slateText = Color(0xFF2D3D4A)
        val mutedDark = Color(0xFF9FB3C3)
        val mutedLight = Color(0xFF5C6F7D)

        val summerBlue = cyan
        val link = summerBlue
        val seaFoxActionBlue = summerBlue

        val dashboardDark = deepOcean
        val dashboardLight = Color(0xFFEAF0F1)
        val topBarDark = Color(0xF20A121A)
        val topBarLight = Color(0xF8F7FAF9)
        val surfaceDark = Color(0xE6162430)
        val surfaceLight = Color(0xF7FFFFFF)
        val surfaceRaisedDark = Color(0xF21B2B38)
        val surfaceRaisedLight = Color(0xFFFFFFFF)
        val hairlineDark = Color(0x55BFEFFF)
        val hairlineLight = Color(0x4460717A)
        val panelHeaderDark = Color(0x55294654)
        val panelHeaderLight = Color(0xDDEDF3F4)

        val menuInputFieldBackgroundLight = Color(0xFFFFFFFF)
        val menuInputFieldBackgroundDark = Color(0xFF172532)
        val menuInputFieldTextLight = ink
        val menuInputFieldTextDark = Color(0xFFEAF7FF)
    }

    object NonMenuButton {
        val color = Color.seaFoxActionBlue
        val disabledColor = color.copy(alpha = 0.45f)
        private val containerColor = color.copy(alpha = 0.15f)
        private val disabledContainerColor = color.copy(alpha = 0.08f)

        @Composable
        fun textButtonColors() = ButtonDefaults.textButtonColors(
            contentColor = color,
            disabledContentColor = disabledColor,
        )

        @Composable
        fun filledButtonColors() = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = color,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledColor,
        )

        @Composable
        fun elevatedButtonColors() = ButtonDefaults.elevatedButtonColors(
            containerColor = containerColor,
            contentColor = color,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledColor,
        )
    }

    object LinkControl {
        @Composable
        fun radioButtonColors() = RadioButtonDefaults.colors(
            selectedColor = Color.link,
            unselectedColor = Color.link.copy(alpha = 0.7f),
            disabledSelectedColor = Color.link.copy(alpha = 0.4f),
            disabledUnselectedColor = Color.link.copy(alpha = 0.25f),
        )
    }

    object Size {
        val menuPadding = 8.dp
        val menuInputFieldHeight = 20.dp
        val menuInputFieldRadius: Dp = 4.dp
        val menuContentPadding = 32.dp
        val menuSectionSpacing = 10.dp
        val menuSmallSpacing = 4.dp
        val menuDropdownTrackAndThumbHeightMin = 420.dp
        val menuDropdownScrollbarVerticalPadding = 4.dp
        val menuDropdownScrollbarWidth = 6.dp
        val menuDropdownScrollbarTrackWidth = 3.dp
        val menuDropdownTrackRadius = 2.dp
        val menuDropdownThumbRadius = 3.dp
        val menuDropdownLeadingSymbolSpacing = 6.dp
        val menuDropdownItemLeadingPaddingEnd = 8.dp
        val menuDropdownItemMinHeight = 26.dp
        val menuDropdownMiniSpacing = 6.dp
        val menuDropdownContentMaxHeight = 420.dp
        val menuSmallCornerRadius = 8.dp
        val menuHamburgerStrokeWidth = 3.dp
        val compactMenuItemHeight = 18.dp
        val compactMenuItemPaddingHorizontal = 8.dp
        val compactMenuItemPaddingVertical = 0.dp
        val menuHamburgerHeight = 28.dp
        val menuBodyTextSp = 14.sp
        val menuLineHeight = 14.sp
        val menuHamburgerBottomOffsetSp = 1.sp
    }

    object Shape {
        val menuInputFieldShape = RoundedCornerShape(Size.menuInputFieldRadius)
    }

    object Type {
        val compactMenuButtonFontWeight = FontWeight.Bold

        val linkText = TextStyle(
            color = Color.link,
        )
    }
}
