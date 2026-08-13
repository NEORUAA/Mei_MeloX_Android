package com.ljyh.mei.ui.screen.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.ljyh.mei.R
import com.ljyh.mei.constants.UserIdKey
import com.ljyh.mei.data.model.melox.MessageContact
import com.ljyh.mei.data.model.melox.PrivateConversation
import com.ljyh.mei.data.model.melox.ShareResource
import com.ljyh.mei.data.model.melox.ShareResourceKind
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.toMediaItem
import com.ljyh.mei.playback.queue.ListQueue
import com.ljyh.mei.ui.glass.GlassButton
import com.ljyh.mei.ui.glass.GlassCard
import com.ljyh.mei.ui.glass.GlassEmphasis
import com.ljyh.mei.ui.glass.GlassIconButton
import com.ljyh.mei.ui.glass.GlassSurface
import com.ljyh.mei.ui.glass.IosPinnedListPage
import com.ljyh.mei.ui.glass.SfIcon
import com.ljyh.mei.ui.glass.SfSymbol
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.utils.rememberPreference

@Composable
fun ConversationsScreen(viewModel: ConversationsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val navController = LocalNavController.current
    val currentUserId by rememberPreference(UserIdKey, "")
    val currentUser = currentUserId.toLongOrNull() ?: 0L
    val insets = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    IosPinnedListPage(
        title = stringResource(R.string.private_messages),
        onNavigateBack = navController::navigateUp,
        bottomPadding = insets.calculateBottomPadding(),
        actions = {
            GlassIconButton(onClick = { Screen.MessageContacts.navigate(navController) }) {
                SfIcon("person.crop.circle.badge.plus", stringResource(R.string.message_start))
            }
            GlassIconButton(viewModel::refresh) {
                SfIcon(SfSymbol.ArrowClockwise, stringResource(R.string.refresh))
            }
        },
    ) {
        if (state.isLoading && state.conversations.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        items(state.conversations, key = { it.id }) { conversation ->
            val participant = conversation.participant(currentUser)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    Screen.PrivateConversation.navigate(navController) { addPath(participant.id.toString()) }
                },
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = participant.avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(50)),
                    )
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(participant.displayName, fontWeight = FontWeight.SemiBold)
                        Text(
                            conversation.summary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (conversation.unreadCount > 0) {
                        GlassSurface(emphasis = GlassEmphasis.Prominent, shape = RoundedCornerShape(50)) {
                            Text(conversation.unreadCount.toString(), modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageContactsScreen(viewModel: MessageContactsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val navController = LocalNavController.current
    val insets = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, state.contacts) {
        val value = query.trim()
        if (value.isEmpty()) state.contacts else state.contacts.filter {
            it.displayName.contains(value, ignoreCase = true) || it.nickname.contains(value, ignoreCase = true)
        }
    }

    IosPinnedListPage(
        title = stringResource(R.string.message_start),
        onNavigateBack = navController::navigateUp,
        bottomPadding = insets.calculateBottomPadding(),
        actions = {
            GlassIconButton(viewModel::refresh) {
                SfIcon(SfSymbol.ArrowClockwise, stringResource(R.string.refresh))
            }
        },
    ) {
        item {
            GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(50)) {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    decorationBox = { inner ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SfIcon(SfSymbol.Search, null, size = 18.dp)
                            Box(Modifier.weight(1f).padding(start = 9.dp)) {
                                if (query.isEmpty()) {
                                    Text(
                                        stringResource(R.string.message_search_contacts),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                inner()
                            }
                        }
                    },
                )
            }
        }
        if (state.isLoading && state.contacts.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        if (!state.isLoading && state.error == null && filtered.isEmpty()) {
            item { Text(stringResource(R.string.share_no_contacts), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(filtered, key = MessageContact::id) { contact ->
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    Screen.PrivateConversation.navigate(navController) { addPath(contact.id.toString()) }
                },
            ) {
                Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = contact.avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(50)),
                    )
                    Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
                        Text(contact.displayName, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        contact.signature?.takeIf(String::isNotBlank)?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    SfIcon("chevron.right", null, size = 16.dp)
                }
            }
        }
    }
}

@Composable
fun ConversationScreen(userId: Long, viewModel: ConversationViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val navController = LocalNavController.current
    val playerConnection = LocalPlayerConnection.current
    val currentUserId by rememberPreference(UserIdKey, "")
    val currentUser = currentUserId.toLongOrNull() ?: 0L
    val insets = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    var draft by remember { mutableStateOf("") }
    LaunchedEffect(userId) { viewModel.load(userId) }
    val send = { viewModel.send(draft) { draft = "" } }

    Column(
        modifier = Modifier.fillMaxSize().padding(
            start = 12.dp,
            end = 12.dp,
            top = insets.calculateTopPadding() + 8.dp,
            bottom = insets.calculateBottomPadding() + 8.dp,
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(navController::navigateUp) {
                SfIcon(SfSymbol.ChevronBack, stringResource(R.string.navigation_back), mirrored = true)
            }
            Text(
                stringResource(R.string.private_conversation),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.messages, key = { it.id }) { message ->
                val outgoing = message.fromUser?.id == currentUser
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (outgoing) Arrangement.End else Arrangement.Start,
                ) {
                    GlassSurface(
                        emphasis = if (outgoing) GlassEmphasis.Prominent else GlassEmphasis.Regular,
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.fillMaxWidth(0.78f),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                            message.payload.resource?.let { resource ->
                                MessageResourceCard(resource, outgoing) {
                                    when (resource.kind) {
                                        ShareResourceKind.Song -> {
                                            val item = resource.toMediaMetadata().toMediaItem()
                                            playerConnection?.playQueue(
                                                ListQueue("private-message", resource.title, listOf(item.mediaId to item)),
                                            )
                                        }
                                        ShareResourceKind.Playlist -> Screen.PlayList.navigate(navController) {
                                            addPath(resource.id.toString())
                                        }
                                        ShareResourceKind.Album -> Screen.Album.navigate(navController) {
                                            addPath(resource.id.toString())
                                        }
                                    }
                                }
                            }
                            if (message.payload.text.isNotBlank()) {
                                Text(
                                    message.payload.text,
                                    modifier = Modifier.padding(top = if (message.payload.resource == null) 0.dp else 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassSurface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(50),
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                    decorationBox = { inner ->
                        if (draft.isEmpty()) Text(stringResource(R.string.message_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        inner()
                    },
                )
            }
            GlassButton(onClick = send, enabled = draft.isNotBlank() && !state.isSending, emphasis = GlassEmphasis.Prominent) {
                Text(stringResource(R.string.send))
            }
        }
    }
}

@Composable
private fun MessageResourceCard(resource: ShareResource, outgoing: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = resource.artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)),
        )
        Column(Modifier.weight(1f).padding(horizontal = 9.dp)) {
            Text(resource.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                resource.subtitle.orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = if (outgoing) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .76f)
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        SfIcon("chevron.right", null, size = 15.dp)
    }
}

private fun ShareResource.toMediaMetadata() = MediaMetadata(
    id = id,
    title = title,
    coverUrl = artworkUrl.orEmpty(),
    artists = subtitle.orEmpty().split(" / ").filter(String::isNotBlank).map {
        MediaMetadata.Artist(it.hashCode().toLong(), it)
    },
    duration = 0,
    album = MediaMetadata.Album(0, ""),
)

private fun PrivateConversation.participant(currentUserId: Long): MessageContact =
    fromUser?.takeIf { it.id != currentUserId }
        ?: toUser?.takeIf { it.id != currentUserId }
        ?: fromUser
        ?: toUser
        ?: MessageContact(0, "NetEase user", null, null, null)
