package com.ljyh.mei.ui.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop> {
    error("Glass controls must be hosted by GlassBackdropHost or GlassBackdropProvider")
}

@Composable
fun GlassBackdropProvider(
    backdrop: Backdrop,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalGlassBackdrop provides backdrop, content = content)
}

/**
 * Keeps sampled content and glass overlays in separate layers. Glass controls must never be
 * placed inside [sampledContent], otherwise the backdrop can recursively sample itself.
 */
@Composable
fun GlassBackdropHost(
    modifier: Modifier = Modifier,
    sampledContent: @Composable BoxScope.(LayerBackdrop) -> Unit,
    overlayContent: @Composable BoxScope.(LayerBackdrop) -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
        Box(modifier) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop),
            ) {
                sampledContent(backdrop)
            }
            overlayContent(backdrop)
        }
    }
}

fun Modifier.glassBackdropSource(backdrop: LayerBackdrop): Modifier = layerBackdrop(backdrop)
