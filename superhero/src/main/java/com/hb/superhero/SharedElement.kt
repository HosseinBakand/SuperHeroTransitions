package com.hb.superhero

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hb.superhero.SharedElementTransition.InProgress
import com.hb.superhero.SharedElementTransition.WaitingForEndElementPosition
//import androidx.lifecycle.compose.collectAsStateWithLifecycle


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
//    val uiState by rootState._uiState.collectAsStateWithLifecycle()
    Box(modifier = Modifier.onGloballyPositioned { layoutCoordinates ->
        rootState.rootCoordinates = layoutCoordinates
    }) {
        CompositionLocalProvider(SharedElementsRootStateAmbient provides rootState) {
            children()
        }
        SharedElementTransitionsOverlay(rootState, state)

//        Box(modifier = Modifier.size(rootState._uiState.value.))
    }
}

@Composable
private fun SharedElementTransitionsOverlay(
    rootState: SharedElementsRootState,
    state: MutableState<SharedElementType>
) {
    val label = "Hero"

    val transitionState = updateTransition(
        targetState = if (state.value == SharedElementType.INITIAL) InProgress.State.START else InProgress.State.END,
        label = label
    )
    Log.e("TAGTAG","SharedElementTransitionsOverlay")
    LaunchedEffect(key1 = rootState.trackers){

    }
    rootState.trackers.values.forEach { tracker ->
        Log.e("TAGTAG","trackers $tracker")
        when (val transition = tracker.transition) {
            is WaitingForEndElementPosition -> SharedElementTransitionPlaceholder(
                sharedElement = transition.startElement,
                offsetX = transition.startElement.bounds.left.dp,
                offsetY = transition.startElement.bounds.top.dp
            )

            is InProgress -> {
                Log.e("TAGTAG","starrrrrrrrrrrrrrrrrrrrrrrrrt")
                val offsetX by transitionState.animateDp(label = "OffsetX") { state ->
                    when (state) {
                        InProgress.State.START -> transition.startElementPropKeys.offsetX
                        InProgress.State.END -> transition.endElementPropKeys.offsetX
                    }
                }
                val offsetY by transitionState.animateDp(label = "OffsetY") { state ->
                    when (state) {
                        InProgress.State.START -> transition.startElementPropKeys.offsetY
                        InProgress.State.END -> transition.endElementPropKeys.offsetY
                    }
                }
                val scaleX by transitionState.animateFloat(label = "ScaleX") { state ->
                    when (state) {
                        InProgress.State.START -> transition.startElementPropKeys.scaleX
                        InProgress.State.END -> transition.endElementPropKeys.scaleY
                    }
                }
                val scaleY by transitionState.animateFloat(label = "ScaleY") { state ->
                    when (state) {
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

                SharedElementTransitionPlaceholder(
                    sharedElement = transition.startElement,
                    props = AnimationState(offsetX, offsetY, scaleX, scaleY, 1f - alpha)
                )
                SharedElementTransitionPlaceholder(
                    sharedElement = transition.endElement,
                    props = AnimationState(offsetX, offsetY, scaleX, scaleY, alpha)
                )
            }

            null -> {
//                TODO()
            }
        }
    }
}

@Composable
private fun SharedElementTransitionPlaceholder(
    sharedElement: PositionedSharedElement,
    props: AnimationState
) {
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
private fun SharedElementTransitionPlaceholder(
    sharedElement: PositionedSharedElement,
    offsetX: Dp,
    offsetY: Dp,
    scaleX: Float = 1f,
    scaleY: Float = 1f,
    alpha: Float = 1f
) {
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
    }
}


