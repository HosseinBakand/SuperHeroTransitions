package com.hb.superhero

import androidx.compose.runtime.MutableState
import kotlin.properties.Delegates

class SharedElementsTracker(
    private val onTransitionChanged: () -> Unit
) {
    private var state: State = State.Empty

    var transition by Delegates.observable<SharedElementTransition?>(null) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            (oldValue as? SharedElementTransition.InProgress)?.cleanup()
            onTransitionChanged()
        }
    }

    val isEmpty: Boolean get() = state is State.Empty

    val shouldHideElement: Boolean get() = transition != null

    fun onElementRegistered(elementInfo: SharedElementInfo) {
        when (val state = state) {
            is State.StartElementPositioned -> {
                if (!state.isRegistered(elementInfo)) {
                    this.state = State.EndElementRegistered(startElement = state.startElement, endElementInfo = elementInfo)
                    transition =
                        SharedElementTransition.WaitingForEndElementPosition(state.startElement)
                }
            }
            is State.StartElementRegistered -> {
                if (elementInfo != state.startElementInfo) {
                    this.state = State.StartElementRegistered(startElementInfo = elementInfo)
                }
            }
            is State.Empty -> {
                this.state = State.StartElementRegistered(startElementInfo = elementInfo)
            }
        }
    }

    fun onElementPositioned(element: PositionedSharedElement, hidden: MutableState<Boolean>) {
        when (val state = state) {
            is State.EndElementRegistered -> {
                if (element.info == state.endElementInfo) {
                    this.state = State.StartElementPositioned(startElement = element)
                    transition = SharedElementTransition.InProgress(
                        startElement = state.startElement,
                        endElement = element,
                        onTransitionFinished = {
                            transition = null
                            hidden.value = shouldHideElement
                        })
                } else if (element.info == state.startElementInfo) {
                    this.state = State.EndElementRegistered(startElement = element, endElementInfo = state.endElementInfo)
                    transition =
                        SharedElementTransition.WaitingForEndElementPosition(startElement = element)
                }
            }
            is State.StartElementRegistered -> {
                if (element.info == state.startElementInfo) {
                    this.state = State.StartElementPositioned(startElement = element)
                }
            }

            State.Empty -> TODO()
        }
    }

    fun onElementUnregistered(elementInfo: SharedElementInfo) {
        when (val state = state) {
            is State.EndElementRegistered -> {
                if (elementInfo == state.endElementInfo) {
                    this.state = State.StartElementPositioned(startElement = state.startElement)
                    transition = null
                } else if (elementInfo == state.startElement.info) {
                    this.state = State.StartElementRegistered(startElementInfo = state.endElementInfo)
                    transition = null
                }
            }
            is State.StartElementRegistered -> {
                if (elementInfo == state.startElementInfo) {
                    this.state = State.Empty
                    transition = null
                }
            }

            State.Empty -> TODO()
        }
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