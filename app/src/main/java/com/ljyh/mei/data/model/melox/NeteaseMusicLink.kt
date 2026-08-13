package com.ljyh.mei.data.model.melox

import java.net.URI
import java.net.URLDecoder

sealed interface NeteaseMusicLink {
    data class Song(val id: Long) : NeteaseMusicLink
    data class ListenTogether(val invitationText: String, val invitation: ListenTogetherInvitation) : NeteaseMusicLink
}

object NeteaseMusicLinkParser {
    private val urlPattern = Regex("https?://[^\\s<>]+", RegexOption.IGNORE_CASE)

    fun parse(text: String): NeteaseMusicLink? {
        val normalized = text.trim().replace("&amp;", "&")
        if (normalized.isEmpty()) return null
        val candidates = urlPattern.findAll(normalized).map { it.value.trimEnd('.', ',', '，', '。') }.toList()
            .ifEmpty { listOf(normalized) }
        candidates.forEach { candidate ->
            val uri = runCatching { URI(candidate) }.getOrNull() ?: return@forEach
            val host = uri.host?.lowercase() ?: return@forEach
            if (host != "music.163.com" && !host.endsWith(".music.163.com")) return@forEach
            if (uri.path.orEmpty().contains("/listen-together/", ignoreCase = true)) {
                ListenTogetherInvitation.parse(candidate)?.let {
                    return NeteaseMusicLink.ListenTogether(normalized, it)
                }
            }
            songId(uri)?.let { return NeteaseMusicLink.Song(it) }
        }
        return null
    }

    private fun songId(uri: URI): Long? {
        if (uri.path.orEmpty().trimEnd('/').endsWith("/song", ignoreCase = true)) {
            queryValue(uri.rawQuery, "id")?.toLongOrNull()?.takeIf { it > 0 }?.let { return it }
        }
        val fragment = uri.rawFragment ?: return null
        val fragmentUri = runCatching { URI("https://music.163.com/${fragment.trimStart('/')}") }.getOrNull() ?: return null
        if (!fragmentUri.path.orEmpty().trimEnd('/').endsWith("/song", ignoreCase = true)) return null
        return queryValue(fragmentUri.rawQuery, "id")?.toLongOrNull()?.takeIf { it > 0 }
    }

    private fun queryValue(query: String?, name: String): String? = query
        ?.split('&')
        ?.firstNotNullOfOrNull { part ->
            val key = part.substringBefore('=', "")
            if (!key.equals(name, ignoreCase = true)) null
            else URLDecoder.decode(part.substringAfter('=', ""), Charsets.UTF_8.name())
        }
}
