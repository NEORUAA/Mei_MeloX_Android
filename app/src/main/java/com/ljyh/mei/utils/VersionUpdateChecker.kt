package com.ljyh.mei.utils

import android.net.Uri
import com.ljyh.mei.constants.UserAgent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

sealed interface VersionUpdateResult {
    data class UpdateAvailable(
        val latestTag: String,
        val releaseUrl: String,
    ) : VersionUpdateResult

    data object UpToDate : VersionUpdateResult

    data object Failed : VersionUpdateResult
}

/** Checks the canonical GitHub tag list without blocking the Compose thread. */
object VersionUpdateChecker {
    private const val TagsEndpoint =
        "https://api.github.com/repos/NEORUAA/MeiloX/tags?per_page=100"
    private const val RepositoryUrl = "https://github.com/NEORUAA/MeiloX"
    private const val ConnectTimeoutMillis = 8_000
    private const val ReadTimeoutMillis = 8_000

    suspend fun check(currentVersion: String): VersionUpdateResult = withContext(Dispatchers.IO) {
        val current = parseVersion(currentVersion) ?: return@withContext VersionUpdateResult.Failed
        val connection = (URL(TagsEndpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = ConnectTimeoutMillis
            readTimeout = ReadTimeoutMillis
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", UserAgent)
        }

        try {
            if (connection.responseCode !in 200..299) {
                return@withContext VersionUpdateResult.Failed
            }
            val tags = connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                JSONArray(reader.readText()).toVersionTags()
            }
            val latest = tags.maxWithOrNull { left, right ->
                compareVersions(left.version, right.version)
            } ?: return@withContext VersionUpdateResult.Failed

            if (compareVersions(latest.version, current) > 0) {
                VersionUpdateResult.UpdateAvailable(
                    latestTag = latest.name,
                    releaseUrl = "$RepositoryUrl/releases/tag/${Uri.encode(latest.name)}",
                )
            } else {
                VersionUpdateResult.UpToDate
            }
        } catch (_: Exception) {
            VersionUpdateResult.Failed
        } finally {
            connection.disconnect()
        }
    }

    private data class VersionTag(
        val name: String,
        val version: ComparableVersion,
    )

    private data class ComparableVersion(
        val parts: List<Int>,
        val preRelease: Boolean,
    )

    private fun JSONArray.toVersionTags(): List<VersionTag> = buildList {
        for (index in 0 until length()) {
            val name = optJSONObject(index)?.optString("name")?.trim().orEmpty()
            parseVersion(name)?.let { version -> add(VersionTag(name, version)) }
        }
    }

    private fun parseVersion(raw: String): ComparableVersion? {
        val normalized = raw.trim().removePrefix("refs/tags/")
        val match = VERSION_PATTERN.matchEntire(normalized) ?: return null
        val parts = match.groupValues[1].split('.').map { it.toIntOrNull() ?: return null }
        return ComparableVersion(
            parts = parts,
            preRelease = match.groupValues[2].isNotEmpty(),
        )
    }

    private fun compareVersions(left: ComparableVersion, right: ComparableVersion): Int {
        val size = maxOf(left.parts.size, right.parts.size)
        for (index in 0 until size) {
            val difference = (left.parts.getOrElse(index) { 0 }) -
                (right.parts.getOrElse(index) { 0 })
            if (difference != 0) return difference
        }
        return when {
            left.preRelease == right.preRelease -> 0
            left.preRelease -> -1
            else -> 1
        }
    }

    private val VERSION_PATTERN = Regex(
        "^[vV]?(\\d+(?:\\.\\d+)*)(-[0-9A-Za-z.-]+)?(?:\\+[0-9A-Za-z.-]+)?$",
    )
}
