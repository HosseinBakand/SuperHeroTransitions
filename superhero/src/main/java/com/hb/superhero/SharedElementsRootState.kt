package com.hb.superhero

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot

class SharedElementsRootState {
    private val choreographer = ChoreographerWrapper()
    val trackers = mutableMapOf<SharedElementTag, SharedElementsTracker>()
    var invalidateTransitionsOverlay: () -> Unit = {}
    var rootCoordinates: LayoutCoordinates? = null

    fun shouldHideElement(elementInfo: SharedElementInfo): Boolean {
        return getTracker(elementInfo).shouldHideElement
    }

    fun onElementRegistered(elementInfo: SharedElementInfo) {
        choreographer.removeCallback(elementInfo)
        getTracker(elementInfo).onElementRegistered(elementInfo)
    }

    fun onElementPositioned(
        elementInfo: SharedElementInfo,
        placeholder: @Composable () -> Unit,
        coordinates: LayoutCoordinates,
        hidden: MutableState<Boolean>
    ) {
        val element = PositionedSharedElement(
            info = elementInfo,
            placeholder = placeholder,
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
