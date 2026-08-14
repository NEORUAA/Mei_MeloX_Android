package com.ljyh.mei.ui.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousRoundedRectangle
import com.kyant.shapes.Capsule
import com.ljyh.mei.ui.liquidglass.InteractiveHighlight
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

enum class GlassEmphasis {
    Regular,
    Prominent,
}

internal val LocalGlassSurfaceBrightness = staticCompositionLocalOf { 0f }

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    backdrop: Backdrop = LocalGlassBackdrop.current,
    shape: Shape = ContinuousRoundedRectangle(LocalGlassDimensions.current.regularCornerRadius),
    emphasis: GlassEmphasis = GlassEmphasis.Regular,
    enabled: Boolean = true,
    refractionHeight: Dp = 12.dp,
    refractionAmount: Dp = 24.dp,
    opticalHighlightBoost: Float = 0f,
    exportedBackdrop: LayerBackdrop? = null,
    onClick: (() -> Unit)? = null,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalGlassColors.current
    val isLight = !colors.isDark
    val brightness = LocalGlassSurfaceBrightness.current.coerceIn(0f, 1f)
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }
    val surfaceColor = when (emphasis) {
        GlassEmphasis.Regular -> colors.container
        GlassEmphasis.Prominent -> colors.prominentContainer
    }

    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    val progress = interactiveHighlight.pressProgress
                    vibrancy()
                    blur(2.dp.toPx())
                    lens(
                        refractionHeight = refractionHeight.toPx(),
                        refractionAmount = refractionAmount.toPx(),
                        depthEffect = progress > 0.01f,
                        chromaticAberration = true,
                    )
                },
                highlight = {
                    Highlight.Default.copy(
                        alpha = ((if (isLight) 0.48f else 0.32f) + brightness * 0.18f +
                            opticalHighlightBoost + 0.30f * interactiveHighlight.pressProgress)
                            .coerceAtMost(1f),
                    )
                },
                shadow = {
                    Shadow(
                        radius = 24.dp,
                        color = Color.Black.copy(alpha = 0.1f),
                        alpha = (0.08f + 0.22f * interactiveHighlight.pressProgress) *
                            if (enabled) 1f else 0.35f,
                    )
                },
                innerShadow = {
                    InnerShadow(
                        radius = 4.dp + 8.dp * interactiveHighlight.pressProgress,
                        color = Color.Black.copy(alpha = 0.15f),
                        alpha = 0.1f + 0.3f * interactiveHighlight.pressProgress,
                    )
                },
                layerBlock = {
                    val progress = interactiveHighlight.pressProgress
                    val controlHeight = size.height.coerceAtLeast(1f)
                    val scale = lerp(1f, 1f + 4.dp.toPx() / controlHeight, progress)
                    val maxOffset = size.minDimension.coerceAtLeast(1f)
                    val offset = interactiveHighlight.offset
                    translationX = maxOffset * tanh(0.05f * offset.x / maxOffset)
                    translationY = maxOffset * tanh(0.05f * offset.y / maxOffset)
                    val maxDragScale = 4.dp.toPx() / controlHeight
                    val angle = atan2(offset.y, offset.x)
                    scaleX = scale
                    scaleY = scale
                    scaleX += maxDragScale * abs(cos(angle) * offset.x / size.maxDimension.coerceAtLeast(1f)) *
                        (size.width / controlHeight).fastCoerceAtMost(1f)
                    scaleY += maxDragScale * abs(sin(angle) * offset.y / size.maxDimension.coerceAtLeast(1f)) *
                        (controlHeight / size.width.coerceAtLeast(1f)).fastCoerceAtMost(1f)
                },
                exportedBackdrop = exportedBackdrop,
                onDrawSurface = {
                    // iOS 27 controls use a visible white backing in addition to refraction.
                    // Keeping it in the shared surface makes top bars, tab bars and floating
                    // controls retain the same milky edge treatment over colorful content.
                    drawRect(
                        Color.White.copy(
                            alpha = (if (isLight) 0.16f else 0.06f) + brightness * 0.18f,
                        ),
                        blendMode = BlendMode.Screen,
                    )
                    if (emphasis == GlassEmphasis.Prominent) {
                        drawRect(
                            colors.prominentContainer.copy(alpha = 1f),
                            alpha = 0.22f,
                            blendMode = BlendMode.Hue,
                        )
                    }
                    drawRect(surfaceColor.copy(alpha = surfaceColor.alpha * if (enabled) 1f else 0.8f))
                },
            )
            .then(if (onClick != null && enabled) interactiveHighlight.modifier else Modifier)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        enabled = enabled,
                        interactionSource = null,
                        indication = null,
                        role = Role.Button,
                        onClick = onClick,
                    ).then(if (enabled) interactiveHighlight.gestureModifier else Modifier)
                } else {
                    Modifier
                },
            ),
        contentAlignment = contentAlignment,
        content = {
            val contentColor = if (emphasis == GlassEmphasis.Prominent) Color.White else colors.content
            CompositionLocalProvider(
                LocalContentColor provides contentColor,
                LocalGlassContentColor provides contentColor,
            ) {
                content()
            }
        },
    )
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop = LocalGlassBackdrop.current,
    enabled: Boolean = true,
    emphasis: GlassEmphasis = GlassEmphasis.Regular,
    content: @Composable RowScope.() -> Unit,
) {
    GlassSurface(
        modifier = modifier.height(LocalGlassDimensions.current.controlHeight),
        backdrop = backdrop,
        shape = Capsule(),
        emphasis = emphasis,
        enabled = enabled,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop = LocalGlassBackdrop.current,
    enabled: Boolean = true,
    emphasis: GlassEmphasis = GlassEmphasis.Regular,
    content: @Composable BoxScope.() -> Unit,
) {
    GlassSurface(
        modifier = modifier.size(LocalGlassDimensions.current.iconButtonSize),
        backdrop = backdrop,
        shape = CircleShape,
        emphasis = emphasis,
        enabled = enabled,
        onClick = onClick,
        content = content,
    )
}
