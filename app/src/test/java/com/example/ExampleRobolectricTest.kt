package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.audio.TactileAudioEngine
import com.example.data.local.LocalAliasEntity
import com.example.data.local.NeoTactileDatabase
import com.example.ui.auth.VerifiedAccountStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NeoTactile Chat", appName)
    }

    @Test
    fun `room local alias override persists locally`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, NeoTactileDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.localAliasDao()

        dao.upsertAlias(
            LocalAliasEntity(
                targetUserId = "peer_nova_01",
                overriddenDisplayName = "Custom Tactile Alias",
                overriddenAvatarUri = "preset://avatar_cyan"
            )
        )

        val fetched = dao.observeAliasForUser("peer_nova_01").first()
        assertEquals("Custom Tactile Alias", fetched?.overriddenDisplayName)
        db.close()
    }

    @Test
    fun `anti-fake account store blocks fake usernames and verifies real accounts`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertTrue(VerifiedAccountStore.isFakeIdentifier("fake_user"))
        assertTrue(VerifiedAccountStore.isFakeIdentifier("spam_bot"))
        assertFalse(VerifiedAccountStore.isFakeIdentifier("saymon_bd26"))

        val captcha = VerifiedAccountStore.generateCaptchaCode()
        assertEquals(4, captcha.length)

        val session = VerifiedAccountStore.saveVerifiedAccount(
            context = context,
            userIdInput = "saymon_bd26",
            passwordInput = "SecurePass#2026",
            displayName = "Saymon BD",
            phone = "+8801700000000"
        )
        assertTrue(session.isAccountVerified)

        val loginResult = VerifiedAccountStore.verifyAndLoginManualAccount(
            context = context,
            userIdInput = "saymon_bd26",
            passwordInput = "SecurePass#2026"
        )
        assertTrue(loginResult.isSuccess)
        assertEquals("Saymon BD", loginResult.getOrNull()?.displayName)
    }

    @Test
    fun `mobile device storage saves documents and reports storage stats`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = TactileAudioEngine(context)
        val uriStr = engine.saveDocumentToDeviceStorage("test_note.txt", "Saved on mobile storage")
        assertTrue(uriStr.contains("test_note.txt"))

        val stats = engine.getDeviceStorageStats()
        assertNotNull(stats)
        assertTrue(stats.savedFilesCount >= 1)
    }
}
