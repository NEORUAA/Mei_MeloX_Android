package com.ljyh.mei.data.model.melox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NeteaseMusicLinkParserTest {
    @Test
    fun parsesSongAndHashSongLinks() {
        assertEquals(NeteaseMusicLink.Song(347230), NeteaseMusicLinkParser.parse("https://music.163.com/song?id=347230"))
        assertEquals(NeteaseMusicLink.Song(42), NeteaseMusicLinkParser.parse("分享 https://music.163.com/#/song?id=42 给你"))
    }

    @Test
    fun parsesListenTogetherInvitationFromShareText() {
        assertEquals(
            NeteaseMusicLink.ListenTogether(
                "和我听 https://st.music.163.com/listen-together/share/?songId=1&roomId=room-a&inviterUid=9",
                ListenTogetherInvitation("room-a", "9", 1),
            ),
            NeteaseMusicLinkParser.parse("和我听 https://st.music.163.com/listen-together/share/?songId=1&amp;roomId=room-a&amp;inviterUid=9"),
        )
    }

    @Test
    fun rejectsUntrustedHostsAndIncompleteInvitations() {
        assertNull(NeteaseMusicLinkParser.parse("https://example.com/song?id=42"))
        assertNull(NeteaseMusicLinkParser.parse("https://st.music.163.com/listen-together/share/?roomId=room-a"))
    }
}
