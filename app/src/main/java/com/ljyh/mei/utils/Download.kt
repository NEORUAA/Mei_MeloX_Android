package com.ljyh.mei.utils

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.ljyh.mei.AppContext
import com.ljyh.mei.data.model.room.DownloadStatus
import com.ljyh.mei.data.model.room.DownloadTask
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.playback.DownloadWorker
import com.ljyh.mei.playback.SongDownloadInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

object DownloadManager {
    private const val SONG_WORK_NAME_PREFIX = "download_song_"
    private val managementScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun getDefaultDownloadPath(): String {
        return "Music/Mei"
    }

    suspend fun enqueue(
        context: Context,
        songs: List<SongDownloadInfo>,
        playlistName: String,
        playlistId: String = "",
        downloadPath: String = getDefaultDownloadPath()
    ) {
        DownloadWorker.createNotificationChannel(context)

        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)

            val tasks = songs.map { info ->
                DownloadTask(
                    songId = info.songId,
                    url = info.url ?: "",
                    fileName = "",
                    fileType = info.fileType.ifBlank {
                        val pathWithoutQuery = (info.url ?: "").substringBefore("?")
                        val lastSegment = pathWithoutQuery.substringAfterLast("/")
                        lastSegment.substringAfterLast(".", "")
                    },
                    status = DownloadStatus.PENDING,
                    progress = 0,
                    songTitle = info.songTitle,
                    songArtist = info.songArtist.joinToString("/"),
                    songAlbum = info.songAlbum,
                    songCover = info.songCover,
                    quality = info.quality,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            }

            db.downloadDao().insertAll(tasks)

            val wm = WorkManager.getInstance(context)
            songs.forEach { song ->
                val uniqueWorkName = SONG_WORK_NAME_PREFIX + song.songId
                val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                    .addTag("download")
                    .addTag(uniqueWorkName)
                    .setInputData(
                        androidx.work.Data.Builder()
                            .putString(DownloadWorker.KEY_SONG_IDS, Gson().toJson(listOf(song.songId)))
                            .putString(DownloadWorker.KEY_PLAYLIST_NAME, playlistName)
                            .putString(DownloadWorker.KEY_DOWNLOAD_PATH, downloadPath)
                            .build()
                    )
                    .build()
                wm.enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, workRequest)
            }
            Timber.tag("DownloadManager").d("Song work enqueued: playlist=$playlistId, songs=${songs.size}")
        }
    }

    fun pauseSong(context: Context, songId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(SONG_WORK_NAME_PREFIX + songId)
        managementScope.launch {
            AppDatabase.getDatabase(context).downloadDao().updateStatus(songId, DownloadStatus.PAUSED)
        }
    }

    suspend fun resumeSong(
        context: Context,
        songId: String,
        playlistName: String,
        downloadPath: String = getDefaultDownloadPath()
    ) {
        val db = AppDatabase.getDatabase(context)
        val task = db.downloadDao().getBySongId(songId) ?: return
        db.downloadDao().updateStatus(songId, DownloadStatus.PENDING)
        enqueue(
            context = context,
            songs = listOf(
                SongDownloadInfo(
                    songId = task.songId,
                    url = task.url,
                    songTitle = task.songTitle,
                    songArtist = task.songArtist.split("/").map { it.trim() }.filter { it.isNotBlank() },
                    songAlbum = task.songAlbum,
                    songCover = task.songCover,
                    duration = 0,
                    quality = task.quality,
                )
            ),
            playlistName = playlistName,
            playlistId = "resume_${System.currentTimeMillis()}",
            downloadPath = downloadPath
        )
    }

    fun deleteTask(context: Context, songId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(SONG_WORK_NAME_PREFIX + songId)
        managementScope.launch {
            val db = AppDatabase.getDatabase(context)
            val song = db.songDao().getSong(songId).first()
            song?.path?.let { path ->
                runCatching {
                    if (path.startsWith("content://")) {
                        context.contentResolver.delete(android.net.Uri.parse(path), null, null)
                    } else {
                        File(path).takeIf(File::exists)?.delete()
                    }
                }.onFailure { Timber.w(it, "Unable to remove downloaded file for %s", songId) }
            }
            db.songDao().updatePath(songId, null)
            db.downloadDao().delete(songId)
        }
    }

    fun deleteAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag("download")
        managementScope.launch {
            val db = AppDatabase.getDatabase(context)
            db.downloadDao().getAll().first().forEach { task ->
                val song = db.songDao().getSong(task.songId).first()
                song?.path?.let { path ->
                    runCatching {
                        if (path.startsWith("content://")) {
                            context.contentResolver.delete(android.net.Uri.parse(path), null, null)
                        } else {
                            File(path).takeIf(File::exists)?.delete()
                        }
                    }.onFailure { Timber.w(it, "Unable to remove downloaded file for %s", task.songId) }
                }
                db.songDao().updatePath(task.songId, null)
            }
            db.downloadDao().deleteAll()
        }
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag("download")
    }

    fun isSongDownloaded(songId: String): Boolean {
        val db = AppDatabase.getDatabase(AppContext.instance)
        val song = kotlinx.coroutines.runBlocking { db.songDao().getSong(songId).first() }
        val path = song?.path ?: return false
        if (path.startsWith("content://")) return true
        return File(path).exists()
    }

    suspend fun isSongDownloading(songId: String): Boolean {
        val db = AppDatabase.getDatabase(AppContext.instance)
        val task = db.downloadDao().getBySongId(songId)
        return task?.status == DownloadStatus.DOWNLOADING || task?.status == DownloadStatus.PENDING
    }
}
