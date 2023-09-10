package com.hb.superhero

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrain
import kotlinx.coroutines.launch

@ExperimentalComposeUiApi
fun Modifier.animateContentSize(
    animationSpec: FiniteAnimationSpec<IntSize> = spring(
        stiffness = Spring.StiffnessMediumLow
    ),
    finishedListener: ((initialValue: IntSize, targetValue: IntSize) -> Unit)? = null
): Modifier =
    this.clipToBounds() then SizeAnimationModifierElement(animationSpec, finishedListener)
@ExperimentalComposeUiApi
private data class SizeAnimationModifierElement(
    val animationSpec: FiniteAnimationSpec<IntSize>,
    val finishedListener: ((initialValue: IntSize, targetValue: IntSize) -> Unit)?
) : ModifierNodeElement<SizeAnimationModifierNode>() {
    override fun create(): SizeAnimationModifierNode =
        SizeAnimationModifierNode(animationSpec, finishedListener)
    override fun update(node: SizeAnimationModifierNode): SizeAnimationModifierNode {
        node.animationSpec = animationSpec
        node.listener = finishedListener

    }
    override fun InspectorInfo.inspectableProperties() {
        name = "animateContentSize"
        properties["animationSpec"] = animationSpec
        properties["finishedListener"] = finishedListener
    }
}
internal val InvalidSize = IntSize(Int.MIN_VALUE, Int.MIN_VALUE)
internal val IntSize.isValid: Boolean
    get() = this != InvalidSize
/**
 * This class creates a [LayoutModifier] that measures children, and responds to children's size
 * change by animating to that size. The size reported to parents will be the animated size.
 */
@ExperimentalComposeUiApi
private class SizeAnimationModifierNode(
    var animationSpec: AnimationSpec<IntSize>,
    var listener: ((startSize: IntSize, endSize: IntSize) -> Unit)? = null
) : LayoutModifierNodeWithPassThroughIntrinsics() {
    private var lookaheadSize: IntSize = InvalidSize
    private var lookaheadConstraints: Constraints = Constraints()
        set(value) {
            field = value
            lookaheadConstraintsAvailable = true
        }
    private var lookaheadConstraintsAvailable: Boolean = false
    private fun targetConstraints(default: Constraints) =
        if (lookaheadConstraintsAvailable) {
            lookaheadConstraints
        } else {
            default
        }
    data class AnimData(
        val anim: Animatable<IntSize, AnimationVector2D>,
        var startSize: IntSize
    )
    var animData: AnimData? by mutableStateOf(null)
    override fun onAttach() {
        super.onAttach()
        // When re-attached, we may be attached to a tree without lookahead scope.
        lookaheadSize = InvalidSize
        lookaheadConstraintsAvailable = false
    }
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = if (isLookingAhead) {
            lookaheadConstraints = constraints
            measurable.measure(constraints)
        } else {
            // Measure with lookahead constraints when available, to avoid unnecessary relayout
            // in child during the lookahead animation.
            measurable.measure(targetConstraints(constraints))
        }
        val measuredSize = IntSize(placeable.width, placeable.height)
        val (width, height) = if (isLookingAhead) {
            lookaheadSize = measuredSize
            measuredSize
        } else {
            animateTo(if (lookaheadSize.isValid) lookaheadSize else measuredSize).let {
                // Constrain the measure result to incoming constraints, so that parent doesn't
                // force center this layout.
                constraints.constrain(it)
            }
        }
        return layout(width, height) {
            placeable.placeRelative(0, 0)
        }
    }
    fun animateTo(targetSize: IntSize): IntSize {
        val data = animData?.apply {
            if (targetSize != anim.targetValue) {
                startSize = anim.value
                coroutineScope.launch {
                    val result = anim.animateTo(targetSize, animationSpec)
                    if (result.endReason == AnimationEndReason.Finished) {
                        listener?.invoke(startSize, result.endState.value)
                    }
                }
            }
        } ?: AnimData(
            Animatable(
                targetSize, IntSize.VectorConverter, IntSize(1, 1)
            ),
            targetSize
        )
        animData = data
        return data.anim.value
    }
}
@ExperimentalComposeUiApi
internal abstract class LayoutModifierNodeWithPassThroughIntrinsics :
    LayoutModifierNode, Modifier.Node() {
    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ) = measurable.minIntrinsicWidth(height)
    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ) = measurable.minIntrinsicHeight(width)
    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ) = measurable.maxIntrinsicWidth(height)
    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ) = measurable.maxIntrinsicHeight(width)
}
internal abstract class LayoutModifierWithPassThroughIntrinsics : LayoutModifier {
    final override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ) = measurable.minIntrinsicWidth(height)
    final override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ) = measurable.minIntrinsicHeight(width)
    final override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ) = measurable.maxIntrinsicWidth(height)
    final override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ) = measurable.maxIntrinsicHeight(width)
}
