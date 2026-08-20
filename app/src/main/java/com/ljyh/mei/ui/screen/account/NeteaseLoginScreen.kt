package com.ljyh.mei.ui.screen.account

import android.annotation.SuppressLint
import android.os.Build
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.ViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
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
import com.ljyh.mei.ui.glass.GlassIconButton
import com.ljyh.mei.ui.glass.GlassSurface
import com.ljyh.mei.ui.glass.SfIcon
import com.ljyh.mei.ui.glass.SfSymbol
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NeteaseLoginScreen(viewModel: NeteaseLoginViewModel = hiltViewModel()) {
    val context = LocalContext.current
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

    Box(Modifier.fillMaxSize()) {
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
            modifier = Modifier.fillMaxSize(),
        )
        GlassSurface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
                ),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(navController::navigateUp) {
                    SfIcon(SfSymbol.ChevronBack, stringResource(R.string.navigation_back), mirrored = true)
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(stringResource(R.string.netease_login), fontWeight = FontWeight.SemiBold)
                    Text(
                        if (detected) stringResource(R.string.netease_login_detected)
                        else stringResource(R.string.netease_login_waiting),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (detected) {
                    GlassButton(onClick = navController::navigateUp, emphasis = GlassEmphasis.Prominent) {
                        Text(stringResource(R.string.done))
                    }
                }
            }
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
