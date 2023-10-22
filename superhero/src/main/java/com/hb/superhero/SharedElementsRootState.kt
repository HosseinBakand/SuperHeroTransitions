package com.hb.superhero

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.flow.MutableStateFlow

class SharedElementsRootState {
    private val choreographer = ChoreographerWrapper()
    val trackers = mutableMapOf<SharedElementTag, SharedElementsTracker>()
    var invalidateTransitionsOverlay: () -> Unit = {}
    var rootCoordinates: LayoutCoordinates? = null

    val _uiState = MutableStateFlow<Pair<IntSize,Offset>?>(
        null
    )

    fun shouldHideElement(elementInfo: SharedElementInfo): Boolean {
        return getTracker(elementInfo).shouldHideElement
    }

    fun onElementRegistered(elementInfo: SharedElementInfo) {
        Log.e("TAGTAG","onElementRegistered ${elementInfo.tag}  ${elementInfo.type}")
        choreographer.removeCallback(elementInfo)
        getTracker(elementInfo).onElementRegistered(elementInfo)
    }

    fun onElementPositioned(
        elementInfo: SharedElementInfo,
        coordinates: LayoutCoordinates,
        hidden: MutableState<Boolean>
    ) {
        Log.e("TAGTAG","onElementPositioned ${elementInfo.tag} ${elementInfo.type} ${coordinates.size}")
        val element = PositionedSharedElement(
            info = elementInfo,
            bounds = calculateElementBoundsInRoot(coordinates)
        )
        getTracker(elementInfo).onElementPositioned(element, hidden)
    }

    fun onElementDisposed(elementInfo: SharedElementInfo) {
        choreographer.postCallback(elementInfo) {
            val tracker = getTracker(elementInfo)
            tracker.onElementUnregistered(elementInfo)
            if (tracker.isEmpty) trackers.remove(elementInfo.tag)
        }
    }

    fun onDisposed() {
        choreographer.clear()
    }

    private fun getTracker(elementInfo: SharedElementInfo): SharedElementsTracker {
        return trackers.getOrPut(elementInfo.tag) {
            SharedElementsTracker(onTransitionChanged = { invalidateTransitionsOverlay() })
        }
    }

    private fun calculateElementBoundsInRoot(elementCoordinates: LayoutCoordinates): Rect {
        return rootCoordinates?.localBoundingBoxOf(elementCoordinates) ?: elementCoordinates.boundsInRoot()
    }
}
