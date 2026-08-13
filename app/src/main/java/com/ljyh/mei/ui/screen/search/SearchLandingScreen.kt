package com.ljyh.mei.ui.screen.search

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ljyh.mei.R
import com.ljyh.mei.data.model.melox.SearchDiscovery
import com.ljyh.mei.data.repository.MeloXRepository
import com.ljyh.mei.ui.glass.GlassCard
import com.ljyh.mei.ui.glass.SfIcon
import com.ljyh.mei.ui.glass.SfSymbol
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchDiscoveryState(
    val loading: Boolean = true,
    val discovery: SearchDiscovery? = null,
    val error: String? = null,
)

@HiltViewModel
class SearchDiscoveryViewModel @Inject constructor(
    private val repository: MeloXRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchDiscoveryState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { repository.searchDiscovery() }
                .onSuccess { _state.value = SearchDiscoveryState(loading = false, discovery = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message) }
        }
    }
}

@Composable
fun SearchLandingScreen(viewModel: SearchDiscoveryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val navController = LocalNavController.current
    val insets = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    fun search(keyword: String) {
        Screen.SearchResult.navigate(navController) {
            addPath(Uri.encode(keyword))
            addPath(SearchType.Song.type.toString())
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = insets.calculateTopPadding() + 12.dp,
            bottom = insets.calculateBottomPadding() + 20.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SfIcon(SfSymbol.Search, contentDescription = null, size = 30.dp)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(stringResource(R.string.search_discovery), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    state.discovery?.defaultKeyword?.let {
                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
        if (state.loading && state.discovery == null) {
            item { Row(Modifier.fillMaxWidth().padding(40.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } }
        }
        state.error?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.error) }
        }
        state.discovery?.hotKeywords?.forEachIndexed { index, item ->
            item(key = item.keyword) {
                GlassCard(modifier = Modifier.fillMaxWidth(), onClick = { search(item.keyword) }) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            (index + 1).toString().padStart(2, '0'),
                            color = if (index < 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(end = 13.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(item.keyword, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            item.description?.takeIf(String::isNotBlank)?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        SfIcon("chevron.forward", contentDescription = null, size = 15.dp)
                    }
                }
            }
        }
    }
}
