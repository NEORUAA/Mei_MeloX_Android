package com.ljyh.mei.ui.screen.account

import android.annotation.SuppressLint
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.ViewModel
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.core.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ljyh.mei.BuildConfig
import com.ljyh.mei.R
import com.ljyh.mei.constants.CookieKey
import com.ljyh.mei.constants.UserAvatarUrlKey
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.constants.UserNicknameKey
import com.ljyh.mei.data.repository.MeloXRepository
import com.ljyh.mei.ui.glass.GlassButton
import com.ljyh.mei.ui.glass.GlassEmphasis
import com.ljyh.mei.ui.glass.GlassSurfaceStyle
import com.ljyh.mei.ui.glass.IosPinnedListPage
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NeteaseLoginScreen(viewModel: NeteaseLoginViewModel = hiltViewModel()) {
    val navController = LocalNavController.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var detected by remember { mutableStateOf(false) }
    val cookieManager = remember { CookieManager.getInstance() }

    LaunchedEffect(webView) {
        while (webView != null && !detected) {
            val cookieHeader = cookieManager.getCookie("https://music.163.com").orEmpty()
            val musicU = cookieHeader.split(';')
                .map(String::trim)
                .firstOrNull { it.startsWith("MUSIC_U=") }
                ?.substringAfter('=')
                ?.takeIf(String::isNotBlank)
            if (musicU != null) {
                viewModel.completeLogin(musicU)
                detected = true
            }
            delay(500)
        }
    }

    IosPinnedListPage(
        title = stringResource(R.string.netease_login),
        subtitle = if (detected) stringResource(R.string.netease_login_detected)
        else stringResource(R.string.netease_login_waiting),
        showsLargeTitle = false,
        horizontalContentPadding = 0.dp,
        bottomPadding = LocalPlayerAwareWindowInsets.current
            .asPaddingValues()
            .calculateBottomPadding(),
        onNavigateBack = navController::navigateUp,
        actions = {
            if (detected) {
                GlassButton(
                    onClick = navController::navigateUp,
                    style = GlassSurfaceStyle.Standard,
                    emphasis = GlassEmphasis.Prominent,
                ) {
                    Text(stringResource(R.string.done))
                }
            }
        },
    ) {
        item(key = "netease-login-webview") {
            AndroidView(
                factory = { currentContext ->
                    WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
                    WebView(currentContext).apply {
                        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.userAgentString = settings.userAgentString + " Mei/1.0"
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)
                        webViewClient = WebViewClient()
                        loadUrl("https://music.163.com/#/login")
                        webView = this
                    }
                },
                modifier = Modifier.fillParentMaxSize(),
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.apply {
                stopLoading()
                webViewClient = WebViewClient()
                destroy()
            }
            webView = null
        }
    }
}

@HiltViewModel
class NeteaseLoginViewModel @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val repository: MeloXRepository,
) : ViewModel() {
    suspend fun completeLogin(musicU: String) {
        context.dataStore.edit { it[CookieKey] = musicU }
        runCatching { repository.accountProfile() }.getOrNull()?.let { profile ->
            context.dataStore.edit { preferences ->
                preferences[UserIdKey] = profile.id.toString()
                preferences[UserNicknameKey] = profile.nickname
                profile.avatarUrl?.let { preferences[UserAvatarUrlKey] = it }
            }
        }
    }
}

fun logoutNetease(context: android.content.Context) {
    CookieManager.getInstance().removeAllCookies {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            context.dataStore.edit { preferences ->
                preferences.remove(CookieKey)
                preferences.remove(UserIdKey)
                preferences.remove(UserNicknameKey)
                preferences.remove(UserAvatarUrlKey)
            }
        }
    }
}
