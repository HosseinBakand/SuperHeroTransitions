package com.hb.superhero

import android.view.Choreographer
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hb.superhero.SharedElementTransition.InProgress
import com.hb.superhero.SharedElementTransition.WaitingForEndElementPosition
import com.hb.superhero.SharedElementsTracker.State.Empty
import com.hb.superhero.SharedElementsTracker.State.EndElementRegistered
import com.hb.superhero.SharedElementsTracker.State.StartElementPositioned
import com.hb.superhero.SharedElementsTracker.State.StartElementRegistered
import kotlin.properties.Delegates

enum class SharedElementType { FROM, TO }

@Composable
fun SharedElement(
    tag: Any,
    type: SharedElementType,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    children: @Composable () -> Unit
) {
    val elementInfo = SharedElementInfo(tag, type)
    val rootState = SharedElementsRootStateAmbient.current
    val hidden = remember { mutableStateOf(false) }

    rootState.onElementRegistered(elementInfo)

    val visibilityModifier = if (hidden.value) Modifier.alpha(alpha = 0f) else Modifier
    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                rootState.onElementPositioned(
                    elementInfo = elementInfo,
                    placeholder = placeholder ?: children,
                    coordinates = coordinates,
                    hidden = hidden
                )
            }
            .then(visibilityModifier),
    ) {
        children()
    }
}

@Composable
fun SharedElementsRoot(state: MutableState<SharedElementType>, children: @Composable () -> Unit) {
    val rootState = remember { SharedElementsRootState() }

    Box(modifier = Modifier.onGloballyPositioned { layoutCoordinates ->
        rootState.rootCoordinates = layoutCoordinates
    }) {
        CompositionLocalProvider(SharedElementsRootStateAmbient provides rootState) {
            children()
        }
        SharedElementTransitionsOverlay(rootState, state)
    }
}

@Composable
private fun SharedElementTransitionsOverlay(rootState: SharedElementsRootState, state: MutableState<SharedElementType>) {
    val label = "Hero"

    val transitionState = updateTransition(targetState = if (state.value == SharedElementType.FROM) InProgress.State.START else InProgress.State.END, label = label)



    rootState.trackers.values.forEach { tracker ->
        when (val transition = tracker.transition) {
            is WaitingForEndElementPosition -> SharedElementTransitionPlaceholder(
                sharedElement = transition.startElement,
                offsetX = transition.startElement.bounds.left.dp,
                offsetY = transition.startElement.bounds.top.dp
            )
            is InProgress -> {
                val offsetX by transitionState.animateDp(label = "OffsetX") { state ->
                    when(state) {
                        InProgress.State.START -> transition.startElementPropKeys.offsetX
                        InProgress.State.END -> transition.endElementPropKeys.offsetX
                    }
                }
                val offsetY by transitionState.animateDp(label = "OffsetY") { state ->
                    when(state) {
                        InProgress.State.START -> transition.startElementPropKeys.offsetY
                        InProgress.State.END -> transition.endElementPropKeys.offsetY
                    }
                }
                val scaleX by transitionState.animateFloat(label = "ScaleX") { state ->
                    when(state) {
                        InProgress.State.START -> transition.startElementPropKeys.scaleX
                        InProgress.State.END -> transition.endElementPropKeys.scaleY
                    }
                }
                val scaleY by transitionState.animateFloat(label = "ScaleY") { state ->
                    when(state) {
                        InProgress.State.START -> transition.startElementPropKeys.scaleX
                        InProgress.State.END -> transition.endElementPropKeys.scaleY
                    }
                }
                val alpha by transitionState.animateFloat(label = "Raw") { state ->
                    when (state) {
                        InProgress.State.START -> 0f
                        InProgress.State.END -> 1f
                    }
                }

                SharedElementTransitionPlaceholder(sharedElement = transition.startElement, props = AnimationState(offsetX, offsetY, scaleX, scaleY, 1f - alpha))
                SharedElementTransitionPlaceholder(sharedElement = transition.endElement, props = AnimationState(offsetX, offsetY, scaleX, scaleY, alpha))
            }

            null -> TODO()
        }
    }
}

@Composable
private fun SharedElementTransitionPlaceholder(sharedElement: PositionedSharedElement, props: AnimationState) {
    SharedElementTransitionPlaceholder(
        sharedElement = sharedElement,
        offsetX = props.offsetX,
        offsetY = props.offsetY,
        scaleX = props.scaleX,
        scaleY = props.scaleY,
        alpha = props.alpha
    )
}

@Composable
private fun SharedElementTransitionPlaceholder(sharedElement: PositionedSharedElement, offsetX: Dp, offsetY: Dp, scaleX: Float = 1f, scaleY: Float = 1f, alpha: Float = 1f) {
    Box(
        modifier = Modifier
            .size(
                width = sharedElement.bounds.width.dp,
                height = sharedElement.bounds.height.dp
            )
            .offset(
                x = offsetX,
                y = offsetY
            )
            .scale(
                scaleX,
                scaleY
            )
            .alpha(
                alpha = alpha
            ),
    ) {
        sharedElement.placeholder()
    }
}

private val SharedElementsRootStateAmbient = staticCompositionLocalOf<SharedElementsRootState> {
    error("SharedElementsRoot not found. SharedElement must be hosted in SharedElementsRoot.")
}

private class SharedElementsRootState {
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

private class SharedElementsTracker(
    private val onTransitionChanged: () -> Unit
) {
    private var state: State = Empty

    var transition by Delegates.observable<SharedElementTransition?>(null) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            (oldValue as? InProgress)?.cleanup()
            onTransitionChanged()
        }
    }

    val isEmpty: Boolean get() = state is Empty

    val shouldHideElement: Boolean get() = transition != null

    fun onElementRegistered(elementInfo: SharedElementInfo) {
        when (val state = state) {
            is StartElementPositioned -> {
                if (!state.isRegistered(elementInfo)) {
                    this.state = EndElementRegistered(startElement = state.startElement, endElementInfo = elementInfo)
                    transition = WaitingForEndElementPosition(state.startElement)
                }
            }
            is StartElementRegistered -> {
                if (elementInfo != state.startElementInfo) {
                    this.state = StartElementRegistered(startElementInfo = elementInfo)
                }
            }
            is Empty -> {
                this.state = StartElementRegistered(startElementInfo = elementInfo)
            }
        }
    }

    fun onElementPositioned(element: PositionedSharedElement, hidden: MutableState<Boolean>) {
        when (val state = state) {
            is EndElementRegistered -> {
                if (element.info == state.endElementInfo) {
                    this.state = StartElementPositioned(startElement = element)
                    transition = InProgress(startElement = state.startElement, endElement = element, onTransitionFinished = {
                        transition = null
                        hidden.value = shouldHideElement
                    })
                } else if (element.info == state.startElementInfo) {
                    this.state = EndElementRegistered(startElement = element, endElementInfo = state.endElementInfo)
                    transition = WaitingForEndElementPosition(startElement = element)
                }
            }
            is StartElementRegistered -> {
                if (element.info == state.startElementInfo) {
                    this.state = StartElementPositioned(startElement = element)
                }
            }

            Empty -> TODO()
        }
    }

    fun onElementUnregistered(elementInfo: SharedElementInfo) {
        when (val state = state) {
            is EndElementRegistered -> {
                if (elementInfo == state.endElementInfo) {
                    this.state = StartElementPositioned(startElement = state.startElement)
                    transition = null
                } else if (elementInfo == state.startElement.info) {
                    this.state = StartElementRegistered(startElementInfo = state.endElementInfo)
                    transition = null
                }
            }
            is StartElementRegistered -> {
                if (elementInfo == state.startElementInfo) {
                    this.state = Empty
                    transition = null
                }
            }

            Empty -> TODO()
        }
    }

    private sealed class State {
        object Empty : State()

        open class StartElementRegistered(val startElementInfo: SharedElementInfo) : State() {
            open fun isRegistered(elementInfo: SharedElementInfo): Boolean {
                return elementInfo == startElementInfo
            }
        }

        open class StartElementPositioned(open val startElement: PositionedSharedElement) : StartElementRegistered(startElement.info)

        class EndElementRegistered(override val startElement: PositionedSharedElement, val endElementInfo: SharedElementInfo) : StartElementPositioned(startElement) {
            override fun isRegistered(elementInfo: SharedElementInfo): Boolean {
                return super.isRegistered(elementInfo) || elementInfo == endElementInfo
            }
        }
    }
}

private typealias SharedElementTag = Any

private data class SharedElementInfo(val tag: SharedElementTag, val type: SharedElementType)

private class PositionedSharedElement(
    val info: SharedElementInfo,
    val placeholder: @Composable () -> Unit,
    val bounds: Rect
)

private sealed class SharedElementTransition(val startElement: PositionedSharedElement) {

    class WaitingForEndElementPosition(startElement: PositionedSharedElement) : SharedElementTransition(startElement)

    class InProgress(
        startElement: PositionedSharedElement,
        val endElement: PositionedSharedElement,
        var onTransitionFinished: () -> Unit
    ) : SharedElementTransition(startElement) {

        val startElementPropKeys = AnimationState(
            startElement.bounds.left.dp,
            startElement.bounds.top.dp,
            1f,
            1f,
            1f
        )

        val endElementPropKeys = AnimationState(
            endElement.bounds.left.dp,
            endElement.bounds.top.dp,
            endElement.bounds.width / startElement.bounds.width,
            endElement.bounds.height / startElement.bounds.height,
            1f
        )

        fun cleanup() {
            onTransitionFinished = {}
        }

        enum class State {
            START, END
        }
    }
}

internal data class AnimationState(
    val offsetX: Dp,
    val offsetY: Dp,
    val scaleX: Float,
    val scaleY: Float,
    val alpha: Float
)

private class ChoreographerWrapper {
    private val callbacks = mutableMapOf<SharedElementInfo, Choreographer.FrameCallback>()
    private val choreographer = Choreographer.getInstance()

    fun postCallback(elementInfo: SharedElementInfo, callback: () -> Unit) {
        if (callbacks.containsKey(elementInfo)) return

        val frameCallback = Choreographer.FrameCallback {
            callbacks.remove(elementInfo)
            callback()
        }
        callbacks[elementInfo] = frameCallback
        choreographer.postFrameCallback(frameCallback)
    }

    fun removeCallback(elementInfo: SharedElementInfo) {
        callbacks.remove(elementInfo)?.also(choreographer::removeFrameCallback)
    }

    fun clear() {
        callbacks.values.forEach(choreographer::removeFrameCallback)
        callbacks.clear()
    }
}