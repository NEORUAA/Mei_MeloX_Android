package com.ljyh.mei.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.ljyh.mei.ui.liquidglass.DampedDragAnimation
import com.ljyh.mei.ui.liquidglass.InteractiveHighlight
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sin

@androidx.compose.runtime.Immutable
data class GlassTabItem<T>(
    val key: T,
    val label: String,
    val symbol: SfSymbol,
    val contentDescription: String = label,
)

private val LocalLiquidTabScale = staticCompositionLocalOf { { 1f } }

/**
 * iOS split-search tab bar backed by AndroidLiquidGlass' complete three-layer interaction.
 *
 * The outer glass keeps the capsule-to-circle navigation morph. The invisible source row and
 * the combined-backdrop indicator restore the original press, drag, lens, highlight and
 * velocity deformation from LiquidBottomTabs.
 */
@Composable
fun <T> GlassBottomBar(
    items: List<GlassTabItem<T>>,
    selectedKey: T,
    onSelected: (T) -> Unit,
    onExpand: () -> Unit,
    compactProgress: Float,
    compactSize: Dp,
    modifier: Modifier = Modifier,
    backdrop: Backdrop = LocalGlassBackdrop.current,
) {
    require(items.isNotEmpty())
    val compact = compactProgress.coerceIn(0f, 1f)
    val currentCompact by rememberUpdatedState(compact)
    val currentOnExpand by rememberUpdatedState(onExpand)
    val selectedIndex = items.indexOfFirst { it.key == selectedKey }.takeIf { it >= 0 } ?: 0
    val selectedItem = items[selectedIndex]
    val colors = LocalGlassColors.current
    val isLight = !colors.isDark
    val containerColor = colors.container
    val tabsBackdrop = rememberLayerBackdrop()
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()

    BoxWithConstraints(modifier.height(64.dp), contentAlignment = Alignment.CenterStart) {
        val density = LocalDensity.current
        val fullWidthPx = constraints.maxWidth.toFloat()
        val compactWidthPx = with(density) { compactSize.toPx() }
        val surfaceWidthPx = lerp(fullWidthPx, compactWidthPx, compact)
        val surfaceWidth = with(density) { surfaceWidthPx.toDp() }
        val surfaceHeight = androidx.compose.ui.unit.lerp(64.dp, compactSize, compact)
        val tabWidthPx = ((fullWidthPx - with(density) { 8.dp.toPx() }) / items.size)
            .coerceAtLeast(1f)
        val indicatorWidth = with(density) {
            lerp(tabWidthPx, (compactSize - 8.dp).toPx(), compact).toDp()
        }
        val expandedIndicatorVisibility = (1f - compact * 1.5f).coerceIn(0f, 1f)

        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / fullWidthPx).fastCoerceIn(-1f, 1f)
                with(density) {
                    4.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }
        var currentIndex by remember { mutableIntStateOf(selectedIndex) }
        val dragAnimation = remember(animationScope, items.size) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedIndex.toFloat(),
                valueRange = 0f..(items.lastIndex.coerceAtLeast(1)).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 78f / 56f,
                onDragStarted = {},
                onDragStopped = {
                    if (currentCompact >= 0.74f) {
                        currentOnExpand()
                        animateToValue(selectedIndex.toFloat())
                        return@DampedDragAnimation
                    }
                    val target = targetValue.fastRoundToInt().fastCoerceIn(0, items.lastIndex)
                    currentIndex = target
                    animateToValue(target.toFloat())
                    animationScope.launch {
                        offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                    }
                },
                onDrag = { _, dragAmount ->
                    if (currentCompact >= 0.74f) return@DampedDragAnimation
                    updateValue(
                        (targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, items.lastIndex.toFloat()),
                    )
                    animationScope.launch { offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x) }
                },
            )
        }
        LaunchedEffect(selectedIndex, compact >= 0.74f) {
            currentIndex = selectedIndex
            dragAnimation.animateToValue(selectedIndex.toFloat())
        }
        LaunchedEffect(dragAnimation) {
            snapshotFlow { currentIndex }
                .drop(1)
                .collectLatest { index -> onSelected(items[index].key) }
        }
        val interactiveHighlight = remember(animationScope, isLtr) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, _ ->
                    Offset(
                        x = if (isLtr) {
                            (dragAnimation.value + 0.5f) * tabWidthPx + panelOffset
                        } else {
                            size.width - (dragAnimation.value + 0.5f) * tabWidthPx + panelOffset
                        },
                        y = size.height / 2f,
                    )
                },
            )
        }
        val commonTransform = Modifier.graphicsLayer {
            translationX = panelOffset
            scaleY = 1f + 0.045f * sin(PI.toFloat() * compact)
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
        }

        // The compact control is deliberately the exact same one-layer component as search.
        // Keeping the expanded indicator composed here would stack a second refractive material.
        if (compact >= 0.98f) {
            GlassIconButton(
                onClick = currentOnExpand,
                backdrop = backdrop,
                style = GlassSurfaceStyle.Navigation,
                modifier = Modifier.size(compactSize),
            ) {
                SfIcon(
                    symbol = selectedItem.symbol,
                    contentDescription = selectedItem.contentDescription,
                    tint = colors.content,
                    size = 22.dp,
                    weight = FontWeight.SemiBold,
                )
            }
            return@BoxWithConstraints
        }

        Row(
            modifier = Modifier
                .width(surfaceWidth)
                .height(surfaceHeight)
                .then(commonTransform)
                .navigationGlassBackground(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    containerColor = containerColor,
                    pressProgress = dragAnimation.pressProgress,
                    layerBlock = {
                        val press = dragAnimation.pressProgress
                        val scale = lerp(1f, 1f + 16.dp.toPx() / size.width, press)
                        scaleX = scale
                        scaleY = scale
                    },
                )
                .then(interactiveHighlight.modifier)
                .padding(4.dp)
                .clip(Capsule()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FullTabContent(
                items = items,
                selectedKey = selectedItem.key,
                onSelected = onSelected,
                enabled = compact < 0.74f,
                alpha = (1f - compact * 1.35f).coerceIn(0f, 1f),
            )
        }

        CompositionLocalProvider(
            LocalLiquidTabScale provides {
                lerp(1f, 1.2f, dragAnimation.pressProgress) * (1f - compact)
                    .coerceAtLeast(0.82f)
            },
        ) {
            Row(
                modifier = Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .width(with(density) { fullWidthPx.toDp() })
                    .height(androidx.compose.ui.unit.lerp(56.dp, compactSize - 8.dp, compact))
                    .padding(horizontal = 4.dp)
                    .then(commonTransform)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            val press = dragAnimation.pressProgress
                            vibrancy()
                            blur(8.dp.toPx())
                            lens(
                                24.dp.toPx() * press,
                                28.dp.toPx() * press,
                                depthEffect = press > 0.01f,
                                chromaticAberration = true,
                            )
                        },
                        highlight = {
                            Highlight.Default.copy(alpha = 0.94f * dragAnimation.pressProgress)
                        },
                        onDrawSurface = { drawRect(containerColor) },
                    )
                    .then(interactiveHighlight.modifier)
                    .graphicsLayer(colorFilter = ColorFilter.tint(colors.accent)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FullTabContent(
                    items = items,
                    selectedKey = selectedItem.key,
                    onSelected = onSelected,
                    // Keep the exported source interactive just like LiquidBottomTabs. It is
                    // visually hidden, but it sits above the visible row in the hit-test tree.
                    enabled = compact < 0.74f,
                    alpha = (1f - compact * 1.35f).coerceIn(0f, 1f),
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .graphicsLayer {
                    // The outer padding already places the 56dp indicator at x=4dp inside
                    // the 64dp capsule. Adding another 4dp here made the compact orb and icon
                    // visibly off-centre.
                    val compactX = 0f
                    val tabX = if (isLtr) {
                        dragAnimation.value * tabWidthPx + panelOffset
                    } else {
                        surfaceWidthPx - (dragAnimation.value + 1f) * tabWidthPx + panelOffset
                    }
                    translationX = lerp(tabX, compactX, compact)
                }
                .then(interactiveHighlight.gestureModifier)
                .then(dragAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                    shape = { Capsule() },
                    effects = {
                        val press = dragAnimation.pressProgress
                        val opticalIntensity = press * expandedIndicatorVisibility
                        lens(
                            14.dp.toPx() * opticalIntensity,
                            22.dp.toPx() * opticalIntensity,
                            depthEffect = opticalIntensity > 0.01f,
                            chromaticAberration = true,
                        )
                    },
                    highlight = {
                        Highlight.Default.copy(
                            alpha = 0.90f * dragAnimation.pressProgress * expandedIndicatorVisibility,
                        )
                    },
                    shadow = {
                        Shadow(alpha = 0.84f * dragAnimation.pressProgress * expandedIndicatorVisibility)
                    },
                    innerShadow = {
                        val strength = dragAnimation.pressProgress * expandedIndicatorVisibility
                        InnerShadow(
                            radius = 10.dp * strength,
                            alpha = 0.86f * strength,
                        )
                    },
                    layerBlock = {
                        scaleX = dragAnimation.scaleX
                        scaleY = dragAnimation.scaleY
                        val velocity = dragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val press = dragAnimation.pressProgress
                        drawRect(
                            if (isLight) Color.Black.copy(alpha = 0.10f)
                            else Color.White.copy(alpha = 0.10f),
                            alpha = (1f - press) * expandedIndicatorVisibility,
                        )
                        drawRect(Color.Black.copy(alpha = 0.03f * press * expandedIndicatorVisibility))
                    },
                )
                .height(androidx.compose.ui.unit.lerp(56.dp, compactSize - 8.dp, compact))
                .width(indicatorWidth)
                .clickable(
                    enabled = compact >= 0.74f,
                    interactionSource = null,
                    indication = null,
                    role = Role.Tab,
                    onClick = currentOnExpand,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (compact > 0.001f) {
                SfIcon(
                    symbol = selectedItem.symbol,
                    contentDescription = selectedItem.contentDescription,
                    tint = androidx.compose.ui.graphics.lerp(colors.accent, colors.content, compact),
                    size = 25.dp,
                    weight = FontWeight.SemiBold,
                    modifier = Modifier.graphicsLayer {
                        alpha = compact
                        val scale = lerp(0.82f, 1f, compact)
                        scaleX = scale
                        scaleY = scale
                    },
                )
            }
        }
    }
}

@Composable
private fun <T> androidx.compose.foundation.layout.RowScope.FullTabContent(
    items: List<GlassTabItem<T>>,
    selectedKey: T,
    onSelected: (T) -> Unit,
    enabled: Boolean,
    alpha: Float,
) {
    val colors = LocalGlassColors.current
    val scale = LocalLiquidTabScale.current
    items.forEach { item ->
        val selected = item.key == selectedKey
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(Capsule())
                .clickable(
                    enabled = enabled,
                    interactionSource = null,
                    indication = null,
                    role = Role.Tab,
                    onClick = { onSelected(item.key) },
                )
                .graphicsLayer {
                    this.alpha = alpha
                    val contentScale = scale()
                    scaleX = contentScale
                    scaleY = contentScale
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SfIcon(
                symbol = item.symbol,
                contentDescription = item.contentDescription,
                tint = if (selected) colors.accent else colors.content,
                size = 24.dp,
                weight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                modifier = Modifier.padding(top = 3.dp),
            )
            Text(
                text = item.label,
                color = if (selected) colors.accent else colors.content,
                style = IosTypography.caption,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp, bottom = 4.dp),
            )
        }
    }
}
