package com.ljyh.mei.ui.screen

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.ui.platform.LocalContext
import com.ljyh.mei.di.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.net.URLDecoder
import com.ljyh.mei.ui.screen.about.AboutScreen
import com.ljyh.mei.ui.screen.album.AlbumDetailScreen
import com.ljyh.mei.ui.screen.history.HistoryScreen
import com.ljyh.mei.ui.screen.local.LocalMusicScreen
import com.ljyh.mei.ui.screen.local.LocalSongListScreen
import com.ljyh.mei.ui.screen.main.home.HomeHubScreen
import com.ljyh.mei.ui.screen.main.library.LibraryScreen
import com.ljyh.mei.ui.screen.playlist.EveryDay
import com.ljyh.mei.ui.screen.playlist.PlaylistScreen
import com.ljyh.mei.ui.screen.search.SearchResultScreen
import com.ljyh.mei.ui.screen.setting.AppearanceSettings
import com.ljyh.mei.ui.screen.artist.ArtistScreen
import com.ljyh.mei.ui.screen.main.findmusic.FindMusicScreen
import com.ljyh.mei.ui.screen.setting.ContentsSetting
import com.ljyh.mei.ui.screen.setting.DownloadManageScreen
import com.ljyh.mei.ui.screen.setting.DownloadSetting
import com.ljyh.mei.ui.screen.setting.StorageManagementScreen
import com.ljyh.mei.ui.screen.setting.GeneralSettings
import com.ljyh.mei.ui.screen.setting.PlaySetting
import com.ljyh.mei.ui.screen.setting.EqualizerSettings
import com.ljyh.mei.ui.screen.setting.LyricsSettings
import com.ljyh.mei.ui.screen.setting.SettingScreen
import com.ljyh.mei.ui.screen.log.LogScreen
import com.ljyh.mei.ui.screen.comment.CommentScreen
import com.ljyh.mei.ui.screen.cloud.CloudMusicScreen
import com.ljyh.mei.ui.screen.podcast.PodcastDetailScreen
import com.ljyh.mei.ui.screen.podcast.PodcastScreen
import com.ljyh.mei.ui.screen.search.SearchLandingScreen
import com.ljyh.mei.ui.screen.social.ConversationScreen
import com.ljyh.mei.ui.screen.social.ConversationsScreen
import com.ljyh.mei.ui.screen.social.MessageContactsScreen
import com.ljyh.mei.ui.screen.listentogether.ListenTogetherScreen
import com.ljyh.mei.ui.screen.recognition.SongRecognitionScreen
import com.ljyh.mei.ui.screen.account.NeteaseLoginScreen
import com.ljyh.mei.ui.screen.account.AccountHomeScreen
import com.ljyh.mei.ui.screen.account.ListeningRankScreen
import com.ljyh.mei.ui.screen.song.SongWikiScreen


@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    composable(Screen.Home.route) {
        HomeHubScreen()
    }

    composable(Screen.Library.route) {
        LibraryScreen()
    }

    composable(Screen.FindMusic.route) {
        FindMusicScreen()
    }

    composable(Screen.Podcasts.route) {
        PodcastScreen()
    }

    composable(Screen.CloudMusic.route) {
        CloudMusicScreen()
    }

    composable(Screen.Search.route) {
        SearchLandingScreen()
    }

    composable(Screen.PrivateMessages.route) {
        ConversationsScreen()
    }

    composable(Screen.MessageContacts.route) {
        MessageContactsScreen()
    }

    composable(
        route = "${Screen.PrivateConversation.route}/{userId}",
        arguments = listOf(navArgument("userId") { type = NavType.LongType }),
    ) {
        ConversationScreen(it.arguments!!.getLong("userId"))
    }

    composable(Screen.ListenTogether.route) {
        ListenTogetherScreen()
    }

    composable(Screen.SongRecognition.route) {
        SongRecognitionScreen()
    }

    composable(Screen.NeteaseLogin.route) {
        NeteaseLoginScreen()
    }

    composable(Screen.AccountHome.route) {
        AccountHomeScreen()
    }

    composable(
        route = "${Screen.AccountListeningRank.route}/{userId}",
        arguments = listOf(navArgument("userId") { type = NavType.LongType }),
    ) {
        ListeningRankScreen(it.arguments!!.getLong("userId"))
    }

    composable(
        route = "${Screen.PodcastDetail.route}/{id}",
        arguments = listOf(navArgument("id") { type = NavType.LongType }),
    ) {
        PodcastDetailScreen(it.arguments!!.getLong("id"))
    }

    composable(Screen.Test.route) {
        Test()
    }

    composable(Screen.Setting.route) {
        SettingScreen(scrollBehavior)
    }

    composable(Screen.AppearanceSettings.route) {
        AppearanceSettings(scrollBehavior)
    }

    composable(Screen.GeneralSettings.route) {
        GeneralSettings()
    }

    composable(Screen.LyricsSettings.route) {
        LyricsSettings()
    }

    composable(Screen.ContentSettings.route) {
        ContentsSetting(scrollBehavior)
    }
    composable(Screen.PlaySettings.route){
        PlaySetting(scrollBehavior)
    }
    composable(Screen.EqualizerSettings.route) {
        EqualizerSettings()
    }

    composable(Screen.DownloadSettings.route) {
        DownloadSetting(scrollBehavior)
    }

    composable(Screen.StorageManagement.route) {
        StorageManagementScreen()
    }

    composable(Screen.DownloadManage.route) {
        DownloadManageScreen(scrollBehavior)
    }

    composable(Screen.LocalMusic.route) {
        LocalMusicScreen(scrollBehavior)
    }

    composable(
        route = "${Screen.LocalSongList.route}/{type}/{name}",
        arguments = listOf(
            navArgument("type") { type = NavType.StringType },
            navArgument("name") { type = NavType.StringType }
        )
    ) {
        val type = it.arguments?.getString("type") ?: "all"
        val name = it.arguments?.getString("name") ?: ""
        val context = LocalContext.current

        val filterValue: String
        val title: String

        when (type) {
            "folder" -> {
                filterValue = URLDecoder.decode(name, "UTF-8")
                title = filterValue.substringAfterLast('/').ifEmpty { filterValue.substringAfterLast(":") }
            }
            "artist" -> {
                filterValue = name
                title = name
            }
            "album" -> {
                filterValue = name
                title = name
            }
            else -> {
                filterValue = name
                title = "全部歌曲"
            }
        }

        LocalSongListScreen(
            filterType = when (type) {
                "folder" -> "folder"
                else -> type
            },
            filterValue = filterValue,
            title = title,
            scrollBehavior = scrollBehavior
        )
    }

    composable(Screen.EveryDay.route){
        EveryDay()
    }
    composable(Screen.About.route) {
        AboutScreen()
    }
    composable(Screen.Log.route) {
        LogScreen()
    }
    composable(
        route = "${Screen.SearchResult.route}/{query}/{type}",
        arguments = listOf(
            navArgument("query") {
                type = NavType.StringType
            }
            ,
            navArgument("type") {
                type = NavType.IntType
            }
        ),
    ) {
        SearchResultScreen(
            query = android.net.Uri.decode(it.arguments!!.getString("query")!!),
            type= it.arguments!!.getInt("type"),
        )
    }
    composable(
        route = "${Screen.PlayList.route}/{id}",
        arguments = listOf(
            navArgument("id") {
                type = NavType.LongType
            }
        )
    ) {
        PlaylistScreen(id = it.arguments!!.getLong("id"))
    }


    composable(
        route = "${Screen.Album.route}/{id}",
        arguments = listOf(
            navArgument("id") {
                type = NavType.LongType
            }
        )
    ) {
        AlbumDetailScreen(id = it.arguments!!.getLong("id"))
    }
    
    composable(
        route = "${Screen.Artist.route}/{id}",
        arguments = listOf(
            navArgument("id") {
                type = NavType.StringType
            }
        )
    ) {
        ArtistScreen(id = it.arguments!!.getString("id")!!)
    }

    composable(Screen.History.route) {
        HistoryScreen()
    }

    composable(
        route = "${Screen.Comment.route}/{songId}",
        arguments = listOf(
            navArgument("songId") { type = NavType.StringType }
        )
    ) {
        CommentScreen(
            songId = it.arguments!!.getString("songId")!!
        )
    }

    composable(
        route = "${Screen.SongWiki.route}/{songId}",
        arguments = listOf(navArgument("songId") { type = NavType.LongType }),
    ) {
        SongWikiScreen(songId = it.arguments!!.getLong("songId"))
    }
}
