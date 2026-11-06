package com.example

import com.example.data.parser.M3uParser
import com.example.data.security.CryptoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun m3uParser_parsesLiveAndVodCorrectly() {
        val sampleM3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="beIN1" tvg-name="beIN Sports 1" tvg-logo="http://logo.png" group-title="Sports",beIN Sports 1 HD
            http://server.com:8080/live/user/pass/123.m3u8
            #EXTINF:-1 tvg-id="" tvg-name="Inception" tvg-logo="http://inception.png" group-title="أفلام أكشن",Inception (2010)
            http://server.com:8080/movie/user/pass/456.mp4
        """.trimIndent()

        val result = M3uParser.parse(sampleM3u.byteInputStream(), subscriptionId = 1L)

        assertEquals(1, result.channels.size)
        assertEquals("beIN Sports 1 HD", result.channels[0].name)
        assertEquals("Sports", result.channels[0].categoryName)
        assertEquals("http://server.com:8080/live/user/pass/123.m3u8", result.channels[0].streamUrl)

        assertEquals(1, result.vodItems.size)
        assertEquals("Inception (2010)", result.vodItems[0].originalTitle)
        assertEquals("أفلام أكشن", result.vodItems[0].categoryName)
        assertTrue(result.vodItems[0].streamUrl.endsWith(".mp4"))
    }

    @Test
    fun cryptoManager_encryptsAndDecryptsCorrectly() {
        val secretPassword = "SuperSecretPassword123!@#"
        val encrypted = CryptoManager.encrypt(secretPassword)

        // Ensure ciphertext is scrambled and not plaintext
        assertNotEquals(secretPassword, encrypted)
        assertTrue(encrypted.isNotEmpty())

        // Decrypt and ensure original content matches
        val decrypted = CryptoManager.decrypt(encrypted)
        assertEquals(secretPassword, decrypted)
    }
}
