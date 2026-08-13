package com.ljyh.mei.recognition

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.floor

enum class RecognitionDuration(val seconds: Int?) {
    Quick(3), Balanced(6), Extended(9), Continuous(null)
}

class SongRecognitionRecorder(private val context: Context) {
    suspend fun record(seconds: Int): FloatArray = withContext(Dispatchers.IO) {
        check(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            "Microphone permission is required"
        }
        require(seconds in 1..15)
        val sourceRate = preferredSampleRate()
        val minBuffer = AudioRecord.getMinBufferSize(
            sourceRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        check(minBuffer > 0) { "No microphone input is available" }
        val recorder = createRecorder(sourceRate, minBuffer)
        val targetFrames = sourceRate * seconds
        val samples = ShortArray(targetFrames)
        var offset = 0
        try {
            recorder.startRecording()
            while (offset < targetFrames) {
                ensureActive()
                val read = recorder.read(samples, offset, minOf(minBuffer / 2, targetFrames - offset))
                check(read >= 0) { "Microphone read failed ($read)" }
                offset += read
            }
        } finally {
            runCatching { recorder.stop() }
            recorder.release()
        }
        resample(samples, offset, sourceRate, 8_000)
    }

    private fun preferredSampleRate(): Int = listOf(48_000, 44_100, 16_000, 8_000).firstOrNull { rate ->
        AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) > 0
    } ?: 44_100

    private fun createRecorder(sampleRate: Int, minBuffer: Int): AudioRecord {
        val bufferSize = maxOf(minBuffer * 2, sampleRate)
        for (source in listOf(MediaRecorder.AudioSource.UNPROCESSED, MediaRecorder.AudioSource.MIC)) {
            val recorder = runCatching {
                AudioRecord(
                    source,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                )
            }.getOrNull() ?: continue
            if (recorder.state == AudioRecord.STATE_INITIALIZED) return recorder
            recorder.release()
        }
        error("Could not initialize microphone input")
    }

    private fun resample(input: ShortArray, count: Int, sourceRate: Int, targetRate: Int): FloatArray {
        val targetCount = floor(count.toDouble() * targetRate / sourceRate).toInt()
        return FloatArray(targetCount) { index ->
            val sourcePosition = index.toDouble() * sourceRate / targetRate
            val left = floor(sourcePosition).toInt().coerceIn(0, count - 1)
            val right = (left + 1).coerceAtMost(count - 1)
            val fraction = (sourcePosition - left).toFloat()
            ((input[left] * (1f - fraction) + input[right] * fraction) / Short.MAX_VALUE)
        }
    }
}

class NeteaseFingerprintGenerator(context: Context) {
    private val applicationContext = context.applicationContext
    private var webView: WebView? = null
    private var prepared: CompletableDeferred<Unit>? = null

    suspend fun generate(samples: FloatArray): String = withContext(Dispatchers.Main) {
        require(samples.isNotEmpty())
        prepare().await()
        val bytes = ByteBuffer.allocate(samples.size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .apply { samples.forEach(::putFloat) }
            .array()
        val pcm = Base64.encodeToString(bytes, Base64.NO_WRAP)
        suspendCancellableCoroutine { continuation ->
            webView?.evaluateJavascript("generateFingerprint(${quote(pcm)})") { value ->
                runCatching {
                    val result = JSONArray("[$value]").getString(0)
                    check(result.isNotBlank()) { "The fingerprint runtime returned no data" }
                    result
                }.onSuccess { if (continuation.isActive) continuation.resume(it) }
                    .onFailure { if (continuation.isActive) continuation.resumeWithException(it) }
            } ?: continuation.resumeWithException(IllegalStateException("Fingerprint runtime is unavailable"))
        }
    }

    fun release() {
        val destroy = Runnable {
            webView?.apply {
                stopLoading()
                loadUrl("about:blank")
                removeAllViews()
                destroy()
            }
            webView = null
            prepared?.cancel()
            prepared = null
        }
        if (Looper.myLooper() == Looper.getMainLooper()) destroy.run()
        else Handler(Looper.getMainLooper()).post(destroy)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun prepare(): CompletableDeferred<Unit> {
        prepared?.let { return it }
        val deferred = CompletableDeferred<Unit>()
        prepared = deferred
        webView = WebView(applicationContext).apply {
            settings.javaScriptEnabled = true
            settings.allowFileAccess = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (!deferred.isCompleted) deferred.complete(Unit)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: android.webkit.WebResourceRequest?,
                    error: android.webkit.WebResourceError?,
                ) {
                    if (request?.isForMainFrame == true && !deferred.isCompleted) {
                        deferred.completeExceptionally(IllegalStateException(error?.description?.toString()))
                    }
                }
            }
            loadUrl("file:///android_asset/audio_fingerprint/index.html")
        }
        return deferred
    }

    private fun quote(value: String): String = JSONObject.quote(value)
}
