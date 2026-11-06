package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.ui.theme.GoldPrimary

/**
 * Android TV Leanback Focus Effect Modifier:
 * - Smoothly scales up focused elements (1.06x - 1.08x)
 * - Lifts element above neighboring grid items using zIndex
 * - Animates an elevated luminous golden border (3.dp)
 * - Adds dynamic drop shadow for 10-foot TV viewing clarity
 */
@Composable
fun Modifier.tvFocusable(
    shape: Shape = RoundedCornerShape(12.dp),
    focusScale: Float = 1.07f,
    focusedBorderColor: Color = GoldPrimary,
    focusedBorderWidth: Dp = 3.dp,
    unfocusedBorderWidth: Dp = 0.dp,
    unfocusedBorderColor: Color = Color.Transparent,
    onFocusChanged: ((Boolean) -> Unit)? = null
): Modifier {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusScale else 1.0f,
        animationSpec = tween(durationMillis = 180),
        label = "tv_focus_scale"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) focusedBorderWidth else unfocusedBorderWidth,
        animationSpec = tween(durationMillis = 180),
        label = "tv_focus_border"
    )

    val borderColor = if (isFocused) focusedBorderColor else unfocusedBorderColor

    return this
        .onFocusChanged { focusState ->
            isFocused = focusState.isFocused
            onFocusChanged?.invoke(focusState.isFocused)
        }
        .zIndex(if (isFocused) 10f else 1f)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            shadowElevation = if (isFocused) 24f else 0f
            this.shape = shape
            clip = false
        }
        .border(borderWidth, borderColor, shape)
}
