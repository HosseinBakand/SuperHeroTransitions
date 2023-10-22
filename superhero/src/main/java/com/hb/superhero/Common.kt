package com.hb.superhero

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect


typealias SharedElementTag = Any


val SharedElementsRootStateAmbient = staticCompositionLocalOf<SharedElementsRootState> {
    error("SharedElementsRoot not found. SharedElement must be hosted in SharedElementsRoot.")
}
class PositionedSharedElement(
    val info: SharedElementInfo,
//    val placeholder: @Composable () -> Unit,
    val bounds: Rect
)
enum class SharedElementType { INITIAL, TARGET }

data class SharedElementInfo(val tag: SharedElementTag, val type: SharedElementType)