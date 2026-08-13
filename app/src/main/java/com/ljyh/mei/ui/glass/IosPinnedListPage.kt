package com.ljyh.mei.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.runtimeShaderEffect

private const val AlphaMaskShader = """
uniform shader content;
uniform float2 size;
layout(color) uniform half4 tint;
uniform float tintIntensity;

half4 main(float2 coord) {
    float mask = smoothstep(size.y, size.y * 0.42, coord.y);
    return mix(content.eval(coord) * mask, tint * mask, tintIntensity);
}
"""

@Composable
fun rememberIosGridCollapseProgress(
    gridState: LazyGridState,
    collapseDistance: Dp = 56.dp,
): Float {
    val collapseDistancePx = with(LocalDensity.current) { collapseDistance.toPx() }
    val progress by remember(gridState, collapseDistancePx) {
        derivedStateOf {
            if (gridState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (gridState.firstVisibleItemScrollOffset / collapseDistancePx).coerceIn(0f, 1f)
            }
        }
    }
    return progress
}

/**
 * Fixed iOS navigation bar over a progressively blurred scroll source.
 *
 * The scrolling layer exports its rendered result to a dedicated backdrop. The toolbar is kept
 * outside that layer, so the backdrop graph cannot recursively sample the toolbar itself.
 */
@Composable
fun IosPinnedListPage(
    title: String,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    listState: LazyListState = rememberLazyListState(),
    showsLargeTitle: Boolean = true,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    backgroundColor: Color? = null,
    content: LazyListScope.() -> Unit,
) {
    val collapseDistancePx = with(LocalDensity.current) { 56.dp.toPx() }
    val collapseProgress by remember(listState, showsLargeTitle) {
        derivedStateOf {
            if (!showsLargeTitle) {
                1f
            } else if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (listState.firstVisibleItemScrollOffset / collapseDistancePx).coerceIn(0f, 1f)
            }
        }
    }
    IosPinnedPage(
        title = title,
        subtitle = subtitle,
        bottomPadding = bottomPadding,
        modifier = modifier,
        onNavigateBack = onNavigateBack,
        actions = actions,
        collapseProgress = collapseProgress,
        backgroundColor = backgroundColor,
    ) { contentPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (showsLargeTitle) {
                item(key = "ios-large-title:$title") {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = 1f - collapseProgress
                                val scale = 1f - 0.04f * collapseProgress
                                scaleX = scale
                                scaleY = scale
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                            }
                            .blur(6.dp * collapseProgress)
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = title,
                            style = IosTypography.largeTitle,
                            fontWeight = FontWeight.Bold,
                        )
                        subtitle?.let {
                            Text(
                                text = it,
                                style = IosTypography.subheadline,
                                color = LocalGlassColors.current.secondaryContent,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                    }
                }
            }
            content()
        }
    }
}

/**
 * Fixed iOS navigation bar for pages whose body is not a single [LazyColumn].
 *
 * [content] is the only exported sample layer. All glass controls inside it continue to read
 * [LocalGlassBackdrop], while the toolbar reads this dedicated page layer, preventing feedback.
 */
@Composable
fun IosPinnedPage(
    title: String,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    collapseProgress: Float = 1f,
    backgroundColor: Color? = null,
    content: @Composable BoxScope.(PaddingValues) -> Unit,
) {
    val pageBackdrop = rememberLayerBackdrop()
    val parentBackdrop = LocalGlassBackdrop.current
    val topBarBackdrop = rememberCombinedBackdrop(parentBackdrop, pageBackdrop)
    val colors = LocalGlassColors.current
    val pageBackground = backgroundColor ?: colors.groupedBackground
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val toolbarHeight = statusBarHeight + 62.dp
    val contentPadding = PaddingValues(
        start = 16.dp,
        top = toolbarHeight + 10.dp,
        end = 16.dp,
        bottom = bottomPadding + 24.dp,
    )

    Box(modifier.fillMaxSize().background(pageBackground)) {
        Box(Modifier.fillMaxSize().layerBackdrop(pageBackdrop)) {
            content(contentPadding)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(toolbarHeight + 34.dp)
                .align(Alignment.TopCenter)
                .drawPlainBackdrop(
                    backdrop = pageBackdrop,
                    shape = { RectangleShape },
                    effects = {
                        blur(10.dp.toPx())
                        runtimeShaderEffect("IosTopBarAlphaMask", AlphaMaskShader, "content") {
                            setFloatUniform("size", size.width, size.height)
                            setColorUniform("tint", pageBackground)
                            setFloatUniform("tintIntensity", 0.78f)
                        }
                    },
                ),
        )
        CompositionLocalProvider(LocalGlassBackdrop provides topBarBackdrop) {
            IosTopToolbar(
                title = title,
                subtitle = subtitle,
                modifier = Modifier.fillMaxWidth().statusBarsPadding().align(Alignment.TopCenter),
                collapseProgress = collapseProgress,
                navigation = onNavigateBack?.let { navigateBack ->
                    {
                        GlassIconButton(navigateBack) {
                            SfIcon(SfSymbol.ChevronBack, null, mirrored = true)
                        }
                    }
                },
                actions = actions,
            )
        }
    }
}
