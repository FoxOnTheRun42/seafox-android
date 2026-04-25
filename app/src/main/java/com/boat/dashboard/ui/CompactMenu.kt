package com.seafox.nmea_dashboard.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.seafox.nmea_dashboard.SeaFoxDesignTokens

@Composable
internal fun CompactMenuTextButton(
    text: String,
    style: TextStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = SeaFoxDesignTokens.Size.compactMenuItemPaddingHorizontal,
        vertical = SeaFoxDesignTokens.Size.compactMenuItemPaddingVertical,
    ),
    fillWidth: Boolean = true,
    minHeight: Dp = SeaFoxDesignTokens.Size.compactMenuItemHeight,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.primary,
) {
    val resolvedColor = if (style.color != Color.Unspecified) style.color else contentColor
    val activeColor = if (enabled) resolvedColor else resolvedColor.copy(alpha = 0.4f)
    val widthModifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier
    Box(
        modifier = modifier
            .then(widthModifier)
            .heightIn(min = minHeight)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(contentPadding)
    ) {
        Text(
            text,
            style = style.copy(
                color = activeColor,
                fontWeight = SeaFoxDesignTokens.Type.compactMenuButtonFontWeight
            ),
            color = activeColor,
            maxLines = 1
        )
    }
}

@Composable
private fun ScrollableMenuTextContent(
    state: ScrollState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    var viewportHeightPx by remember { mutableFloatStateOf(0f) }

    val canScroll = state.maxValue > 0
    val thumbHeightPx = if (viewportHeightPx > 0f && canScroll) {
        (viewportHeightPx * viewportHeightPx / (viewportHeightPx + state.maxValue.toFloat())).coerceIn(16f, viewportHeightPx)
    } else {
        0f
    }
    val maxThumbOffset = (viewportHeightPx - thumbHeightPx).coerceAtLeast(0f)
    val thumbOffsetPx = if (canScroll && maxThumbOffset > 0f) {
        (state.value.toFloat() / state.maxValue.toFloat()).coerceIn(0f, 1f) * maxThumbOffset
    } else {
        0f
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .onGloballyPositioned { layoutCoordinates ->
                    viewportHeightPx = layoutCoordinates.size.height.toFloat()
                }
                .fillMaxWidth()
                .padding(end = if (canScroll) SeaFoxDesignTokens.Size.menuDropdownItemLeadingPaddingEnd else 0.dp)
                .verticalScroll(state)
        ) {
            content()
        }

        if (canScroll && viewportHeightPx > 0f) {
            val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            val thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
            val thumbHeight = with(density) { thumbHeightPx.toDp() }
            val thumbOffset = with(density) { thumbOffsetPx.toDp() }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(vertical = SeaFoxDesignTokens.Size.menuDropdownScrollbarVerticalPadding)
                    .width(SeaFoxDesignTokens.Size.menuDropdownScrollbarWidth)
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .width(SeaFoxDesignTokens.Size.menuDropdownScrollbarTrackWidth)
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                        .background(trackColor, shape = RoundedCornerShape(SeaFoxDesignTokens.Size.menuDropdownTrackRadius))
                )
                Box(
                    modifier = Modifier
                        .width(SeaFoxDesignTokens.Size.menuDropdownScrollbarWidth)
                        .height(thumbHeight)
                        .align(Alignment.TopEnd)
                        .offset(y = thumbOffset)
                        .background(thumbColor, shape = RoundedCornerShape(SeaFoxDesignTokens.Size.menuDropdownThumbRadius))
                )
            }
        }
    }
}

@Composable
internal fun CompactMenuDropdownItem(
    text: String,
    style: TextStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingSymbol: String? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = SeaFoxDesignTokens.Size.compactMenuItemPaddingHorizontal,
        vertical = SeaFoxDesignTokens.Size.compactMenuItemPaddingVertical,
    ),
    minHeight: Dp = SeaFoxDesignTokens.Size.compactMenuItemHeight,
) {
    val resolvedColor = if (style.color != Color.Unspecified) style.color else MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clickable(onClick = onClick)
            .padding(contentPadding)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingSymbol != null) {
                Text(
                    text = leadingSymbol,
                    style = style.copy(
                        color = resolvedColor
                    ),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(SeaFoxDesignTokens.Size.menuDropdownLeadingSymbolSpacing))
            }
            Text(
                text,
                style = style.copy(
                    color = resolvedColor,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                color = resolvedColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CompactMenuDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null,
    isDarkMenu: Boolean = true,
) {
    val menuShape = RoundedCornerShape(18.dp)
    val menuContainerColor = if (isDarkMenu) {
        SeaFoxDesignTokens.Color.surfaceRaisedDark
    } else {
        SeaFoxDesignTokens.Color.surfaceRaisedLight
    }
    val menuTextColor = if (isDarkMenu) Color(0xFFEAF7FF) else SeaFoxDesignTokens.Color.ink
    val menuBorderColor = if (isDarkMenu) {
        SeaFoxDesignTokens.Color.hairlineDark
    } else {
        SeaFoxDesignTokens.Color.hairlineLight
    }
    val menuTextScrollState = rememberScrollState()
    val menuContentPadding = SeaFoxDesignTokens.Size.menuContentPadding
    AlertDialog(
        onDismissRequest = onDismissRequest,
        content = {
            Card(
                modifier = Modifier.border(1.dp, menuBorderColor, menuShape),
                shape = menuShape,
                colors = CardDefaults.cardColors(
                    containerColor = menuContainerColor,
                )
            ) {
                Column(
                    modifier = Modifier.padding(menuContentPadding),
                    verticalArrangement = Arrangement.spacedBy(MENU_SPACING)
                ) {
                    CompositionLocalProvider(
                        LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(color = menuTextColor)
                    ) {
                        title()
                    }
                    HorizontalDivider()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = SeaFoxDesignTokens.Size.menuDropdownContentMaxHeight)
                    ) {
                        ScrollableMenuTextContent(state = menuTextScrollState) {
                            CompositionLocalProvider(
                                LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(color = menuTextColor)
                            ) {
                                text()
                            }
                        }
                    }
                    if (menuTextScrollState.maxValue > 0) {
                        HorizontalDivider()
                    }
                    if (confirmButton != null || dismissButton != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = SeaFoxDesignTokens.Size.menuSectionSpacing),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (dismissButton != null) {
                                dismissButton()
                            }
                            if (confirmButton != null && dismissButton != null) {
                                Spacer(modifier = Modifier.width(SeaFoxDesignTokens.Size.menuSectionSpacing))
                            }
                            if (confirmButton != null) {
                                confirmButton()
                            }
                        }
                    }
                }
            }
        }
    )
}
