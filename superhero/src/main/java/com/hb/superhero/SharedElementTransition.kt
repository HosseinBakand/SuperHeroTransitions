package com.hb.superhero

import androidx.compose.animation.core.AnimationState
import androidx.compose.ui.unit.dp

sealed class SharedElementTransition(val startElement: PositionedSharedElement) {

    class WaitingForEndElementPosition(startElement: PositionedSharedElement) :
        SharedElementTransition(startElement)

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