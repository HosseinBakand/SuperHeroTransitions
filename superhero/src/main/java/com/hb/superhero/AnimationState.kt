package com.hb.superhero

import androidx.compose.ui.unit.Dp


data class AnimationState(
    val offsetX: Dp,
    val offsetY: Dp,
    val scaleX: Float,
    val scaleY: Float,
    val alpha: Float
)