package com.ljyh.mei.ui.component.player.component

import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import coil3.Bitmap
import com.ljyh.mei.constants.MeshFlowSpeedKey
import com.ljyh.mei.constants.MeshLowFreqVolumeKey
import com.ljyh.mei.constants.MeshPlayingKey
import com.ljyh.mei.constants.MeshRenderScaleKey
import com.ljyh.mei.constants.MeshStaticModeKey
import com.ljyh.mei.constants.MeshSubdivisionKey
import com.ljyh.mei.ui.component.player.LocalPlayerBackdropCover
import com.ljyh.mei.ui.component.player.component.mesh.AlbumTextureProcessor
import com.ljyh.mei.ui.component.player.component.mesh.MeshBackgroundView
import com.ljyh.mei.utils.audio.AudioVisualizerManager
import com.ljyh.mei.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Composable
fun FluidBackground(
    imageUrl: String?,
    audioVisualizerManager: AudioVisualizerManager,
    isPlaying: Boolean = true,
    alpha: Float = 1f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bass by audioVisualizerManager.bassValue.collectAsState()

    val (flowSpeed) = rememberPreference(MeshFlowSpeedKey, defaultValue = 0.25f)
    val (renderScale) = rememberPreference(MeshRenderScaleKey, defaultValue = 0.75f)
    val (staticMode) = rememberPreference(MeshStaticModeKey, defaultValue = false)
    val (meshPlaying) = rememberPreference(MeshPlayingKey, defaultValue = true)
    val (volumeScale) = rememberPreference(MeshLowFreqVolumeKey, defaultValue = 0.1f)
    val (subdivision) = rememberPreference(MeshSubdivisionKey, defaultValue = 50)

    // 1. 将图片加载逻辑独立出来，只负责把 Bitmap 提取出来
    // 使用 produceState 是处理这种“异步数据转同步状态”的最佳实践
    val albumBitmap by produceState<Bitmap?>(null, imageUrl) {
        if (imageUrl.isNullOrEmpty()) {
            value = null
            return@produceState
        }
        withContext(Dispatchers.IO) {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .size(256)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                // Detach from Coil's bitmap pool: the GL renderer owns this instance and
                // recycles it on track change, which must never corrupt a pooled bitmap.
                val decoded = result.image.toBitmap()
                value = decoded.copy(android.graphics.Bitmap.Config.ARGB_8888, false) ?: decoded
            }
        }
    }

    var meshView by remember { mutableStateOf<MeshBackgroundView?>(null) }

    // Push the album exactly once per bitmap change. Calling setAlbum from AndroidView's
    // update block re-fires on every recomposition (sheet animation ~60Hz, bass ~10Hz),
    // and each call restarts the renderer's cross-fade with a new random mesh preset,
    // which is the visible flicker/dark-dip source.
    LaunchedEffect(meshView, albumBitmap) {
        val view = meshView ?: return@LaunchedEffect
        val bitmap = albumBitmap ?: return@LaunchedEffect
        view.setAlbum(bitmap)
    }

    // Publish the cover as the player backdrop's recording stand-in: this GL surface's
    // pixels cannot be captured by a Compose layer recording, so sheets sample this instead.
    val backdropCover = LocalPlayerBackdropCover.current
    LaunchedEffect(backdropCover, albumBitmap) {
        backdropCover?.value = withContext(Dispatchers.Default) {
            // The mesh renders AlbumTextureProcessor's heavily blurred, darkened output;
            // that is the faithful stand-in for the player background, not the sharp cover.
            albumBitmap?.let(AlbumTextureProcessor::process)
        }?.asImageBitmap()
    }

    // 2. 组装当前需要传递给 View 的所有状态
    val shouldAnimate = !meshPlaying || isPlaying

    // 3. 去掉过于严格的版本限制 (只要设备存在就能初始化，低端机 GLES 3.0 兼容性极好)
    // 如果你想绝对保险，可以写 >= Build.VERSION_CODES.LOLLIPOP (21)
    AndroidView(
        factory = { ctx ->
            MeshBackgroundView(ctx).apply {
                meshView = this
                this.alpha = alpha.coerceIn(0f, 1f)
                // 初始化时的默认值
                setFlowSpeed(flowSpeed)
                setRenderScale(renderScale)
                setSubdivision(subdivision)
                setStaticMode(staticMode)
                setPlaying(shouldAnimate)
                setPreserveEGLContextOnPause(true)
            }
        },
        update = { view ->
            // GLSurfaceView owns a native Surface; driving the View alpha avoids a bright
            // first frame escaping a Compose graphics layer during sheet expansion.
            view.alpha = alpha.coerceIn(0f, 1f)

            view.updateVolume(bass * volumeScale)
            view.setFlowSpeed(flowSpeed)
            view.setRenderScale(renderScale)
            view.setSubdivision(subdivision)
            view.setStaticMode(staticMode)
            view.setPlaying(shouldAnimate)
        },
        modifier = modifier.fillMaxSize()
    )
}
